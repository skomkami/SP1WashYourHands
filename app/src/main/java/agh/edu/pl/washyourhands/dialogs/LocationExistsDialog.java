package agh.edu.pl.washyourhands.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatDialogFragment;

public class LocationExistsDialog extends AppCompatDialogFragment {

    private LocationExistsDialogListener listener;

    private String locationName;
    private Location location;

    public LocationExistsDialog(String locationName, Location location)
    {
        this.locationName = locationName;
        this.location = location;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Warning")
                .setMessage("Location with given name already exists. Do you want to override it?")
                .setNegativeButton("Cancel", (dialog, which) -> {

                })
                .setPositiveButton("Override", (dialog, which) -> {
                    listener.onOverrideClicked(this.locationName, this.location);
                });

        return builder.create();
    }

    public interface LocationExistsDialogListener {
        void onOverrideClicked(String locationName, Location location);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (LocationExistsDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
             + "must implement LocationExistsDialogListener");
        }
    }
}
