package com.example.pose;

public class gpsSat {

    //Initialize variables

    public int prn, tocYear, tocMonth, tocDay, tocHour, tocMinute;
    public double tocSecond, SvClockBias, SvClockDrift, SvClockDriftRate, AgeOfEphemeries, Crs, DeltaN, M0, Cuc, e, Cus, SqrtA, toeSecond, Cic, l0, Cis ;
    public double i0, CrC, omega0, Omegadot, idot, codesOnL2, toeWeek, L2PFlag, TrTimeOfMeas;



    gpsSat(String ephemeriesString){ //Konstruktor

        this.prn = Integer.parseInt(ephemeriesString.substring(0,2).replace(" ",""));
        this.tocYear = Integer.parseInt(ephemeriesString.substring(2,5).replace(" ",""));
        this.tocMonth = Integer.parseInt(ephemeriesString.substring(5,8).replace(" ",""));
        this.tocDay = Integer.parseInt(ephemeriesString.substring(8,11).replace(" ",""));
        this.tocHour = Integer.parseInt(ephemeriesString.substring(11,14).replace(" ",""));
        this.tocMinute = Integer.parseInt(ephemeriesString.substring(14,17).replace(" ",""));
        this.tocSecond = Double.parseDouble(ephemeriesString.substring(17,22).replace(" ",""));
        this.SvClockBias = Double.parseDouble(replaceD(ephemeriesString.substring(22,41)));
        this.SvClockDrift = Double.parseDouble(replaceD(ephemeriesString.substring(41,60)));
        this.SvClockDriftRate = Double.parseDouble(replaceD(ephemeriesString.substring(60,79)));
        //RawData second line
        this.AgeOfEphemeries = Double.parseDouble(replaceD(ephemeriesString.substring(79, 101)));
        this.Crs = Double.parseDouble(replaceD(ephemeriesString.substring(101, 120)));
        this.DeltaN = Double.parseDouble(replaceD(ephemeriesString.substring(120, 139)));
        this.M0 = Double.parseDouble(replaceD(ephemeriesString.substring(139, 158)));
        // RawData third Line
        this.Cuc = Double.parseDouble(replaceD(ephemeriesString.substring(158, 180)));
        this.e = Double.parseDouble(replaceD(ephemeriesString.substring(180, 199)));
        this.Cus = Double.parseDouble(replaceD(ephemeriesString.substring(199, 218)));
        this.SqrtA = Double.parseDouble(replaceD(ephemeriesString.substring(218, 237)));
        //RawData fourth Line
        this.toeSecond = Double.parseDouble(replaceD(ephemeriesString.substring(237, 259)));
        this.Cic = Double.parseDouble(replaceD(ephemeriesString.substring(259, 278)));
        this.l0 = Double.parseDouble(replaceD(ephemeriesString.substring(278, 297)));
        this.Cis = Double.parseDouble(replaceD(ephemeriesString.substring(297, 316)));
        //RawData fifth Line
        this.i0 = Double.parseDouble(replaceD(ephemeriesString.substring(316, 338)));
        this.CrC = Double.parseDouble(replaceD(ephemeriesString.substring(338, 357)));
        this.omega0 = Double.parseDouble(replaceD(ephemeriesString.substring(357, 376)));
        this.Omegadot = Double.parseDouble(replaceD(ephemeriesString.substring(376, 395)));
        //RawData sith Line
        this.idot = Double.parseDouble(replaceD(ephemeriesString.substring(395, 417)));
        this.codesOnL2 = Double.parseDouble(replaceD(ephemeriesString.substring(417, 436)));
        this.toeWeek = Double.parseDouble(replaceD(ephemeriesString.substring(436, 455)));
        this.L2PFlag = Double.parseDouble(replaceD(ephemeriesString.substring(455, 474)));
        //RawData seventh Line
        this.TrTimeOfMeas = Double.parseDouble(replaceD(ephemeriesString.substring(474, 496)));
        // Rest are Spare Fields. those are not implemented.





/*
    NavData.i0(zeile,satnumber) = str2num(tline(1:22));
    NavData.Crc(zeile,satnumber) = str2num(tline(23:41));
    NavData.omega0(zeile,satnumber) = str2num(tline(42:60));
    NavData.Omegadot(zeile,satnumber) = str2num(tline(61:79));
*/
        int stop =1;

    }


    public String replaceD (String instring){

        String outstring = instring.replace("D", "E");
        return outstring;

    }


}
