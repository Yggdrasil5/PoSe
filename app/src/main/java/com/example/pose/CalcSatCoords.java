package com.example.pose;


import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import static java.lang.Math.abs;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;


class CalcSatCoords {


    public static double[] calculation(double[] ttxTrxPr, gpsSat[] gpssatellites, int id) {
        Fundamentals fundamentals = new Fundamentals();
        double GM = fundamentals.GM;
        double tTx, tRx, pr, t, t0, tk, Mk, Ek, biasSat, delta_rel, delta_t_approx, tk_corr, vk, u0, uk, r0, rk, ik, lamk, omegaECSF, tb, rotation_earth;


        //Convert to Seconds
        tTx = ttxTrxPr[0]*1e-9; //Auskommentiert wegen Testzwecke
        tRx = ttxTrxPr[1]*1e-9;

        //tTx = ttxTrxPr[0];//Test
        //tRx = ttxTrxPr[1];//Test


        tk = tTx;
        t0 = gpssatellites[id].toeSecond;
        t = tk- t0;  //t = tk-t0;


        //Mean and Excentric Anomalie
        Mk = MeanAnomalie(tk, gpssatellites, id, GM, t);

        Ek = ExcentricAnomalie(Mk, gpssatellites, id);

        //Clock Error calculation and correction %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        //Clockpolynom
        biasSat = gpssatellites[id].SvClockBias + gpssatellites[id].SvClockDrift*t + gpssatellites[id].SvClockDriftRate*pow(t,2);
        //Model of Satelliteclockerror
        delta_rel = fundamentals.rel * gpssatellites[id].e * gpssatellites[id].SqrtA * sin(Ek);

        delta_t_approx = biasSat + delta_rel;
        //Corrected Satelliteclockerror
        tk_corr = tTx - delta_t_approx;
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

        //Calculation of Satellite Coordinates in TRS

        // Calculate Mk and Ek again with correctet clock------------------------------------
        Mk = MeanAnomalie(tk, gpssatellites, id, GM, (tk_corr - t0));
        Ek = ExcentricAnomalie(Mk, gpssatellites, id);
        // ----------------------------------------------------------------------------------
        //True Anomalie
        vk = atan2((sqrt(1- pow(gpssatellites[id].e,2))*sin(Ek)) , (cos(Ek) - gpssatellites[id].e));
        //Approx. argument of latitude
        u0 = gpssatellites[id].omega0 + vk;
        //Corrected argument of perigee
        uk = u0 + gpssatellites[id].Cuc * cos(2*u0) + gpssatellites[id].Cus * sin(2*u0);
        //approx. Satelliteorbit radius
        r0 = pow(gpssatellites[id].SqrtA,2) * (1 - (gpssatellites[id].e * cos(Ek)));
        //Satelliteorbit radius
        rk = r0 + gpssatellites[id].CrC * cos(2*u0) + gpssatellites[id].Crs*sin(2*u0);
        //Corrected inklination
        ik = gpssatellites[id].i0 + gpssatellites[id].Cic * cos(2*u0) + gpssatellites[id].Cis * sin(2*u0) + gpssatellites[id].idot * (tk_corr - t0);
        //Rectas. ascending note ECEF
        lamk = gpssatellites[id].l0 + (gpssatellites[id].Omegadot - fundamentals.omega_earth) * (tk_corr - t0) - (fundamentals.omega_earth * gpssatellites[id].toeSecond);
        //Rectas. ascending note in ECSF
        omegaECSF = gpssatellites[id].l0 + gpssatellites[id].Omegadot * (tk_corr - t0);

        // Rotation Matritzes %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        double [][]R3_lamk = {{cos(-lamk), sin(-lamk), 0},{-sin(-lamk), cos(-lamk), 0},{0, 0, 1}};
        double [][]R1_ik = {{1, 0, 0},{0, cos(-ik), sin(-ik)},{0, -sin(-ik), cos(-ik)}};
        double [][]R3_uk = {{cos(-uk), sin(-uk), 0},{-sin(-uk), cos(-uk), 0},{0, 0, 1}};
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        double[] rk_vec = {rk, 0, 0};

        //Coordinates of Satellite in TRS %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        RealMatrix R3_Lamk = MatrixUtils.createRealMatrix(R3_lamk);
        RealMatrix R1_Ik = MatrixUtils.createRealMatrix(R1_ik);
        RealMatrix R3_Uk = MatrixUtils.createRealMatrix(R3_uk);
        RealVector rk_Vec = MatrixUtils.createRealVector(rk_vec);

        RealMatrix temp1 = R3_Lamk.multiply(R1_Ik);
        RealMatrix temp2 = temp1.multiply(R3_Uk);

        RealVector satCoords = temp2.operate(rk_Vec);
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

        //corrected runtime
        tb = tRx - tk_corr;
        // eart rotaion
        rotation_earth = fundamentals.omega_earth * tb;
        //New Satelliteposition with corrected earth rotation during runtime
        double [][]R3earth = {{cos(rotation_earth), sin(rotation_earth), 0},{-sin(rotation_earth), cos(rotation_earth), 0},{0, 0, 1}};
        RealMatrix R3_earthrot = MatrixUtils.createRealMatrix(R3earth);

        RealVector satCoords_rotcorr = R3_earthrot.operate(satCoords);


        double []calcResult = CalculationResult(satCoords_rotcorr.toArray(), id, delta_t_approx);

        return calcResult;
    }

    private static double[] CalculationResult(double[] toArray, int id, double delta_t_approx) {

         double[] calcresult = new double[5];
         calcresult[0] = id;
         calcresult[1] = delta_t_approx;
         calcresult[2] = toArray[0];
        calcresult[3] = toArray[1];
        calcresult[4] = toArray[2];

        return calcresult;
    }


    private static double MeanAnomalie(double tk, gpsSat[] gpssatellites, int id, double Gm, double t) {


        double sqrtA = gpssatellites[id].SqrtA;
        double a = pow(sqrtA,2);
        double M0 = gpssatellites[id].M0;
        double DeltaN = gpssatellites[id].DeltaN;

        double Mk = M0 + ((sqrt(Gm) / sqrt(pow(a, 3))) + DeltaN) * t;


        return Mk;
    }

    private static double ExcentricAnomalie(double Mk, gpsSat[] gpssatellites, int id) {
        double Ek = Mk;
        double et = 0;

        while (abs(et-Ek) > 1e-10){
            et = Ek;
            Ek = Mk + gpssatellites[id].e * sin(Ek);
        }
        return Ek;
    }
}
