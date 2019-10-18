package com.example.pose;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    gpsSat[] Gpssatellites = new gpsSat[33];
    String[] satellites;
    double[][] iono;
    private Intent intent;
    private boolean is_running;
    ReceiverCoordinates receiverCoordinates = new ReceiverCoordinates();

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {


        long fullBiasNanos = 0;

        @Override
        public void onReceive(Context context, Intent intent) {

            if (Objects.equals(intent.getAction(), GnssDataLogger.BROADCAST_GNSS_DATA_LOGGER)) {
                if (intent.hasExtra(GnssDataLogger.MEASUREMENTS)) {
                    ArrayList<GnssMeasurement> observation = intent.getParcelableArrayListExtra(GnssDataLogger.MEASUREMENTS);
                    GnssClock clk = intent.getParcelableExtra(GnssDataLogger.GPS_CLOCK);

                    // Creating an array of Objects of Type gpsSat.
                    // That means all gps sats are in this array.
                    // Access ex.: var = Gpssatellites[i].var

                    for (int i = 1; i < 33; i++) {
                        if (satellites[i] != null) {
                            Gpssatellites[i] = new gpsSat(satellites[i]);
                        }
                    }


                    Log.i(String.valueOf(this), "InOnReceive");

                    if (fullBiasNanos == 0) {
                        Log.i(String.valueOf(this), "InProcessEpoch1");
                        fullBiasNanos = clk.getFullBiasNanos();
                        CalcEpoch.calculation(observation, clk, fullBiasNanos, Gpssatellites, receiverCoordinates, iono, context);

                    } else if (fullBiasNanos != 0) {
                        Log.i(String.valueOf(this), "InProcessEpoch2");
                        CalcEpoch.calculation(observation, clk, fullBiasNanos, Gpssatellites, receiverCoordinates, iono, context);
                        actualizeMap();
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /** In the OnCreate Method, after permission is granted, the ephemeries will be downloaded
         *  and unzipped. This happens right at the beginning, so the calculation can then took place if the user wants to.
         *
         */
        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.INTERNET, Manifest.permission.ACCESS_FINE_LOCATION};
        requestPermissions(permissions,1);

        Calendar calendar = Calendar.getInstance();
        int doy = calendar.get(Calendar.DAY_OF_YEAR);

        String temp = null;

        if(doy < 100){
            temp = "0" + String.valueOf(doy);
        }else{
            temp = String.valueOf(doy);
        }
        int year = calendar.get(Calendar.YEAR);

        String host = "cddis.gsfc.nasa.gov";
        String filename = "/gnss/data/daily/" + String.valueOf(year) + "/brdc/brdc" + temp + "0.19n.Z";
        String user = "anonymous";
        String pass = "nothing@";
        String downpath = "/storage/emulated/0/ephemeries/latestgps.Z";
        String unzippath = "/storage/emulated/0/ephemeries/latestgps.txt";

        try {
            dataHandler.DownloadAndUnzipEphemeries(host, filename, user, pass, downpath, unzippath); //Starts automatically when apk is started and downloads the latest ephemeries File
            Log.i(String.valueOf(this), "Satellitedata downloaded and unzipped successfully");
            readRinex();
            Log.i(String.valueOf(this), "Satellite and Ionosphere Data read successfully");
        }catch (IOException e){
            e.printStackTrace();
        }

        // Setup intent for the GNSS logger service:
        this.intent = new Intent(this, GnssDataLogger.class);
        this.is_running = false;

        // Setup broadcast receiver for data reception from the GNSS logger service:
        this.registerReceiver(this.broadcastReceiver, new IntentFilter(GnssDataLogger.BROADCAST_GNSS_DATA_LOGGER));

        configureChangeActivityButton();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    public void startStop(View view) throws IOException {

        if (!this.is_running) {
            if (readRinex()) {
                this.startService(this.intent);
                Button button = findViewById(R.id.startStopBtn);
                this.is_running = true;
                button.setText("Stop");
            }
        }else {
            this.stopService(this.intent);
            Button button = findViewById(R.id.startStopBtn);
            this.is_running = false;
            button.setText("Start");
            dataHandler.writePosition(receiverCoordinates.coordlist); //Writes List of Positions in a file
            TextView tv = findViewById(R.id.tvInfo);
            tv.setText("Info");
        }
    }

    public void configureChangeActivityButton() {
        Button btn = findViewById(R.id.changeActivBtn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Launch second activity
                Intent intent = new Intent(MainActivity.this, activity_quality.class);

                List<double[]> coordlist = receiverCoordinates.coordlist;
                intent.putExtra("ReceiverCoordinates", receiverCoordinates);
                startActivity(intent);
            }
        });
    }


    public boolean readRinex()throws  IOException{
        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.INTERNET};
        requestPermissions(permissions,1);

        File infile = new File("/storage/emulated/0/ephemeries/latestgps.txt");
        try {
            satellites = readfile.readSatellites(infile);
            iono = readfile.readIonosphere(infile);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng TUGraz = new LatLng(47.06427, 15.4513);
        mMap.addMarker(new MarkerOptions().position(TUGraz).title("Marker in Graz"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(TUGraz,12));
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    private void actualizeMap() {
        mMap.clear();
        LatLng actual = new LatLng(receiverCoordinates.getCoordsEll()[0], receiverCoordinates.getCoordsEll()[1]);
        mMap.addMarker(new MarkerOptions().position(actual).title("Actual Position"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(actual,14));
    }
}
