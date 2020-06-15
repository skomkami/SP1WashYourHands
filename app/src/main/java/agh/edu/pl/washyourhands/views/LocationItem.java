package agh.edu.pl.washyourhands.views;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

public class LocationItem implements Parcelable {

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public LocationItem createFromParcel(Parcel in) {
            return new LocationItem(in);
        }

        public LocationItem[] newArray(int size) {
            return new LocationItem[size];
        }
    };

    private String name;
    private Location location;

    public LocationItem(String name, Location location) {
        this.name = name;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public LocationItem(Parcel in){
        this.name = in.readString();
        this.location = in.readParcelable(Location.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeParcelable(this.location, 0);
    }
}
