package com.example.pose;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class readfile {

    public static String[] readSatellites(File infile) throws IOException {

        String line = null;
        String[] satellites = new String[33];

        String PRN = null;
        int prn = 0;

        BufferedReader br = new BufferedReader(new FileReader(infile));
        int n = 8;
        int j = 1;

        //Skip first 8 Lines

        for (int i = 0; i < n; i++) {
            line = br.readLine();
        }

        // Header end
        /*
          Read Body:
          Always 8 lines. PRN is in first line at first place
          With StringBuilder append all lines and write it in the Array of Strings on the Place with respect to prn
         */
        line = br.readLine();
        try {
            while ((line != null)) {

                PRN = line.substring(0, 2);
                PRN = PRN.replace(" ", "");
                prn = Integer.parseInt(PRN);
                StringBuilder sb = new StringBuilder();
                while (j < 8) {
                    sb.append(line);
                    line = br.readLine();
                    j++;
                }
                j = 1;

                satellites[prn] = sb.toString();
                line = br.readLine();
            }
        } finally {
            br.close();
        }
        return satellites;
    }

    public static double[][] readIonosphere(File infile) throws IOException{

        String line = null;
        String ionoAlpha = null;
        String ionoBeta = null;

        double[][] iono = new double[2][4];

        BufferedReader br = new BufferedReader(new FileReader(infile));
        int n = 8;
        //Read Header: Always 8 Lines

        for (int i = 0; i < n; i++) {
            line = br.readLine();
            if (i==3){
                ionoAlpha = line; //IonoAlpha is in line 3 and Beta in line 4
                ionoAlpha = ionoAlpha.replace("D", "E");
                String[] ionoAlphaTrim = ionoAlpha.split(" ");
                List<String> values = new ArrayList<String>();
                for (String data: ionoAlphaTrim){
                    if(data.compareTo("") != 0){values.add(data);
                    }
                }
                iono[0] = new double[]{Double.parseDouble(String.valueOf(values.get(0))), Double.parseDouble(String.valueOf(values.get(1))), Double.parseDouble(String.valueOf(values.get(2))), Double.parseDouble(String.valueOf(values.get(3)))};

                line=br.readLine();
                ionoBeta = line;
                ionoBeta = ionoBeta.replace("D", "E");
                String[] ionoBetaTrim = ionoBeta.split(" ");
                values = new ArrayList<String>();
                for (String data: ionoBetaTrim){
                    if(data.compareTo("") != 0){values.add(data);
                    }
                }
                iono[1] = new double[]{Double.parseDouble(String.valueOf(values.get(0))), Double.parseDouble(String.valueOf(values.get(1))), Double.parseDouble(String.valueOf(values.get(2))), Double.parseDouble(String.valueOf(values.get(3)))};

                i=i+1;
            }
        }
        return iono;
    }
}
