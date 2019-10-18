package com.example.pose;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.softmoore.android.graphlib.Graph;
import com.softmoore.android.graphlib.GraphView;
import com.softmoore.android.graphlib.Point;

import java.util.ArrayList;
import java.util.List;


import static java.lang.Math.pow;
import static java.lang.Math.sqrt;


public class activity_quality extends AppCompatActivity {
    List<Point> pointlist = new ArrayList<Point>();
    double[] stddev = new double[]{0,0,0,0,0}; //{Std East, Std North, Std 2D, SemEast, SemNorth}


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quality);
        Intent intent = getIntent();
        ReceiverCoordinates coords = intent.getExtras().getParcelable("ReceiverCoordinates");

        configBackButton();

        if (coords.coordlist.isEmpty()) {
            ArrayList<double[]> coordlist = new ArrayList<double[]>();
            double[] tempCoords = new double[]{0, 0};
            coordlist.add(tempCoords);
            double[] tempMean = new double[]{0, 0};
            createGraphView(coordlist, tempMean);

        } else {
            createGraphView(coords.coordlist, coords.getCoordsMean());
        }

        if (coords.coordlist.size() > 3) {
            stddev = calcStd(coords.coordlist);
        }

        showStdDevs();

    }

    private void showStdDevs() {
        TextView tvStdEast = findViewById(R.id.valueEastTv);
        tvStdEast.setText(String.format("%.3f", stddev[0]));

        TextView tvStdNorth = findViewById(R.id.valueNorthTv);
        tvStdNorth.setText(String.format("%.3f", stddev[1]));

        TextView tvStdTwoD = findViewById(R.id.value2DTv);
        tvStdTwoD.setText(String.format("%.3f", stddev[2]));

        TextView tvSemEast = findViewById(R.id.valueSemEastTv);
        tvSemEast.setText(String.format("%.3f", stddev[3]));

        TextView tvSemNorth = findViewById(R.id.valueSemNorthTv);
        tvSemNorth.setText(String.format("%.3f", stddev[4]));
    }

    private double[] calcStd(List<double[]> coordlist) {
        double sumy = 0 ; double sumx = 0; double sem =0; double twoD=0;
        double sumstdx = 0; double sumstdy = 0;
        double n = coordlist.size();


        for (double[] item: coordlist){
            sumx += item[1];
            sumy += item[0];
        }
        double ym = sumy / n; //Mean Values of x and y
        double xm = sumx / n;

        for (double[] item: coordlist){
            sumstdx += pow((item[1] - xm),2);
            sumstdy += pow((item[0] - ym),2);
            twoD += pow((item[0] - ym),2) + pow((item[1] - xm),2);
        }
        double vary  = (1 / (n-1)) * sumstdy;
        double varx = (1 / (n-1)) * sumstdx;
        double varTwoD = (1 / (n-1)) * twoD;


        double sy = sqrt(vary);
        double sx = sqrt(varx);

        double semy = sy / sqrt(n);
        double semx = sx / sqrt(n);

        double stdTwoD = sqrt(varTwoD);

        double [] deviations = new double[]{sx, sy, stdTwoD, semx, semy}; //{Std East, Std North, Std 2D, SemEast, SemNorth}

        return deviations;
    }

    private void configBackButton(){
        Button btn = findViewById(R.id.backButton);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    public void createGraphView(List<double[]> coords, double[] coordsMean) {

        pointlist = createPointlist(coords, coordsMean);

        Graph graph = new Graph.Builder()
                .setWorldCoordinates(-21,21,-21,21)
                .addPoints(pointlist)
                .setXTicks(new double[]{-20, -15, -10, -6, -4, -2, 2, 4, 6, 10, 15, 20})
                .setYTicks(new double[]{-20, -15, -10, -6, -4, -2, 2, 4, 6, 10, 15, 20})
                .build();

        GraphView graphView = findViewById(R.id.graphView);
        graphView.setGraph(graph);
        setTitle("Quality Parameters");
    }

    private List<Point> createPointlist(List<double[]> coords, double[] coordsMean) {

        for (double[] item: coords) {
            Point temppoint = new Point(item[1] - coordsMean[1], item[0] - coordsMean[0]);
            pointlist.add(temppoint);
        }
        return pointlist;
    }

}
