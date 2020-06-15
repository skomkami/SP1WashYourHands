package agh.edu.pl.washyourhands;

import agh.edu.pl.washyourhands.state.State;
import agh.edu.pl.washyourhands.views.LocationItem;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;
import com.google.android.gms.location.*;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RemindService extends Service {

    private static final String NOTIFICATION_MESSAGE = "Please wash your hands";
    private static final double DISTANCE_DETLA = 30.0;
    private static final Long HOUR_IN_MILLS = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);

    private ArrayList<LocationItem> homeLocations = new ArrayList<>();
    private Location lastReadLocation;
    private boolean atHome = false;
    private Date lastNotificationTime;
    private Long interval = 2 * HOUR_IN_MILLS;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private NotificationManagerCompat notificationManager;
    private BroadcastReceiver addLocationReceiver;
    private BroadcastReceiver removeLocationReceiver;
    private BroadcastReceiver getLocationsReceiver;
    private BroadcastReceiver setIntervalReceiver;
    private BroadcastReceiver getIntervalReceiver;

    private boolean isServiceStarted = false;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        Calendar calendar = Calendar.getInstance();
        lastNotificationTime = calendar.getTime();
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location currentLocation = locationResult.getLastLocation();

                Log.d("mylog", "Lat is: " + currentLocation.getLatitude() + ", Lng is: " + currentLocation.getLongitude());

                sendLocationBroadcast(currentLocation);
                checkCurrentLocation(currentLocation);

                Calendar calendar = Calendar.getInstance();
                Log.d("time since last", "" + (calendar.getTime().getTime() - lastNotificationTime.getTime()) );
                if( calendar.getTime().getTime() - lastNotificationTime.getTime() >= interval) {
                    sendOnHandWashChannel();
                }
            }
        };

        notificationManager = NotificationManagerCompat.from(this);
        registerAddLocationReceiver();
        registerRemoveLocationReceiver();
        registerGetLocationsReceiver();
        registerSetIntervalReceiver();
        registerGetIntervalReceiver();

        restoreState();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            String action = intent.getAction();
            switch (action) {
                case "START": startService(); break;
                case "STOP": stopService(); break;
                default: Log.d("Remind service", "Remind service started without action"); break;
            }
        } else {
            Log.d("Remind service", "Started with a null intent. It has been probably restarted by the system.");
        }
        return START_NOT_STICKY;
    }

    private void startService() {

        isServiceStarted = true;

        requestLocation();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification appRunningNotification = new NotificationCompat.Builder(this, App.APP_RUNNING_CHANNEL)
                .setContentTitle("Wash Your Hands")
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, appRunningNotification);
        sendInterval();
    }

    private void stopService() {
        removeLocationUpdates();
        stopForeground(true);
    }

    private void registerAddLocationReceiver() {
        IntentFilter addLocationFilter = new IntentFilter();
        addLocationFilter.addAction("agh.edu.pl.washyourhands.add_location");
        addLocationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                LocationItem location = intent.getParcelableExtra("location_item");
                atHome = true;
                homeLocations.add(location);
            }
        };
        registerReceiver(addLocationReceiver, addLocationFilter);
    }

    private void registerRemoveLocationReceiver() {
        IntentFilter removeLocationFilter = new IntentFilter();
        removeLocationFilter.addAction("agh.edu.pl.washyourhands.remove_location");
        removeLocationReceiver = new BroadcastReceiver() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onReceive(Context context, Intent intent) {
                String locationName = intent.getStringExtra("location_name");
                int i = 0;
                boolean found = false;
                for(; i < homeLocations.size(); ++i) {
                    if(homeLocations.get(i).getName().equals(locationName)) {
                        found = true;
                        break;
                    }
                }
                if(found == true) {
                    homeLocations.remove(i);
                }
            }
        };
        registerReceiver(removeLocationReceiver, removeLocationFilter);
    }

    private void registerGetLocationsReceiver() {
        IntentFilter getLocationsFilter = new IntentFilter();
        getLocationsFilter.addAction("agh.edu.pl.washyourhands.get_locations");
        getLocationsReceiver = new BroadcastReceiver() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onReceive(Context context, Intent intent) {
                Intent locationsIntend = new Intent();
                locationsIntend.setAction("agh.edu.pl.washyourhands.locations");
                ArrayList<String> locationsNames = homeLocations.stream().map( l -> l.getName()).collect(Collectors.toCollection(ArrayList::new));
                locationsIntend.putStringArrayListExtra("locations", locationsNames);
                sendBroadcast(locationsIntend);
            }
        };
        registerReceiver(getLocationsReceiver, getLocationsFilter);
    }

    private void registerSetIntervalReceiver() {
        IntentFilter setIntervalFilter = new IntentFilter();
        setIntervalFilter.addAction("agh.edu.pl.washyourhands.set_interval");
        setIntervalReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("new interval", "" + intent.getLongExtra("interval", 2 * HOUR_IN_MILLS));
                interval = intent.getLongExtra("interval", 2 * HOUR_IN_MILLS);
            }
        };
        registerReceiver(setIntervalReceiver, setIntervalFilter);
    }

    private void sendInterval() {
        Intent sendInterval = new Intent();
        sendInterval.setAction("agh.edu.pl.washyourhands.interval");
        sendInterval.putExtra("interval", interval);
        sendBroadcast(sendInterval);
    }

    private void registerGetIntervalReceiver() {
        IntentFilter getIntervalFilter = new IntentFilter();
        getIntervalFilter.addAction("agh.edu.pl.washyourhands.get_interval");
        getIntervalReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                sendInterval();
            }
        };
        registerReceiver(getIntervalReceiver, getIntervalFilter);
    }

    private void requestLocation() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    private void removeLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void sendOnHandWashChannel() {
        Notification notification = new NotificationCompat.Builder(this, App.WASH_HANDS_CHANNEL)
                .setSmallIcon(R.drawable.hand)
                .setContentTitle(NOTIFICATION_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER).build();

        notificationManager.notify(2, notification);
        Calendar calendar = Calendar.getInstance();
        lastNotificationTime = calendar.getTime();
    }

    private void sendLocationBroadcast(Location location) {
        Intent local = new Intent();
        local.setAction("agh.edu.pl.washyourhands.current_location");
        local.putExtra("lat", location.getLatitude());
        local.putExtra("lng", location.getLongitude());
        sendBroadcast(local);
    }

    private void checkCurrentLocation(Location currentLocation) {
        Boolean atAnyHomeLocation = false;

        ArrayList<Location> userLocations = new ArrayList<>();

        for (LocationItem location: homeLocations) {
            userLocations.add(location.getLocation());
        }

        for(Location location: userLocations)
            if( location.distanceTo(currentLocation) < DISTANCE_DETLA ) {
                atAnyHomeLocation = true;
                break;
            }

        if( atAnyHomeLocation && !atHome ) {
            atHome = true;
            sendOnHandWashChannel();
        } else if (!atAnyHomeLocation && atHome) atHome = false;

        lastReadLocation = currentLocation;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(addLocationReceiver);
        unregisterReceiver(removeLocationReceiver);
        unregisterReceiver(getLocationsReceiver);
        unregisterReceiver(setIntervalReceiver);
        unregisterReceiver(getIntervalReceiver);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();

        State state = new State(homeLocations, interval, lastNotificationTime);

        Gson gson = new Gson();
        String stateJson = gson.toJson(state);
        editor.putString("state", stateJson);
        editor.commit();
    }

    private void restoreState() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Gson gson = new Gson();

        String emptyStateString = gson.toJson(new State());

        State savedState = gson.fromJson(sharedPref.getString("state", emptyStateString), State.class);

        homeLocations = savedState.getLocations();
        if (savedState.getInterval() != null)
            interval = savedState.getInterval();
        if (savedState.getLastNotificationTime() != null)
            lastNotificationTime = savedState.getLastNotificationTime();
    }

    //Icons made by <a href="https://www.flaticon.com/authors/freepik" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon"> www.flaticon.com</a>
}
