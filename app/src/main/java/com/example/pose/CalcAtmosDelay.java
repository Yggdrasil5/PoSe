package com.example.pose;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.Calendar;

import static java.lang.Double.NaN;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

public class CalcAtmosDelay {

    public static double[] calculation(double[] satCoords, ReceiverCoordinates receiverCoordinates, double[][] iono, double tk, int j) {
        Fundamentals fundamentals = new Fundamentals();


        double[] deltaAtmos = new double[]{0,0};
        double[] alpha = iono[0];
        double[] beta = iono[1];

        if (receiverCoordinates.getCoordsMean()[2] != 0) { //For Testcase dissabled
            double[] elAz = calcElevationAndAzimuth(receiverCoordinates, satCoords);
            double deltaIono = calcIonoKlobuchar(elAz, receiverCoordinates, tk, fundamentals, alpha, beta);
            double deltaTropo = calcTropo(elAz[0], receiverCoordinates);

            deltaAtmos[0] = deltaIono;
            deltaAtmos[1] = deltaTropo;
            return deltaAtmos;
        }else {
            return deltaAtmos;
        }
    }

    private static double calcTropo(double e, ReceiverCoordinates receiverCoordinates) {
        double deltaTropo;
        double[][] xi = new double[5][5];
        double[][] dxi = new double[5][5];
        double[] v1 = new double[]{};
        double[] v2 = new double[]{};
        double[] tropoparameter = new double[]{};

        double k1 = 77.604;
        double k2 = 382000;
        double Rd = 287.054;
        double gm = 9.784;
        double g = 9.80665;

        xi[0] = new double[]{1013.25, 299.65, 26.31, 6.3 * 1e-3, 2.77};
        xi[1] = new double[]{1017.25, 294.15, 21.79, 6.05 * 1e-3, 3.15};
        xi[2] = new double[]{1015.75, 283.15, 11.66, 5.58 * 1e-3, 2.57};
        xi[3] = new double[]{1011.75, 272.15, 6.78, 5.39 * 1e-3, 1.81};
        xi[4] = new double[]{1013.00, 263.65, 4.11, 4.53 * 1e-3, 1.55};

        dxi[0] = new double[]{0, 0, 0, 0, 0};
        dxi[1] = new double[]{-3.75, 7, 8.85, 0.25 * 1e-3, 0.33};
        dxi[2] = new double[]{-2.25, 11, 7.24, 0.32 * 1e-3, 0.46};
        dxi[3] = new double[]{-1.75, 15, 5.36, 0.81 * 1e-3, 0.74};
        dxi[4] = new double[]{-0.5, 14.5, 3.39, 0.62 * 1e-3, 0.3};

        double[] x = new double[]{15, 30, 45, 60, 75};

        RealMatrix rxi = MatrixUtils.createRealMatrix(xi);
        RealMatrix rdxi = MatrixUtils.createRealMatrix(dxi);

        LinearInterpolator li = new LinearInterpolator();
        double[] xivec = new double[]{0, 0, 0, 0, 0};
        double[] dxivec = new double[]{0, 0, 0, 0, 0};

        if ((e * 180/PI) < 15) {
            xivec = rxi.getRow(0);
            dxivec = rdxi.getRow(0);
        } else if ((e * 180/PI) > 75) {
            xivec = rxi.getRow(4);
            dxivec = rdxi.getRow(4);
        } else {
            double el = e * 180/PI;
            for (int i = 0; i < xi.length; i++) {
                v1 = rxi.getColumn(i);
                v2 = rdxi.getColumn(i);

                PolynomialSplineFunction f = li.interpolate(x, v1);
                xivec[i] = f.value(el);

                PolynomialSplineFunction p = li.interpolate(x, v2);
                dxivec[i] = p.value(el);
            }
        }
        if ((e * 180/PI < 5)) {
            deltaTropo = 0;
        } else {
            //obliquity Factor M
            double m = 1.001 / (sqrt(0.002001 + pow(sin(e), 2)));
            tropoparameter = calcTropoParameter(xivec, dxivec);


            double tr0d = (1e-6 * k1 * Rd * xivec[0]) / gm;

            double term1 = (1e-6 * k2 * Rd) / ((xivec[4] + 1) * gm - xivec[3] * Rd);
            double term2 = xivec[2] / xivec[1];
            double tr0w = term1 * term2;

            //Calculate Trzd
            double factor1 = g / (Rd * tropoparameter[3]);
            double trd = (pow((1 - (tropoparameter[3] * receiverCoordinates.getCoordsEll()[2]) / tropoparameter[1]), factor1)) * tr0d;
            // Calculate trw
            double factor2 = (((tropoparameter[4] + 1) * g) / (Rd * tropoparameter[3])) - 1;
            double trw = (pow((1 - (tropoparameter[3] * receiverCoordinates.getCoordsEll()[2]) / tropoparameter[1]), factor2)) * tr0w;

            deltaTropo = (trd + trw) * m;
        }
        return deltaTropo;

    }

    private static double[] calcTropoParameter(double[] xivec, double[] dxivec) {
        double dmin = 28;//for northern Hemisphere
        Calendar calendar = Calendar.getInstance();
        int doy = calendar.get(Calendar.DAY_OF_YEAR);
        double[] tropoParameter = new double[]{0,0,0,0,0};

        for (int i = 0; i< xivec.length; i++){
            tropoParameter[i] = xivec[i] - dxivec[i]*cos((2*PI*(doy-dmin))/365.25);
        }
        return tropoParameter;
    }

    private static double calcIonoKlobuchar(double[] elAz, ReceiverCoordinates receiverCoordinates, double tk, Fundamentals fundamentals, double[] alpha, double[] beta) {
        //Calculation from ESA-Book
        double deltaIono;
        double phi = receiverCoordinates.getCoordsEll()[0]*PI/180;
        double lam = receiverCoordinates.getCoordsEll()[1]*PI/180;
        double Re = fundamentals.earthRadius;
        double h = 350000; //Height for GPS and Galileo same
        //Geomagnetic Pole--------------------------------------------------
        double PHI_p = 78.3*PI/180;
        double LAM_p = 291.0*PI/180;
        //-----------------------------------------------------------------
        //Earth centered Angle
        double psi = PI/2 - elAz[0] - asin(Re/(Re + h)*cos(elAz[0]));
        //Latitude of the IPP
        double PHI_i = asin(sin(phi) * cos(psi) + cos(phi) * sin(psi) * cos(elAz[1]));
        //Longitude of IPP
        double lam_i = lam + (psi*sin(elAz[1]))/(cos(PHI_i));
        //geomagnetic latitude of IPP
        double PHI_m = asin(sin(PHI_i)*sin(PHI_p)+cos(PHI_i)*cos(PHI_p)*cos(lam_i-LAM_p));
        //Local Time at the IPP
        double t = 43200*lam_i/PI + tk;
        t =  (t % 86400); //Zeitraum muss innerhalb 24std liegen
        //
        //Amplitude of the Atmos Delay
        double AI = 0;
        for (int j = 0; j < 4; j++ ){
            AI += alpha[j]  * pow((PHI_m/PI),j);
        }
        if (AI < 0){
            AI = 0;
        }

        //Period of Iono Delay
        double Per = 0;
        for (int j = 0; j < 4; j++ ){
            Per += beta[j]  * pow((PHI_m/PI),j);
        }
        if (Per < 72000){
            Per = 72000;
        }
        // Phase of iono delay
        double xi = (2*PI * (tk - 50400) / Per);
        //Slant factor
        double F = pow(1- pow(((Re/(Re+h))*cos(elAz[0])),2),-0.5);

        if (abs(xi) < PI/2) {
            deltaIono = (5e-9 + AI * cos(xi)) * F;
        }else{
            deltaIono = 5e-9 * F;
        }
        //DeltaIono as distance
        deltaIono = deltaIono*fundamentals.c;

        return deltaIono;
    }

    private static double[] calcElevationAndAzimuth(ReceiverCoordinates receiverCoordinates, double[] satCoords) {
        //Calculations used from ESA-Book
        double[] elAz = new double[]{0,0};
        double[] recCoords = receiverCoordinates.getCoordsEll();


        double phi = recCoords[0]*PI/180;
        double lam = recCoords[1]*PI/180;

        //Diffvec in ECEF
        RealVector satECEF = MatrixUtils.createRealVector(new double[]{satCoords[1], satCoords[2], satCoords[3]});
        RealVector recECEF = MatrixUtils.createRealVector(receiverCoordinates.getCoordsMean());
        RealVector diffVec = satECEF.subtract(recECEF);

        double norm = diffVec.getNorm();
        RealVector rhoHat = diffVec.mapDivide(norm);

        double [] e = new double[]{-sin(lam), cos(lam), 0};
        double [] n = new double[]{-cos(lam)*sin(phi), -sin(lam)*sin(phi), cos(phi)};
        double [] u = new double[]{cos(lam)*cos(phi), sin(lam)*cos(phi), sin(phi)};

        RealVector re = MatrixUtils.createRealVector(e);
        RealVector rn = MatrixUtils.createRealVector(n);
        RealVector ru = MatrixUtils.createRealVector(u);

        double E = asin(rhoHat.dotProduct(ru));
        double A = atan2(rhoHat.dotProduct(rn), rhoHat.dotProduct(re));

        A = (A % (2*PI)) * 180/PI;

        elAz[0] = E;
        elAz[1] = A;

        return elAz;
    }
}
