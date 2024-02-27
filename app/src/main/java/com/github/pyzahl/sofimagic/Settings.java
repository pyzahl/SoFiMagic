package com.github.pyzahl.sofimagic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.github.pyzahl.sofimagic.Logger.log;

/**
 * Created by jonas on 2/18/17.
 */

class Settings {
    private static final String EXTRA_TC1 = "com.github.pyzahl.sofimagic.TC1";
    private static final String EXTRA_TC2 = "com.github.pyzahl.sofimagic.TC2";
    private static final String EXTRA_TC3 = "com.github.pyzahl.sofimagic.TC3";
    private static final String EXTRA_TC4 = "com.github.pyzahl.sofimagic.TC4";
    private static final String EXTRA_ISOPARTIAL = "com.github.pyzahl.sofimagic.ISOPARTIAL";
    private static final String EXTRA_ISODIAMOND = "com.github.pyzahl.sofimagic.ISODIAMOND";
    private static final String EXTRA_ISOTOTAL = "com.github.pyzahl.sofimagic.ISOTOTAL";
    private static final String EXTRA_INTERVAL = "com.github.pyzahl.sofimagic.INTERVAL";
    private static final String EXTRA_SHOTCOUNT = "com.github.pyzahl.sofimagic.SHOTCOUNT";
    private static final String EXTRA_DELAY = "com.github.pyzahl.sofimagic.DELAY";
    private static final String EXTRA_DISPLAYOFF = "com.github.pyzahl.sofimagic.DISPLAYOFF";
    private static final String EXTRA_SILENTSHUTTER = "com.github.pyzahl.sofimagic.SILENTSHUTTER";
    private static final String EXTRA_AEL = "com.github.pyzahl.sofimagic.AEL";
    private static final String EXTRA_BRS = "com.github.pyzahl.sofimagic.BRS";
    private static final String EXTRA_MF = "com.github.pyzahl.sofimagic.MF";

    static int tc1=12*3600;  // Time C1 in Seconds from 00h00m00s of day
    static int tc2=tc1+3600; // Time C2 in Seconds from 00h00m00s of day
    static int tc3=tc2+4*60; // Time C3 in Seconds from 00h00m00s of day
    static int tc4=tc3+3600; // Time C4 in Seconds from 00h00m00s of day

    static public void set_contact_times(String stc1, String stc2, String stc3, String stc4){
        tc1 = parseHMStoSec(stc1);
        tc2 = parseHMStoSec(stc2);
        tc3 = parseHMStoSec(stc3);
        tc4 = parseHMStoSec(stc4);
    }

    static public int parseHMStoSec(String tc) {
        String[] hms = tc.split(":");
        int h = Integer.parseInt(hms[0]);
        int m = Integer.parseInt(hms[1]);
        int s = Integer.parseInt(hms[2]);
        return h*3600+m*60+s;
    }

    static final int MAX_EXPOSURE_PARAMS = 16;

    class shoot_program {
        public String name; // = new String("NONE");
        shoot_program(String name, int ref_ci, int ref_cf, int ti, int tf, int number_shots, String CamFlags) {
            this.name = name;
            this.ref_contact_start = ref_ci;
            this.ref_contact_end = ref_cf;
            this.start_time = ti;
            this.end_time = tf;
            this.number_shots = number_shots;
            this.CameraFlags = CamFlags;
        }

        public boolean skip = false;
        public int ref_contact_start = 0; // 1 = TC1, 2=TC2, 0=MAX=(TC2+TC3)/2. 3=TC3. 4=TC4
        public int ref_contact_end = 0; // 1 = TC1, 2=TC2, 0=MAX=(TC2+TC3)/2. 3=TC3. 4=TC4
        public int start_time = 0; // Time in Seconds relative to ref_contact
        public int end_time = 0; // Time in Seconds relative tp ref_contact
        public int number_shots = 0; // numbershots/burst to distribute
        public String CameraFlags;
        public int[] Bursts = new int[MAX_EXPOSURE_PARAMS];  //          = {-1,             0,       0 }; // 0=END, -1, regular (no burst)
        public int[] Fs = new int[MAX_EXPOSURE_PARAMS];    //          = {0,          0,       0 }; // 0=do not manage/change
        public int[] ISOs = new int[MAX_EXPOSURE_PARAMS];    //          = {400,          400,       0 }; // 0=END
        public int[][] ShutterSpeeds = new int[MAX_EXPOSURE_PARAMS][2]; // = {{1,1000},    {1, 2000}, {0,0}};

        public int get_start_time() {
                return get_TC(ref_contact_start) + start_time;
        }
        public int get_end_time() {
            return get_TC(ref_contact_end) + end_time;
        }

    }

    static public int get_TC(int i) {
        switch (i) {
            case 0: return (tc2+tc3)/2;
            case 1: return tc1;
            case 2: return tc2;
            case 3: return tc3;
            case 4: return tc4;
        }
        return -1;
    }

    static public shoot_program[] magic_program; // Partial1, Contact2, TotalA, TotalMax, TotalB, Contact3, Partial2, END


    // obsoleted
    int isoPartial;
    int isoDiamond;
    int isoTotal;
//--

    double interval;
    int delay;
    int rawInterval, rawDelay;
    int shotCount, rawShotCount;
    boolean displayOff;
    boolean silentShutter;
    boolean ael;
    int fps;    // index
    boolean brs;
    boolean mf;

    Settings() {

        Logger.log("Settings Init Magic Program");
        magic_program = new shoot_program[8]; // Partial1, Contact2, TotalA, TotalMax, TotalB, Contact3, Partial2, END

        // Default Contact Times
        tc1 = 3600*12;
        tc2 = tc1+60*60+10;
        tc3 = tc2+4*60;
        tc4 = tc3+60*60+10;

        // initialze programn defaults
        int phase=0;    // 0,... 7 for Partial1, Contact2, TotalA, TotalMax, TotalB, Contact3, Partial2

        // C1..C2 Partial1 Shooting
        magic_program[phase] = new shoot_program("Partial1", 1,2,+30, -30, 32, "S0,B0,C0");
        int[] P1PartialBursts          = {-1,             0,       0 }; // 0=END, -1, regular (no burst)
        int[] P1PartialISOs            = {400,          400,       0 }; // 0=END
        int[][] P1PartialShutterSpeeds = {{1,1000},    {1, 2000}, {0,0}};
        for (int i=0; P1PartialISOs[i]>0; i++) {
            magic_program[phase].Bursts[i]     = P1PartialBursts[i];
            magic_program[phase].ISOs[i]       = P1PartialISOs[i];
            magic_program[phase].Fs[i]         = 0;
            magic_program[phase].ShutterSpeeds[i][0] = P1PartialShutterSpeeds[i][0];
            magic_program[phase].ShutterSpeeds[i][1] = P1PartialShutterSpeeds[i][1];
        }
        Logger.log(magic_program[phase].name);
        phase++;

        // C2 Shooting Parameters
        // Diamond Ring, Baily's Beats, ...
        magic_program[phase] = new shoot_program("Contact2", 2,2,-5, +5, -1, "S0,B0,C0");
        int[] C2ShootingBursts          = {3,             0}; // 0=END, -1, regular (no burst)
        int[] C2ShootingISOs            = {100,           0}; // 0=END
        int[][] C2ShootingShutterSpeeds = {{1,4000},    {0,0}};
        for (int i=0; C2ShootingISOs[i]>0; i++) {
            magic_program[phase].Bursts[i]     = C2ShootingBursts[i];
            magic_program[phase].ISOs[i]       = C2ShootingISOs[i];
            magic_program[phase].Fs[i]         = 0;
            magic_program[phase].ShutterSpeeds[i][0] = C2ShootingShutterSpeeds[i][0];
            magic_program[phase].ShutterSpeeds[i][1] = C2ShootingShutterSpeeds[i][1];
        }
        Logger.log(magic_program[phase].name);
        phase++;

        // Totality C2..C3 parameters in three sections.
        // Totality C2...Max
        magic_program[phase] = new shoot_program("TotalityA", 2,0, +5, -30, -1, "S0,B0,C0");
        int[] TotalityABursts          = {-1,          0,        0,        0,        0,        0,        0,        0,        0,        0,        0,         0 }; // 0=END, -1, regular (no burst)
        int[] TotalityAISOs            = { 50,       100,      400,      800,      800,      800,      800,      800,      800,      800,      800,         0 }; // 0=END
        int[][] TotalityAShutterSpeeds = { {1,4000}, {1,2000}, {1,1000}, {1,1000}, {1, 500}, {1, 250}, {1, 100}, {1,  50}, {1,  20}, {1,   4}, {1,  1},    {0,0} };
        for (int i=0; TotalityAISOs[i]>0; i++) {
            magic_program[phase].Bursts[i]     = TotalityABursts[i];
            magic_program[phase].ISOs[i]       = TotalityAISOs[i];
            magic_program[phase].Fs[i]         = 0;
            magic_program[phase].ShutterSpeeds[i][0] = TotalityAShutterSpeeds[i][0];
            magic_program[phase].ShutterSpeeds[i][1] = TotalityAShutterSpeeds[i][1];
        }
        Logger.log(magic_program[phase].name);
        phase++;

        // Totality Max..C3
        magic_program[phase] = new shoot_program("MaxTotality", 0,0,-30, +30, -1, "S0,B0,C0");
        int[] MaxTotalityBursts          = {-1,          0,        0,        0,        0,        0,        0,        0,        0,        0,        0,        0,          0 }; // 0=END, -1, regular (no burst)
        int[] MaxTotalityISOs            = { 100,      400,      800,      800,      800,      800,      800,      800,      800,      800,      800,      800,          0 }; // 0=END
        int[][] MaxTotalityShutterSpeeds = { {1,1000}, {1,1000}, {1,1000}, {1, 500}, {1,1000}, {1, 500}, {1,1000}, {1, 500}, {1, 100}, {1,  20}, {1,   1}, {2, 1},      {0,0} };
        for (int i=0; MaxTotalityISOs[i]>0; i++) {
            magic_program[phase].Bursts[i]     = MaxTotalityBursts[i];
            magic_program[phase].ISOs[i]       = MaxTotalityISOs[i];
            magic_program[phase].Fs[i]         = 0;
            magic_program[phase].ShutterSpeeds[i][0] = MaxTotalityShutterSpeeds[i][0];
            magic_program[phase].ShutterSpeeds[i][1] = MaxTotalityShutterSpeeds[i][1];
        }
        Logger.log(magic_program[phase].name);
        phase++;

        // Totality Max..C3
        magic_program[phase] = new shoot_program("TotalityB", 0,3,+30, -5, -1, "S0,B0,C0");
        int[] TotalityBBursts          = {-1,          0,        0,        0,        0,        0,        0,        0,        0,        0,        0,         0 }; // 0=END, -1, regular (no burst)
        int[] TotalityBISOs            = { 50,       100,      400,      800,      800,      800,      800,      800,      800,      800,      800,         0 }; // 0=END
        int[][] TotalityBShutterSpeeds = { {1,4000}, {1,2000}, {1,1000}, {1,1000}, {1, 500}, {1, 250}, {1, 100}, {1,  50}, {1,  20}, {1,   4}, {1,  1},    {0,0} };
        for (int i=0; TotalityBISOs[i]>0; i++) {
            magic_program[phase].Bursts[i]     = TotalityBBursts[i];
            magic_program[phase].ISOs[i]       = TotalityBISOs[i];
            magic_program[phase].Fs[i]         = 0;
            magic_program[phase].ShutterSpeeds[i][0] = TotalityBShutterSpeeds[i][0];
            magic_program[phase].ShutterSpeeds[i][1] = TotalityBShutterSpeeds[i][1];
        }
        Logger.log(magic_program[phase].name);
        phase++;

        // C3 Shooting Parameters
        // Diamond Ring, Baily's Beats, ...
        magic_program[phase] = new shoot_program("Contact3", 3,3,-5, +5, -1, "S0,B0,C0");
        int[] C3ShootingBursts          = {3,             0}; // 0=END, -1, regular (no burst)
        int[] C3ShootingISOs            = {100,           0}; // 0=END
        int[][] C3ShootingShutterSpeeds = {{1,4000},    {0,0}};
        for (int i=0; C3ShootingISOs[i]>0; i++) {
            magic_program[phase].Bursts[i]     = C3ShootingBursts[i];
            magic_program[phase].ISOs[i]       = C3ShootingISOs[i];
            magic_program[phase].Fs[i]         = 0;
            magic_program[phase].ShutterSpeeds[i][0] = C3ShootingShutterSpeeds[i][0];
            magic_program[phase].ShutterSpeeds[i][1] = C3ShootingShutterSpeeds[i][1];
        }
        Logger.log(magic_program[phase].name);
        phase++;

        // C3..C4 Partial2 Shooting
        magic_program[phase] = new shoot_program("Partial2", 3,4,+30, -30, 32, "S0,B0,C0");
        int[] P2PartialBursts          = {-1,             0,       0 }; // 0=END, -1, regular (no burst)
        int[] P2PartialISOs            = {400,          400,       0 }; // 0=END
        int[][] P2PartialShutterSpeeds = {{1,1000},    {1, 2000}, {0,0}};
        for (int i=0; P2PartialISOs[i]>0; i++) {
            magic_program[phase].Bursts[i]     = P2PartialBursts[i];
            magic_program[phase].ISOs[i]       = P2PartialISOs[i];
            magic_program[phase].Fs[i]         = 0;
            magic_program[phase].ShutterSpeeds[i][0] = P2PartialShutterSpeeds[i][0];
            magic_program[phase].ShutterSpeeds[i][1] = P2PartialShutterSpeeds[i][1];
        }
        Logger.log(magic_program[phase].name);
        phase++;

        // END BLOCK
        magic_program[phase] = new shoot_program("END", 4,4, +30, +30, 0, "S0,B0,C0");
        Logger.log(magic_program[phase].name);


        isoPartial = 400;
        isoDiamond = 800;
        isoTotal   = 4000;

        interval = 1;
        rawInterval = 1;
        delay = 0;
        rawDelay = 0;
        shotCount = 1;
        rawShotCount = 1;
        displayOff = false;
        silentShutter = true;
        ael = true;
        fps = 0;
        brs = true;
        mf = true;
    }

    public Settings(int tc1, int tc2, int tc3, int tc4, int isoPartial, int isoDiamond, int isoTotal, double interval, int shotCount, int delay, boolean displayOff, boolean silentShutter, boolean ael, boolean brs, boolean mf) {
        this.tc1 = tc1;
        this.tc2 = tc2;
        this.tc3 = tc3;
        this.tc4 = tc4;

        this.isoPartial = isoPartial;
        this.isoDiamond = isoDiamond;
        this.isoTotal   = isoTotal;

        this.interval = interval;
        this.delay = delay;
        this.shotCount = shotCount;
        this.displayOff = displayOff;
        this.silentShutter = silentShutter;
        this.ael = ael;
        this.brs = brs;
        this.mf = mf;
    }

    void putInIntent(Intent intent) {
        intent.putExtra(EXTRA_TC1, tc1);
        intent.putExtra(EXTRA_TC2, tc2);
        intent.putExtra(EXTRA_TC3, tc3);
        intent.putExtra(EXTRA_TC4, tc4);

        intent.putExtra(EXTRA_ISOPARTIAL, isoPartial);
        intent.putExtra(EXTRA_ISODIAMOND, isoDiamond);
        intent.putExtra(EXTRA_ISOTOTAL, isoTotal);

        intent.putExtra(EXTRA_INTERVAL, interval);
        intent.putExtra(EXTRA_SHOTCOUNT, shotCount);
        intent.putExtra(EXTRA_DELAY, delay);
        intent.putExtra(EXTRA_DISPLAYOFF, displayOff);
        intent.putExtra(EXTRA_SILENTSHUTTER, silentShutter);
        intent.putExtra(EXTRA_AEL, ael);
        intent.putExtra(EXTRA_BRS, brs);
        intent.putExtra(EXTRA_MF, mf);
    }

    static Settings getFromIntent(Intent intent) {
        return new Settings(
                intent.getIntExtra(EXTRA_TC1, 43200),
                intent.getIntExtra(EXTRA_TC2, 46810),
                intent.getIntExtra(EXTRA_TC3, 47050),
                intent.getIntExtra(EXTRA_TC4, 61220),
                intent.getIntExtra(EXTRA_ISOPARTIAL, 400),
                intent.getIntExtra(EXTRA_ISODIAMOND, 800),
                intent.getIntExtra(EXTRA_ISOTOTAL, 4000),
                intent.getDoubleExtra(EXTRA_INTERVAL, 1),
                intent.getIntExtra(EXTRA_SHOTCOUNT, 1),
                intent.getIntExtra(EXTRA_DELAY, 1),
                intent.getBooleanExtra(EXTRA_DISPLAYOFF, false),
                intent.getBooleanExtra(EXTRA_SILENTSHUTTER, true),
                intent.getBooleanExtra(EXTRA_AEL, false),
                intent.getBooleanExtra(EXTRA_BRS, false),
                intent.getBooleanExtra(EXTRA_MF, true)
        );
    }

    void save(Context context)
    {
        SharedPreferences sharedPref = getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("interval", rawInterval);
        editor.putInt("shotCount", rawShotCount);
        editor.putInt("delay", rawDelay);
        editor.putBoolean("silentShutter", silentShutter);
        editor.putBoolean("ael", ael);
        editor.putInt("fps", fps);
        editor.putBoolean("brs", brs);
        editor.putBoolean("mf", mf);
        editor.putBoolean("displayOff", displayOff);
        editor.apply();
    }

    void load(Context context)
    {
        SharedPreferences sharedPref = getDefaultSharedPreferences(context);
        rawInterval = sharedPref.getInt("interval", rawInterval);
        rawShotCount = sharedPref.getInt("shotCount", rawShotCount);
        rawDelay = sharedPref.getInt("delay", rawDelay);
        silentShutter = sharedPref.getBoolean("silentShutter", silentShutter);
        ael = sharedPref.getBoolean("ael", ael);
        fps = sharedPref.getInt("fps", fps);
        brs = sharedPref.getBoolean("brs", brs);
        mf = sharedPref.getBoolean("mf", mf);
        displayOff = sharedPref.getBoolean("displayOff", displayOff);
    }
}
