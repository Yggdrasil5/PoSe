package com.example.pose;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.LocationManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Locale;

public class GnssDataLogger extends Service {

    /* ##########################################################################################
     *
     * C L A S S   M E M B E R   V A R I A B L E S
     *
     * ########################################################################################## */


    // ------------------------------------------------------------------------< Public constants >-
    public static final String LOG_TAG = "GNSS-DATA-LOGGER";
    public static final String BROADCAST_GNSS_DATA_LOGGER = "BROADCAST:" + LOG_TAG;
    public static final String MEASUREMENTS = "gnss-measurements";
    public static final String GPS_CLOCK = "gnss-clock";
    public static final String NAVIGATION_DATA = "gnss-navigation-data";

    private Toast toast; // Used to display floating messages on screen

    // -------------------------------------------------------------------------< LocationManager >-
    private LocationManager loc_mngr;

    // ------------------------------------------------------------------------< Measurement file >-
    private FileWriter gnss_rawdata_logfile;

    // ---------------------------------------------------------< Callback: GNSS measurement data >-
    private final GnssMeasurementsEvent.Callback on_obs_data_received = new GnssMeasurementsEvent.Callback() {
        @Override
        public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
            super.onGnssMeasurementsReceived(eventArgs);

            ArrayList<GnssMeasurement> obs = new ArrayList<>(eventArgs.getMeasurements());
            GnssClock clk = eventArgs.getClock();

            Log.e("MainActivity", "Received " + obs.size() + " satellite signals");
            for (GnssMeasurement m : obs) {

                String str = String.format(Locale.ENGLISH, "Raw,%d,%d,%d,%f,%d,%f,%f,%f,%f,%d,%d,%f,%d,%d,%d,%f,%f,%f,%d,%f,%f,%f,%d,%f,%f,%d,%f,%d,%f,%f\n", SystemClock.elapsedRealtime()
                        ,clk.getTimeNanos()
                        ,clk.getLeapSecond()
                        ,clk.getTimeUncertaintyNanos()
                        ,clk.getFullBiasNanos()
                        ,clk.getBiasNanos()
                        ,clk.getBiasUncertaintyNanos()
                        ,clk.getDriftNanosPerSecond()
                        ,clk.getDriftUncertaintyNanosPerSecond()
                        ,clk.getHardwareClockDiscontinuityCount()
                        ,m.getSvid()
                        ,m.getTimeOffsetNanos()
                        ,m.getState()
                        ,m.getReceivedSvTimeNanos()
                        ,m.getReceivedSvTimeUncertaintyNanos()
                        ,m.getCn0DbHz()
                        ,m.getPseudorangeRateMetersPerSecond()
                        ,m.getPseudorangeRateUncertaintyMetersPerSecond()
                        ,m.getAccumulatedDeltaRangeState()
                        ,m.getAccumulatedDeltaRangeMeters()
                        ,m.getAccumulatedDeltaRangeUncertaintyMeters()
                        ,m.getCarrierFrequencyHz()
                        ,m.getCarrierCycles()
                        ,m.getCarrierPhase()
                        ,m.getCarrierPhaseUncertainty()
                        ,m.getMultipathIndicator()
                        ,m.getSnrInDb()
                        ,m.getConstellationType()
                        , Double.NaN // AGC
                        ,m.getCarrierFrequencyHz());

                String new_str = str.replace("NaN","");
            }

            Intent intent = new Intent(GnssDataLogger.BROADCAST_GNSS_DATA_LOGGER);
            intent.putParcelableArrayListExtra(MEASUREMENTS, obs);
            intent.putExtra(GnssDataLogger.GPS_CLOCK, eventArgs.getClock());
            GnssDataLogger.this.sendBroadcast(intent);
        }

        @Override
        public void onStatusChanged(int status) {
            super.onStatusChanged(status);
//                GnssDataLogger.this.makeToast("Status of observation messages: " + status);
        }
    };

    // ----------------------------------------------------------< Callback: GNSS navigation data >-
    private final GnssNavigationMessage.Callback on_nav_data_received = new GnssNavigationMessage.Callback() {
        @Override
        public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
            super.onGnssNavigationMessageReceived(event);

            Intent intent = new Intent(GnssDataLogger.BROADCAST_GNSS_DATA_LOGGER);
            intent.putExtra(GnssDataLogger.NAVIGATION_DATA, event);
            GnssDataLogger.this.sendBroadcast(intent);
        }

        @Override
        public void onStatusChanged(int status) {
            super.onStatusChanged(status);
//                GnssDataLogger.this.makeToast("Status of navigation messages: " + status);
        }
    };


    /* ##########################################################################################
     *
     * C L A S S   M E T H O D S
     *
     * ########################################################################################## */

    // --------------------------------------------------------------------------< has_permission >-
    private boolean hasPermission(String permission) {
        return this.getApplicationContext().checkSelfPermission(permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    // ------------------------------------------------------------------------------< make_toast >-
    private void makeToast(String text) {
        this.toast.cancel();
        this.toast = Toast.makeText(this.getApplicationContext(), text, Toast.LENGTH_SHORT);
        this.toast.show();
    }

    // --------------------------------------------------------------------------< onStartCommand >-
    @Override
    @SuppressWarnings("MissingPermission") public int onStartCommand(Intent i, int f, int s) {
        this.toast = new Toast(this.getApplicationContext());
        if (this.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            this.loc_mngr = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            if (this.loc_mngr != null) {
                this.loc_mngr.registerGnssMeasurementsCallback(this.on_obs_data_received);
                this.loc_mngr.registerGnssNavigationMessageCallback(this.on_nav_data_received);
            }
        }
        this.makeToast("GNSS data logger is running...");
        return Service.START_STICKY;
    }

    // ----------------------------------------------------------------------------< writeRawData >-
  /*  private void writeRawData(String text) {
        try {
            this.gnss_rawdata_logfile.write(text);
            this.gnss_rawdata_logfile.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
*/
    // -------------------------------------------------------------------------------< onDestroy >-
    @Override
    public void onDestroy() {
        this.loc_mngr.unregisterGnssMeasurementsCallback(this.on_obs_data_received);
        this.loc_mngr.unregisterGnssNavigationMessageCallback(this.on_nav_data_received);
        GnssDataLogger.this.makeToast("GNSS data logger stopped");
    }

    // ----------------------------------------------------------------------------------< onBind >-
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
