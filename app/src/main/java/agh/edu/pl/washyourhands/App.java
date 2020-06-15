package agh.edu.pl.washyourhands;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class App extends Application {
    public static final String APP_RUNNING_CHANNEL = "app_is_running";
    public static final String WASH_HANDS_CHANNEL = "wash_hands_notification";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel appIsRunningChannel = new NotificationChannel(
                    APP_RUNNING_CHANNEL,
                    "App is running",
                    NotificationManager.IMPORTANCE_LOW
            );

            appIsRunningChannel.setDescription("Will will remind you when its necessary to wash your hands.");

            NotificationChannel washHandsChannel = new NotificationChannel(
                    WASH_HANDS_CHANNEL,
                    "Wash your hands",
                    NotificationManager.IMPORTANCE_HIGH
            );

            washHandsChannel.setDescription("Don't forget to wash your hands :)");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(appIsRunningChannel);
            manager.createNotificationChannel(washHandsChannel);
        }
    }
}
