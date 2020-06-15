package agh.edu.pl.washyourhands;

import agh.edu.pl.washyourhands.adapters.LocationsAdapter;
import agh.edu.pl.washyourhands.dialogs.LocationExistsDialog;
import agh.edu.pl.washyourhands.views.LocationItem;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class ManageLocations extends AppCompatActivity implements LocationExistsDialog.LocationExistsDialogListener {

    public static final int SELECT_LOCATION_REQUEST_CODE = 1;

    private Button addCurrentLocationBtn;

    private Button selectLocationBtn;

    private RecyclerView recyclerView;
    private LocationsAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private ArrayList<String> locationsList = new ArrayList<>();

    private BroadcastReceiver updateUIReceiver;

    private Location selectedLocation = null;

    private FusedLocationProviderClient fusedLocationProviderClient;

    private Dialog myDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_locations);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        myDialog = new Dialog(this);

        Toolbar toolbarLocator = findViewById(R.id.toolbar);
        setSupportActionBar(toolbarLocator);
        getSupportActionBar().setTitle("Manage locations");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        addCurrentLocationBtn = findViewById(R.id.add_location);
        selectLocationBtn = findViewById(R.id.select_location);
        selectedLocation = new Location(LocationManager.GPS_PROVIDER);

        addCurrentLocationBtn.setOnClickListener(v -> {
            fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        selectLocationName(location);
                    }
                });
        });

        fusedLocationProviderClient.getLastLocation()
            .addOnSuccessListener(this, location -> {
                if (location != null) {
                    selectedLocation = location;
                }
            });

        selectLocationBtn.setOnClickListener(v -> {
            Intent selectLocation = new Intent(this, SelectLocationActivity.class);
            selectLocation.putExtra("init_location", selectedLocation);
            startActivityForResult(selectLocation, SELECT_LOCATION_REQUEST_CODE);
        });
        buildRecycleView();
        registerUIReceiver();
        sentGetLocationsRequest();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ( resultCode == RESULT_OK ) {
            if ( requestCode == SELECT_LOCATION_REQUEST_CODE && data.hasExtra("selected_location")) {
                LatLng sl = data.getParcelableExtra("selected_location");
                selectedLocation.setLatitude(sl.latitude);
                selectedLocation.setLongitude(sl.longitude);
                Log.d("test", "Selected location from google map: " + selectedLocation.getLongitude() + ", " + selectedLocation.getLatitude());
                selectLocationName(selectedLocation);
            }
        }
    }

    private void selectLocationName(Location location) {
        myDialog.setContentView(R.layout.select_location_name);
        Button addLocationBtn = myDialog.findViewById(R.id.add_location_button);
        EditText locationName = myDialog.findViewById(R.id.location_name_field);
        addLocationBtn.setOnClickListener(v -> {
            String newLocationName =  locationName.getText().toString();
            if (newLocationName.equals("")) {
                Toast.makeText(this, "Enter new location name", Toast.LENGTH_SHORT).show();
                return;
            }
            addLocation(newLocationName, location);
            myDialog.dismiss();
        });
        myDialog.show();
    }

    private void addLocation(String newLocationName, Location location) {
        if(locationsList.contains(newLocationName)) {
            LocationExistsDialog dialog = new LocationExistsDialog(newLocationName, location);
            dialog.show(getSupportFragmentManager(), "location_exists_dialog");
        } else {
            addLocationToList(newLocationName);
            sendAddLocationBroadcast(newLocationName, location);
        }
    }

    private void addLocationToList(String newLocationName) {
        locationsList.add(newLocationName);
        adapter.notifyItemInserted(locationsList.size() - 1);
    }

    private void buildRecycleView() {
        recyclerView = findViewById(R.id.locations_list);
        layoutManager = new LinearLayoutManager(this);
        adapter = new LocationsAdapter(locationsList);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(pos -> {
            String locationToRemove = locationsList.get(pos);
            locationsList.remove(pos);
            adapter.notifyItemRemoved(pos);
            sendDeleteLocationBroadcast(locationToRemove);
        });
    }

    private void sendAddLocationBroadcast(String locationName, Location location) {
        Intent addLocation = new Intent();
        addLocation.setAction("agh.edu.pl.washyourhands.add_location");

        LocationItem locationItem = new LocationItem(locationName, location);

        addLocation.putExtra("location_item", locationItem);
        sendBroadcast(addLocation);
    }

    private void sendDeleteLocationBroadcast(String locationName) {
        Intent deleteLocation = new Intent();
        deleteLocation.setAction("agh.edu.pl.washyourhands.remove_location");
        deleteLocation.putExtra("location_name", locationName);
        sendBroadcast(deleteLocation);
    }

    private void sentGetLocationsRequest() {
        Intent getLocations = new Intent();
        getLocations.setAction("agh.edu.pl.washyourhands.get_locations");
        sendBroadcast(getLocations);
    }

    private void registerUIReceiver() {
        IntentFilter updateUIFilter = new IntentFilter();
        updateUIFilter.addAction("agh.edu.pl.washyourhands.locations");

        updateUIReceiver = new BroadcastReceiver() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    if (intent.hasExtra("locations")) {
                        ArrayList<String> locationsFromService = intent.getStringArrayListExtra("locations");
                        locationsFromService.forEach( l -> addLocationToList(l));
                    }
                }
            }
        };

        registerReceiver(updateUIReceiver, updateUIFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myDialog.dismiss();
        unregisterReceiver(updateUIReceiver);
    }

    @Override
    public void onOverrideClicked(String locationName, Location location) {
        sendDeleteLocationBroadcast(locationName);
        sendAddLocationBroadcast(locationName, location);
    }
}
