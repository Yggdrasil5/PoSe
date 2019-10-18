package com.example.pose;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.LocationManager;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;

public class GnssData extends Service {

    public static final String LOG_TAG = "GNSS-DATA-LOGGER";
    public static final String BROADCAST_GNSS_DATA_LOGGER = "BROADCAST:" + LOG_TAG;
    public static final String MEASUREMENTS = "gnss-measurements";
    public static final String GPS_CLOCK = "gnss-clock";
Intent i;
Intent intent;

    LocationManager locationManager;

    private final GnssMeasurementsEvent.Callback gnns_Callback = new GnssMeasurementsEvent.Callback() {
        @Override
        public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
            super.onGnssMeasurementsReceived(eventArgs);

            ArrayList<GnssMeasurement> observation = new ArrayList<>(eventArgs.getMeasurements());
            GnssClock clock = eventArgs.getClock();

            Log.e("MainActivity", "Received " + observation.size() + " satellite signals");

        }

        Intent intent = new Intent(GnssData.BROADCAST_GNSS_DATA_LOGGER);
        Intent i = new Intent(GnssData.MEASUREMENTS);


    };



    //---- Class Methods

    private boolean hasPermission(String permission) {
        return this.getApplicationContext().checkSelfPermission(permission)
                == PackageManager.PERMISSION_GRANTED;
    }



    @Override @SuppressWarnings("MissingPermission") public int onStartCommand(Intent i, int f, int s) {
        //this.toast = new Toast(this.getApplicationContext());
        if (this.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            this.locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            if (this.locationManager != null) {
                this.locationManager.registerGnssMeasurementsCallback(this.gnns_Callback);
             //   this.locationManager.registerGnssNavigationMessageCallback(this.on_nav_data_received);
            }
        }
        //this.makeToast("GNSS data logger is running...");
        return Service.START_STICKY;
    }


    @Override public void onDestroy() {
        this.locationManager.unregisterGnssMeasurementsCallback(this.gnns_Callback);
        //this.locationManager.unregisterGnssNavigationMessageCallback(this.on_nav_data_received);
        //GnssData.this.makeToast("GNSS data logger stopped");
        Log.i(String.valueOf(this), "Stopped logging");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
