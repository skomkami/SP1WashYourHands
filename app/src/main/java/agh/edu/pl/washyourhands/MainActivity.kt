package agh.edu.pl.washyourhands

import agh.edu.pl.washyourhands.enums.Actions
import android.Manifest
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    var intervalField: TextView? = null
    var setIntervalBtn: Button? = null
//    var locationField: TextView? = null
    var manageLocationsBtn: Button? = null
    var remindServiceToggle: Switch? = null
    var intervalInMinutes: Long = 0

    private var updateUIReceiver: BroadcastReceiver? = null

    private fun timeToString(hr: Int, min: Int): String {
        return ((hr/10)%10).toString() + (hr%10).toString() + ":" + ((min/10)%10).toString() + (min%10).toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        intervalField = findViewById(R.id.txt_set_interval)
        setIntervalBtn = findViewById(R.id.set_interval)
//        locationField = findViewById(R.id.txt_location)
        manageLocationsBtn = findViewById(R.id.manage_locations)
        remindServiceToggle = findViewById(R.id.remind_service_toggle)

        setIntervalBtn!!.setOnClickListener { _ ->
            run {
                val onTimeSetListener: TimePickerDialog.OnTimeSetListener = TimePickerDialog.OnTimeSetListener { _, hr, min ->
                    intervalInMinutes = (60*hr+min).toLong()
                    intervalField!!.text = timeToString(hr, min)
                    val setInterval = Intent()
                    setInterval.action = "agh.edu.pl.washyourhands.set_interval"
                    val intervalMilliseconds = TimeUnit.MILLISECONDS.convert((hr * 60 + min).toLong(), TimeUnit.MINUTES)
                    setInterval.putExtra("interval", intervalMilliseconds)
                    sendBroadcast(setInterval)
                    sendGetInterval()
                }
                val hours: Int = (intervalInMinutes/60).toInt()
                val minutes: Int = (intervalInMinutes%60).toInt()
                val timePicker: TimePickerDialog = TimePickerDialog(this, TimePickerDialog.THEME_HOLO_LIGHT, onTimeSetListener, hours, minutes, true)
                timePicker.show()
                //val (hours, minutes) = intervalField!!.text.split(":").map { it.toLong() }
                //val intervalMilliseconds = TimeUnit.MILLISECONDS.convert(hours * 60 + minutes, TimeUnit.MINUTES)
            }
        }

        remindServiceToggle!!.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startService()
            } else {
                stopService()
            }
        }

        manageLocationsBtn!!.setOnClickListener { _ ->
            run {
                val secondActivity = Intent(this, ManageLocations::class.java)
                startActivity(secondActivity)
            }
        }

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startService()
            } else requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else startService()

        registerUIReceiver()
        sendGetInterval()
    }



    private fun startService() {
        val intent = Intent(this, RemindService::class.java)
        intent.action = Actions.START.name
        startService(intent)
    }

    private fun stopService() {
        val intent = Intent(this, RemindService::class.java)
        intent.action = Actions.STOP.name
        startService(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            1 -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    startService()
                else
                    Toast.makeText(
                        this,
                        "Please, give application permission to access your location",
                        Toast.LENGTH_LONG
                    ).show()
            }
            else -> finishAndRemoveTask()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(updateUIReceiver);
    }

    private fun sendGetInterval() {
        val getInterval = Intent()
        getInterval.action = "agh.edu.pl.washyourhands.get_interval"
        sendBroadcast(getInterval)
}

    private fun registerUIReceiver() {
        val filter = IntentFilter()
        filter.addAction("agh.edu.pl.washyourhands.current_location")
        filter.addAction("agh.edu.pl.washyourhands.interval")

        updateUIReceiver = createUpdateUIReceiver()

        registerReceiver(updateUIReceiver, filter)
    }

    private fun createUpdateUIReceiver() = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
//                if (intent.hasExtra("lat") && intent.hasExtra("lng"))
//                    locationField!!.text =
//                        intent.getDoubleExtra("lat", 0.00).toString() + "/" + intent.getDoubleExtra("lng", 0.00)

                if (intent.hasExtra("interval")) {
                    val interval = intent.getLongExtra("interval", 0L);
                    intervalInMinutes = TimeUnit.MINUTES.convert(interval, TimeUnit.MILLISECONDS)
                    val hours = intervalInMinutes / 60
                    val minutes = intervalInMinutes % 60
                    intervalField!!.text = timeToString(hours.toInt(), minutes.toInt())
                }
            }
        }
    }
}
