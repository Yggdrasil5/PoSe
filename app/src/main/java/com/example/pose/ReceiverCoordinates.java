package com.example.pose;

import android.os.Parcel;
import android.os.Parcelable;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.tan;

public class ReceiverCoordinates implements Parcelable {

    Fundamentals fundamentals = new Fundamentals();

    private double[] coords, coordsMgi, coordsMean, coordsEll;
    List<double[]> coordlist = new ArrayList<double[]>();

    ReceiverCoordinates(){ //Constructor
        double[] coords = new double[]{0,0,0};
        double[] coordsEll = new double[]{0,0,0};
        double[] coordsMean = new double[]{0,0,0};
        double[] coordsMgi = new double[]{0,0,0};

        this.coords = coords;
        this.coordsEll = coordsEll;
        this.coordsMean = coordsMean;
        this.coordsMgi = coordsMgi; //Nur für Test, da Bezugsmeridian gebraucht wird

    }

    void setCoords(double[] coords){

        this.coords = coords;
        addToList();
        calcCoordsmean();
        this.coordsEll = transformToEll(fundamentals.ell84a, fundamentals.ell84f, this.coordsMean);
        transformToMgi();
    }

    private void transformToMgi() {
        double[] coordsCartBessel = transformWgsToBessl();
        double[] coordsEllBessl = transformToEll(fundamentals.ellBesslA, fundamentals.ellBesslF, coordsCartBessel);
        double meridian = 34;
        double[] coordsMgi = projGkMgi(fundamentals.ellBesslA, fundamentals.ellBesslB, fundamentals.ellBesslF, meridian, coordsEllBessl);
        this.coordsMgi = coordsMgi;

    }

    private double[] projGkMgi(double a, double b, double f, double meridian, double[] coordsEllBessl) {
        double n = (a-b)/(a+b);
        double alpha = ((a+b)/2) * (1 + 0.25*power(n,2) + 0.015625*power(n,4));
        double beta = (-3.0/2.0)*n + (9.0/16.0)*pow(n,3) - (3.0/32.0)*pow(n,5);
        double gamma = (15.0/16.0)*pow(n,2) - (15.0/32.0)*pow(n,4);
        double delta = (-35.0/48.0)*pow(n,3) + (105.0/256.0)*pow(n,5);

        double phi = coordsEllBessl[0]*PI/180;
        double lam=coordsEllBessl[1]*PI/180;
        //Umrechnen wegen unterschiedlicher Bezugssysteme Ferro liegt 17°40' westl von Greenwich
        meridian = meridian*PI/180;
        double ferro = (17.0 + 40.0/60.0)*PI/180;
        double lam0 = meridian - ferro;

        double Bphi = alpha*(phi+beta*sin(2*phi)+gamma*sin(4*phi)+delta*sin(6*phi));
        //Größen und Hilfsgrößen
        double es2 = (pow(a,2) - pow(b,2)) / pow(b,2);
        double eta2 = es2 * pow(cos(phi),2);
        double N = pow(a,2) / (b*sqrt(1+eta2));
        double t = tan(phi);
        double l = lam-lam0;
        //Berechnung der Koordinaten laut Scriptum Bezugssysteme
        double x = Bphi+(t/2)*N*pow(cos(phi),2)*pow(l,2);
        x = x + (t/24)*N*pow(cos(phi),4)*(5-pow(t,2) + 9*eta2 + 4*pow(eta2,2))*pow(l,4);
        x = x + (t/720)*N*pow(cos(phi),6)*(61 - 58*pow(t,2) + pow(t,4) + 270*eta2 - 330*pow(t,2)*eta2)*pow(l,6);
        x = x + (t/40320)*N*pow(cos(phi),8)*(1385-3111*pow(t,2) + 543*pow(t,4) - pow(t,6))*pow(l,8);

        double y = N*cos(phi)*l + (1.0/6.0)*N*pow(cos(phi),3)*(1 - pow(t,2) + eta2)*pow(l,3);
        y = y + (1.0/120.0)*N*pow(cos(phi),5)*(5 - 18*pow(t,2) + pow(t,4) + 14*eta2 - 58*pow(t,2)*eta2)*pow(l,5);
        y = y + (1.0/5040.0)*N*pow(cos(phi),7)*(61 - 479*pow(t,2) + 179*pow(t,4) - pow(t,6))*pow(l,7);

        coordsMgi[1] = x;
        coordsMgi[0] = y;
        return  coordsMgi;
    }

    private double power(double n, int x) {
        double out = 1;
        for (int i = 0; i < x; i++){
            out = out * n;
        }
        return out;
    }

    private double[] transformWgsToBessl() {
        double[] coordsWgs = this.getCoordsMean();
        double dx = -577.326;
        double dy = -90.129;
        double dz = -463.919;
        double dm = -2.4232*1e-6;

        double ax = (5.137/3600)*PI/180;
        double ay = (1.474/3600)*PI/180;
        double az = (5.297/3600)*PI/180;

        double[][] r = new double[3][3];
        r[0] = new double[]{1, az, -ay};
        r[1] = new double[]{-az, 1, ax};
        r[2] = new double[]{ay, -ax, 1};

        RealVector coordsWgsv = MatrixUtils.createRealVector(coordsWgs);
        RealVector c = MatrixUtils.createRealVector(new double[]{dx, dy, dz});
        RealMatrix R = MatrixUtils.createRealMatrix(r);

        RealVector temp = R.operate(coordsWgsv);
        RealVector temp2 = temp.mapMultiply((1+dm));

        RealVector coordsCartv = c.add(temp2);
        double[] coordsBessl = coordsCartv.toArray();

        return coordsBessl;
    }


    private void calcCoordsmean() {
        double[] tempSum = new double[]{0,0,0};
        for (Integer i =0; i< coordlist.size(); i++){
            tempSum[0] += coordlist.get(i)[0];
            tempSum[1] += coordlist.get(i)[1];
            tempSum[2] += coordlist.get(i)[2];
        }
        tempSum[0] = tempSum[0]/coordlist.size();
        tempSum[1] = tempSum[1]/coordlist.size();
        tempSum[2] = tempSum[2]/coordlist.size();
        coordsMean = tempSum;
    }

    private void addToList() {
        if(coordlist.size() == 0) {
            this.coordlist.add(0, this.coords);
        }else{
            this.coordlist.add(this.coords);
        }

    }

    public double[] transformToEll(double a, double f, double[] coords) {

        double x = coords[0];
        double y = coords[1];
        double z = coords[2];
        double h = 0;

        double eq = 2*f - pow(f,2);
        double e = sqrt(eq);

        double lam = atan2(y,x);

        //Calc Phi iterrative
        double p = sqrt(pow(x,2) + pow(y,2));
        double phi  = atan2((z/p), (1-pow(e,2)));
        double phi_temp = 0;

        while (abs(phi_temp - phi) >= 1e-7){
            double diff = abs(phi_temp - phi);
            phi_temp = phi;

            double N = a / sqrt(1 - pow(e,2) * pow(sin(phi_temp),2));
            h = p / cos(phi_temp) - N;
            phi = atan2((z/p), (1-(N/(N+h))*pow(e,2)));
        }

        this.coordsEll[0] = phi/PI*180;
        this.coordsEll[1] = lam/PI*180;
        this.coordsEll[2] = h;

        return coordsEll;
    }

    double[] getCoords(){
        return Arrays.copyOf(this.coords, this.coords.length);
    }

    double [] getCoordsMean(){
        return this.coordsMean;
    }

    double[] getCoordsEll(){
        return this.coordsEll;
    }

    double[] getCoordsMgi() {return this.coordsMgi;}



    //Parcelable implementation---------
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(coordlist);
        dest.writeDoubleArray(coords);
        dest.writeDoubleArray(coordsMgi);
        dest.writeDoubleArray(coordsEll);
        dest.writeDoubleArray(coordsMean);
    }
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator(){
        public ReceiverCoordinates createFromParcel(Parcel in) {
            return new ReceiverCoordinates(in);
        }
        public ReceiverCoordinates[] newArray(int size) {
            return new ReceiverCoordinates[size];
        }
    };

    public ReceiverCoordinates(Parcel in){
       // double[] coords, coordsMgi, coordsMean, coordsEll;

        coordlist = new ArrayList<double[]>();
        in.readList(coordlist, ReceiverCoordinates.class.getClassLoader());
        coords = in.createDoubleArray();
        coordsMgi = in.createDoubleArray();
        coordsEll = in.createDoubleArray();
        coordsMean = in.createDoubleArray();
    }

    //End Parcalable implementation ----------------------------

}


