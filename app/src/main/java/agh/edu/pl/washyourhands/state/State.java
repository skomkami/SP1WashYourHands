package agh.edu.pl.washyourhands.state;

import agh.edu.pl.washyourhands.views.LocationItem;

import java.util.ArrayList;
import java.util.Date;

public class State {
    private ArrayList<LocationItem> locations = new ArrayList<>();
    private Long interval;
    private Date lastNotificationTime;

    public ArrayList<LocationItem> getLocations() {
        return locations;
    }

    public void setLocations(ArrayList<LocationItem> locations) {
        this.locations = locations;
    }

    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }

    public Date getLastNotificationTime() {
        return lastNotificationTime;
    }

    public void setLastNotificationTime(Date lastNotificationTime) {
        this.lastNotificationTime = lastNotificationTime;
    }

    public State(ArrayList<LocationItem> locations, Long interval, Date lastNotificationTime) {
        this.locations = locations;
        this.interval = interval;
        this.lastNotificationTime = lastNotificationTime;
    }

    public State() {
    }
}
