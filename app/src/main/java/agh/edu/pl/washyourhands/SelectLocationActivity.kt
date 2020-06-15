package agh.edu.pl.washyourhands

import android.app.Activity
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnSuccessListener

class SelectLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        val TAG = "SelectLocationActivity"
    }

    private lateinit var mMap: GoogleMap
    private var marker: Marker? = null
    private var latLng: LatLng? = null
    private var selectBtn: Button? = null
    private var myLocationBtn: Button? = null

    //widgets
    private var mSearchText: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_location)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

//        moveMyLocationBtn(mapFragment)

        mSearchText = findViewById(R.id.input_search)
        selectBtn = findViewById(R.id.set_location_btn)
        selectBtn!!.setOnClickListener { selectLocation() }
        myLocationBtn = findViewById(R.id.my_location_btn)
        myLocationBtn!!.setOnClickListener {
            var fusedLocationProviderClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationProviderClient.lastLocation
                .addOnSuccessListener(this,
                    OnSuccessListener { location: Location? ->
                        if (location != null)
                        {
                            latLng = LatLng(location!!.latitude, location!!.longitude)
                            marker!!.position = latLng
                            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng!!))
                        }
                    }
                )
        }

    }

//    private fun moveMyLocationBtn(mapFragment: SupportMapFragment) {
//        val locationBtn = (mapFragment!!.view?.findViewById<View>(Integer.parseInt("1"))?.parent as View)
//            .findViewById<View>(Integer.parseInt("2"))
//
//        val rlp =  locationBtn.getLayoutParams() as RelativeLayout.LayoutParams
//        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
//        rlp.addRule(RelativeLayout.ALIGN_RIGHT, RelativeLayout.TRUE)
//        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
//        rlp.setMargins(0, 200, 50, 0)
//    }

    private fun initSearch(){
        mSearchText!!.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(text: String): Boolean {
                if(text.isEmpty()) return false
                geoLocate()
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return true
            }
        })
        /*{ _: TextView, actionId: Int, keyEvent: KeyEvent ->
            if(actionId == EditorInfo.IME_ACTION_SEARCH
                || actionId == EditorInfo.IME_ACTION_DONE
                || keyEvent.action == KeyEvent.ACTION_DOWN
                || keyEvent.action == KeyEvent.KEYCODE_ENTER
            ) geoLocate()

            false
        }*/
    }

    private fun geoLocate() {
        val searchString = mSearchText!!.query.toString()
        val geocoder: Geocoder = Geocoder(this)

        var list: MutableList<Address> = mutableListOf()
        try {
           list = geocoder.getFromLocationName(searchString, 1)
        } catch (e: Exception) {
            Log.e(TAG, "geoLocate: " + e.message)
        }

        if(list.size > 0) {
            val address: Address = list[0]
            Log.d(TAG, "geoLocate: found a location: " + address.toString())

            mMap.animateCamera(CameraUpdateFactory.newLatLng(LatLng(address.latitude, address.longitude)))
        }

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if ( intent.hasExtra("init_location")) {
            val loc: Location? = intent.getParcelableExtra("init_location")
            latLng = LatLng(loc!!.latitude, loc!!.longitude)
        }

        marker = mMap.addMarker(MarkerOptions().position(latLng!!).title("Select this location"))
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng!!, 15F))
//        mMap.animateCamera(CameraUpdateFactory.zoomTo(15F), 500, null);

        initSearch()

        mMap.setOnMapClickListener { latlng ->
            if (marker != null) {
                marker!!.remove()
            }
            marker = mMap.addMarker(
                MarkerOptions()
                    .position(latlng)
                    .icon(
                        BitmapDescriptorFactory
                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
            )

            latLng = latlng
            println(latlng)
        }
    }

    private fun selectLocation(): Boolean {
        val resultIntent = Intent()
        resultIntent.putExtra("selected_location", latLng!!)
        setResult(Activity.RESULT_OK, resultIntent)

        finish()
        return true
    }
}
