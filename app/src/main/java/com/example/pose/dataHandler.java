package com.example.pose;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.apache.commons.compress.compressors.z.ZCompressorInputStream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public abstract class dataHandler extends AppCompatActivity {

    /**
     *
     * Get Day of the Year and Year for File download from following ftp
     * ftp://cddis.gsfc.nasa.gov/gnss/data/daily/2019/brdc/brdcDDD0.YYn.Z    DDD->Day of Year, YY--> Year
     * Then unzip the file with ZCompressor Package from https://commons.apache.org/proper/commons-compress/
     * Write the actual ephemeries in ../latestEphemeries.txt
     * */


    public static void DownloadAndUnzipEphemeries(final String host, final String filename, final String user, final String pass, final String path, final String unzippath) throws  IOException{

        class  FTPDownload extends AsyncTask<String, Void, Void> {


            @Override
            protected Void doInBackground(String... urls) {

                FTPClient client = new FTPClient();


                try {
                    client.connect(host); //no user and pw needed but you have to login anyway with (login: "anonymous", Pw: "anything@")
                    Log.i(String.valueOf(this), "Connection established");
                    boolean successLogin = client.login(user, pass);
                    Log.i(String.valueOf(this), "Successfully logged in");
                    client.enterLocalPassiveMode();
                    client.setFileType(FTP.BINARY_FILE_TYPE);

                    String remoteFile = filename;
                    File newDownload = new File(path);

                    OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(newDownload));
                    InputStream inputStream = client.retrieveFileStream(remoteFile);

                    byte[] bytesArray = new byte[4096];
                    int bytesRead = -1;
                    while ((bytesRead = inputStream.read(bytesArray)) != -1) {
                        outputStream.write(bytesArray, 0, bytesRead);
                    }

                    boolean success = client.completePendingCommand();

                    outputStream.flush(); //neccessary to get the rest of the stream
                    outputStream.close();
                    inputStream.close();

                    if (success) {
                        Log.i(String.valueOf(this), "File downloaded successfully.");
                        try {
                            dataHandler.unzip(path, unzippath);
                            Log.i(String.valueOf(this), "Decompressed Ephemeries successfully");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        client.disconnect();
                        Log.i(String.valueOf(this), "Successfully logged out");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        }

        FTPDownload task = new FTPDownload();
        task.execute();
    }


    public static void unzip(String compressedFile, String unzippedfile) throws IOException {

        try {
            // Generate File Input and Outputstream
            FileInputStream fin = new FileInputStream(compressedFile);
            BufferedInputStream in = new BufferedInputStream(fin);
            FileOutputStream out = new FileOutputStream(unzippedfile);

            //Decompress File with ZCompressor Package
            ZCompressorInputStream zIn = new ZCompressorInputStream(in);
            final byte[] buffer = new byte[4096];
            int n = 0;
            while (-1 != (n = zIn.read(buffer))) {
                out.write(buffer, 0, n);
            }
            out.close();
            zIn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writePosition(List<double[]> coordlist) {
        String storagePath = "/storage/emulated/0/pose/";
        String dateString = dateToString();
        String filename = dateString + ".txt";

        File directory = new File(storagePath);

        if (!directory.exists()){
            directory.mkdir();
        }

        File infile = new File(storagePath + filename);
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(infile));
            for (double[] line : coordlist){
                bw.write(Arrays.toString(line) + "\n");
            }
            bw.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static String dateToString(){
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String dateString = dateFormat.format(date);
        return dateString;
    }
}

