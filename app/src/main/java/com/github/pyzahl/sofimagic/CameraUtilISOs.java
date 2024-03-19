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

    public static final float[] Apertures = new float[]{
            0f,
            1f,
            1.1f,
            1.3f,
            1.4f,
            1.6f,
            1.8f,
            2f,
            2.5f,
            2.8f,
            3.2f,
            3.5f,
            4f,
            4.5f,
            5f,
            5.6f,
            6.3f,
            7.1f,
            8f,
            9f,
            10f,
            11f,
            13f,
            14f,
            16f,
            18f,
            20f,
            22f,
            28f,
            32f
    };

    public static int getApertureValueIndex(float ap)
    {
        for (int i = 0; i < Apertures.length; ++i)
        {
            if (Apertures[i]-ap == 0.0)
                return i;
        }
        return -1;
    }

    // Camera Flags, Drive Mode, ...
    public static final String[] CFlags = new String[]{
            "S", // DriveMode Single
            "C", // DriveMode Continuous (Burst) High
            "M", // DriveMode Continuous (Burst) Middle
            "L", // DriveMode Continuous (Burst) Low
            "B"  // BriveMode Bracketing
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

    public static int getCFlagIndex(char cf)
    {
        for (int i = 0; i < CFlags.length; ++i)
            if (CFlags[i].toCharArray()[0] == cf) return i;
        return -1;
    }



    public static class getCFString implements ListEntry.LookupFunction<String, Integer> {
        public static final getCFString instance = new getCFString();
        private getCFString() {
        }
        public String call(Integer i) {
            if (i>=0 && i < CFlags.length) return CFlags[i];
            else return "S";
        }
    }

    public static class getBurstDurationstring implements ListEntry.LookupFunction<String, Integer> {
        public static final getBurstDurationstring instance = new getBurstDurationstring();
        private getBurstDurationstring() {
        }
        public String call(Integer i) {
            return Integer.toString(i);
        }
    }


}


