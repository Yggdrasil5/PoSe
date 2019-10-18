package com.example.pose;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class Fundamentals {

    public double GM, omega_earth, c, rel, ell84a, ell84f, ell84b, earthRadius, ellBesslA, ellBesslF, ellBesslB;

    Fundamentals() {
        this.GM = 3986004.418e8; //Gravitationskonstante
        this.c = 299792458;
        this.rel = -2 * (sqrt(this.GM) / pow(this.c, 2)); //Relativistischer Effekt
        this.omega_earth = 7.292115 * 1e-5; //Winkelgeschwindigkeit der Erde [rad/s]
        this.ell84a = 6378137;
        this.ell84f = 1 / 298.257223563;
        this.ell84b = this.ell84a * (1 - this.ell84f);
        this.earthRadius = 6378000;
        this.ellBesslA = 6.377397155080000e+06;
        this.ellBesslF = 0.003342773181849;
        this.ellBesslB = 6.356078962900000e+06;
    }
}
