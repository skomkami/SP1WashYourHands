package agh.edu.pl.washyourhands.adapters;

import agh.edu.pl.washyourhands.R;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class LocationsAdapter extends RecyclerView.Adapter<LocationsAdapter.LocationsViewHolder> {

    private ArrayList<String> locationsList;
    private OnItemClickListener clickListener;

    public interface OnItemClickListener {
        void onItemClick(int pos);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        clickListener = listener;
    }

    public LocationsAdapter(ArrayList<String> locations) {
        locationsList = locations;
    }

    public static class LocationsViewHolder extends RecyclerView.ViewHolder {

        public TextView locationName;
        public Button deleteBtn;

        public LocationsViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);
            locationName = itemView.findViewById(R.id.location_name);
            deleteBtn = itemView.findViewById(R.id.delete_location);

            deleteBtn.setOnClickListener(v -> {
                if ( listener != null ) {
                    int position = getAdapterPosition();
                    if ( position != RecyclerView.NO_POSITION ) {
                        listener.onItemClick(position);
                    }
                }
            });
        }
    }

    @NonNull
    @Override
    public LocationsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.location_item, parent, false);
        LocationsViewHolder lvh = new LocationsViewHolder(v, clickListener);
        return lvh;
    }

    @Override
    public void onBindViewHolder(@NonNull LocationsViewHolder holder, int position) {
        String currentItem = locationsList.get(position);
        holder.locationName.setText(currentItem);
    }

    @Override
    public int getItemCount() {
        return locationsList.size();
    }
}
