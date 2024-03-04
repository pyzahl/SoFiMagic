package com.github.pyzahl.sofimagic;

public class CameraUtilISOs {
    public static final int[] ISOs = new int[]{
            0, // USED AS END LIST MARKER
            50,
            64,
            80,
            100,
            125,
            160,
            200,
            250,
            320,
            400,
            500,
            640,
            800,
            1000,
            1250,
            1600,
            2000,
            2500,
            3200,
            4000,
            5000,
            6400,
            8000,
            10000,
            12800,
            16000,
            20000,
            25600,
            32000,
            40000,
            51200,
            64000,
            80000,
            102400
    };

    public static final double[] Apertures = new double[]{
            0,
            1,
            2,
            2.2,
            2.5,
            2.8,
            3.2,
            4,
            4.5,
            3.2,
            5,
            5.6,
            6.3,
            7.1,
            8,
            9,
            10,
            11,
            13,
            14,
            16,
            18,
            20,
            22,
            28,
            32
    };

    // Camera Flags, Drive Mode, ...
    public static final String[] CFlags = new String[]{
            "S",  // DriveMode Single
            "C",  // DriveMode Continuous (Burst)
            "BKT" // BriveMode Bracketing
    };

    public static int getISOIndex(int iso)
    {
        for (int i = 0; i < ISOs.length; ++i)
            if (ISOs[i] == iso) return i;
        return -1;
    }

    public static int getISO(int pos){
        if (pos>=0 && pos < ISOs.length) return ISOs[pos];
        else return -1;
    }
    public static String getISOStr(int pos){
        if (pos>=0 && pos < ISOs.length) return Integer.toString(ISOs[pos]);
        else return "AUTO";
    }

    public static class getISOString implements ListEntry.LookupFunction<String, Integer> {
        public static final getISOString instance = new getISOString();
        private getISOString() {
        }
        public String call(Integer i) {
            return getISOStr(i);
        }
    }

    public static int getFIndex(double f)
    {
        for (int i = 0; i < Apertures.length; ++i)
            if (Math.abs(Apertures[i] - f) < 0.1) return i;
        return -1;
    }


    public static String getFStr(int pos){
        if (pos>=0 && pos < Apertures.length) return Double.toString(Apertures[pos]);
        else return "AUTO";
    }


    public static class getFString implements ListEntry.LookupFunction<String, Integer> {
        public static final getFString instance = new getFString();
        private getFString() {
        }
        public String call(Integer i) {
            return getFStr(i);
        }
    }

    public static int getCFlagIndex(char[] cf)
    {
        for (int i = 0; i < CFlags.length; ++i)
            if (CFlags[i].toCharArray()[0] == cf[0]) return i;
        return -1;
    }



    public static class getCFString implements ListEntry.LookupFunction<String, Integer> {
        public static final getCFString instance = new getCFString();
        private getCFString() {
        }
        public String call(Integer i) {
            return CFlags[i];
        }
    }

    public static class getBurstCountString implements ListEntry.LookupFunction<String, Integer> {
        public static final getBurstCountString instance = new getBurstCountString();
        private getBurstCountString() {
        }
        public String call(Integer i) {
            return Integer.toString(i);
        }
    }


}


