package com.example.pose;

import android.app.Activity;
import android.content.Context;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssStatus;
import android.widget.TextView;

import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;


public class CalcEpoch {


   public static void calculation(ArrayList<GnssMeasurement> obs, GnssClock clk, long fullBiasNanos, gpsSat[] gpssatellites, ReceiverCoordinates receiverCoordinates, double[][] iono, Context context) {

        double pr, bias_empf =0;
        double[] newcoords = receiverCoordinates.getCoords();
        double[] solution = new double[]{0,0,0};
        double[] satCoords = new double[]{0, 0, 0,0};
        double[] deltaDistAtmos = new double[]{0,0};
        double[][] ttxTrxPr = new double[35][3];
        double[][] satCoordsArray  = new double[0][4];
        double xtemp= newcoords[0], ytemp=newcoords[1];
        Fundamentals fundamentals = new Fundamentals();

        //Piller 5 Steyrergasse 30
       double[] pillercoord = new double[]{4195390.4810, 1159800.6800, 4646944.4996};
       ReceiverCoordinates piller = new ReceiverCoordinates();
       piller.setCoords(pillercoord);
       //Reference in the Wood
       double[] wood = new double[]{4189486.600, 1178262.163, 4647673.390};
       ReceiverCoordinates refWood = new ReceiverCoordinates();
       refWood.setCoords(wood);
        //Reference on the Street
       double[] refstreet = new double[]{4189452.450, 1178361.237, 4647706.955};
       ReceiverCoordinates refStreet = new ReceiverCoordinates();
       refStreet.setCoords(refstreet);
       // end Reference Coords


        int gpssat = 0;
        for (int i =0; i < obs.size(); i++){
            if (obs.get(i).getConstellationType() == GnssStatus.CONSTELLATION_GPS){
                gpssat++;
            }
        }

       TextView tvGps = (TextView) ((Activity)context).findViewById(R.id.tvNumberOfGpsSats);
       tvGps.setText("# of GPS-Satellites: " + gpssat);


        if (gpssat > 3) { //For Testcasaes >0 otherwise >3
            int badsats = 0;

            for (int i = 0; i < obs.size(); i++) { //After TEST delete 1 and the comment
                GnssMeasurement delta = obs.get(i);

                int id = delta.getSvid();
                if (delta.getConstellationType() == GnssStatus.CONSTELLATION_GPS){ //Just for Constellation of GPS
                    solution = CalcPseudorange.calculation(delta, clk, fullBiasNanos); //[tTx, tRx, pr]
                    ttxTrxPr[id] = solution; //get tTx tRx and Pr in one ndimensional array
                   if(solution[2] > 1e6 && solution[2] < 1e8){
                        satCoords = CalcSatCoords.calculation(ttxTrxPr[id], gpssatellites, id); // einblenden wenn Test vorrüber
                        satCoordsArray = appendarray(satCoordsArray, satCoords);
                    }else {
                        badsats = badsats+1;
                    }
                }
                TextView tvDiscarded = (TextView) ((Activity)context).findViewById(R.id.tvDiscarded);
                tvDiscarded.setText("# of discarded Satellites: " + badsats);
            }

            double difference = 1;
            if(satCoordsArray.length > 3) {//TestCase
                while (difference >= 1e-6) {

                    double c = fundamentals.c;
                    double[][] A = new double[satCoordsArray.length][4];
                    double[] dl = new double[satCoordsArray.length];


                    for (int j = 0; j < satCoordsArray.length; j++) {
                        int id = (int) satCoordsArray[j][0];

                        //satCoordsArray [id],[delta_t_approx],[Xs],[Ys],[Zs]
                        double delta_t_aprox = satCoordsArray[j][1];
                        double rho = sqrt(pow(satCoordsArray[j][2] - newcoords[0], 2) + pow(satCoordsArray[j][3] - newcoords[1], 2) + pow(satCoordsArray[j][4] - newcoords[2], 2));

                        //Calculate Atmospheric Delay
                        deltaDistAtmos = CalcAtmosDelay.calculation(satCoordsArray[j], receiverCoordinates, iono, ttxTrxPr[j][0], (int) satCoordsArray[j][0]);
                        //deltaDistAtmos = new double[]{0,0};
                        //

                        pr = ttxTrxPr[id][2];
                        double range_corr = pr - c * bias_empf + c * delta_t_aprox + deltaDistAtmos[0] + deltaDistAtmos[1];

                        A[j][0] = -(satCoordsArray[j][2] - newcoords[0]) / rho;
                        A[j][1] = -(satCoordsArray[j][3] - newcoords[1]) / rho;
                        A[j][2] = -(satCoordsArray[j][4] - newcoords[2]) / rho;
                        A[j][3] = 1;

                        dl[j] = range_corr - rho;
                    }
                    //Gauß-Markov-Modell %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
                    RealVector dlv = MatrixUtils.createRealVector(dl);
                    RealMatrix Am = MatrixUtils.createRealMatrix(A);

                    RealMatrix At = Am.transpose();

                    RealMatrix N = At.multiply(Am); //AtPA
                    RealVector n_ = At.operate(dlv); //AtPdl

                    RealMatrix Ninverse = new LUDecomposition(N).getSolver().getInverse(); //Inverse of N

                    RealVector dxd = Ninverse.operate(n_);
                    double[] dxdarray = dxd.toArray();

                    newcoords[0] = newcoords[0] + dxdarray[0];
                    newcoords[1] = newcoords[1] + dxdarray[1];
                    newcoords[2] = newcoords[2] + dxdarray[2];
                    bias_empf = bias_empf + dxdarray[3] / c;
                    //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

                    //Differenz berechnen
                    difference = diffdist(newcoords[0], newcoords[1], xtemp, ytemp);
                    xtemp = newcoords[0];
                    ytemp = newcoords[1];
                }
                //GAUSS-MARKOV END-------------------------------------------------------------------

                receiverCoordinates.setCoords(newcoords);

                double[] difftoPillar = calcDiff(receiverCoordinates.getCoordsMgi(), piller.getCoordsMgi());
                double[] difftoStreet = calcDiff(receiverCoordinates.getCoordsMgi(), refStreet.getCoordsMgi());
                double[] difftoWood = calcDiff(receiverCoordinates.getCoordsMgi(), refWood.getCoordsMgi());

                TextView tvInfo = (TextView) ((Activity)context).findViewById(R.id.tvInfo);
                tvInfo.setText("Pillar: Diff Rechtsw.: " + String.format("%.3f", difftoPillar[0]) + " Diff Hochw.: " + String.format("%.3f", difftoPillar[1]));

                TextView tvInfo2 = (TextView) ((Activity)context).findViewById(R.id.tvInfo2);
                tvInfo2.setText("Street: Diff Rechtsw.: " + String.format("%.3f", difftoStreet[0]) + " Diff Hochw.: " + String.format("%.3f", difftoStreet[1]));

                TextView tvInfo3 = (TextView) ((Activity)context).findViewById(R.id.tvInfo3);
                tvInfo3.setText("Wood: Diff Rechtsw.: " + String.format("%.3f", difftoWood[0]) + " Diff Hochw.: " + String.format("%.3f", difftoWood[1]));

                TextView tvPhi = (TextView) ((Activity)context).findViewById(R.id.textViewPhi);
                TextView tvLam = (TextView) ((Activity)context).findViewById(R.id.textViewLam);

                tvPhi.setText("Phi: " + String.format("%.5f",receiverCoordinates.getCoordsEll()[0]));
                tvLam.setText("Lambda: " + String.format("%.5f",receiverCoordinates.getCoordsEll()[1]));

            }else {
                TextView tvInfo = (TextView) ((Activity)context).findViewById(R.id.tvInfo);
                tvInfo.setText("Bad Observations. "+ badsats +" Satellites had to be discarded");
            }

        }else{
            TextView tvInfo = (TextView) ((Activity)context).findViewById(R.id.tvInfo);
            tvInfo.setText("Not enough Satellites");
        }

   }

    private static double[] calcDiff(double[] coordsmeas, double[] piller) {
     double phidiff = abs(piller[0] - coordsmeas[0]);
     double lamdiff = abs(piller[1] - coordsmeas[1]);

     double[] diff = new double[]{phidiff, lamdiff};

     return diff;
    }


    private static double[][] appendarray(double[][] satCoordsArray, double[] satCoords) {
            int size = satCoordsArray.length;

            int j=0;
            if (size == 0 ) {
                double[][] returnArray = new double[size+1][4];
                returnArray[j] = satCoords;

                return returnArray;
            } else {
                double[][] returnArray = new double[size +1][4];
                for (int i = 0; i < size; i++) {
                    returnArray[i] = satCoordsArray[i];
                    j = j + 1;
                }

           returnArray[j] = satCoords;
           return returnArray;
            }
    }

    private static double diffdist(double xr, double yr, double xtemp, double ytemp){

            double difference = sqrt(pow((xtemp - xr),2) + pow((ytemp - yr),2));
            return difference;
    }
}
