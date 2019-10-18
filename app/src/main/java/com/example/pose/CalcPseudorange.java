package com.example.pose;

import android.location.GnssClock;
import android.location.GnssMeasurement;

public class CalcPseudorange {


    public static double[] calculation(GnssMeasurement delta, GnssClock clk, long fullBiasNanos) {

        /*
        // Für Testzwecke Werte manuell anlegen (Matlab vergleich)

        long fullBiasNanostest = -1201938101452638922L;
        double[] timeNanos = new double[3];
        timeNanos[0] = 4.054700000000000e10;  timeNanos[1] = 4.154700000000000e10;
        double[] receivedTimeNanos = new double[34];
        receivedTimeNanos[2] = 2.005419179677040e14; receivedTimeNanos[3] = 2.005419271017240e+14; receivedTimeNanos[6] = 2.005419280943970e+14; receivedTimeNanos[9] = 2.005419310012090e+14; receivedTimeNanos[17] = 2.005419170672640e+14;
        double[] timeOffsetNanos = new double[3];
        timeOffsetNanos[0] = 0; timeOffsetNanos[1] = 0;

        double[][] ttxTrxPr = new double[34][3];
        int[] ids = new int[]{2, 3, 6, 9, 17};


        //---------------------------------------------------------------------------
*/
        long tRx_temp, bias;
        double tTx, tRx, pr;
        double retVec[] = new double[3];


        Fundamentals fundamentals = new Fundamentals();
        double c = fundamentals.c;
        c=c*1e-9; //In Nanosec

        //for (int i=0; i<5; i++) { //Test
           // double ttx = receivedTimeNanos[ids[i]] + timeOffsetNanos[0]; //Test
            tTx =  delta.getReceivedSvTimeNanos() + delta.getTimeOffsetNanos();  //ReceivedSVTime -> long, TimeOffset -> double

            //long bia = fullBiasNanostest;//Test
            //long trxtemp = (long)timeNanos[0] - bia;//Test

            bias = fullBiasNanos + (long)clk.getBiasNanos();  //FullBiasNanos -> long, BiasNanos -> double
            tRx_temp = clk.getTimeNanos() - bias;

            //tRx umrechnen auf TOW
            long WEEKSEC = (long) 604800e9;
            tRx = (tRx_temp % WEEKSEC);

            //long trx = (trxtemp % WEEKSEC); //Test
            //double trxdoubl = (double) trx; //Test

            pr = (tRx - tTx) * c;

            //double prtest = (trxdoubl -ttx)*c;


            retVec[0] = tTx;
            retVec[1] = tRx;
            retVec[2] = pr;

            //retVec[0] = ttx;//Test
            //retVec[1] = trx; //Test
            //retVec[2] = prtest; //Test

            //int id = ids[i];
            //ttxTrxPr[id][0] = ttx; //Test
            //ttxTrxPr[id][1] = trx; //Test
            //ttxTrxPr[id][2] = prtest; //Test

        //}//Test
        //return ttxTrxPr;//Test
        return retVec;
    }

}
