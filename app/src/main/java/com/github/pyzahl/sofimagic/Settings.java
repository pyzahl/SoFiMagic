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

    private static final String EXTRA_DISPLAYOFF = "com.github.pyzahl.sofimagic.DISPLAYOFF";
    private static final String EXTRA_SILENTSHUTTER = "com.github.pyzahl.sofimagic.SILENTSHUTTER";
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

    public static final int MAX_EXPOSURE_PARAMS = 32;

    class shoot_program {
        public String name; // = new String("NONE");
        shoot_program(String name, int ref_ci, int ref_cf, int ti, int tf, int number_shots) {
            this.name = name;
            this.ref_contact_start = ref_ci;
            this.ref_contact_end = ref_cf;
            this.start_time = ti;
            this.end_time = tf;
            this.number_shots = number_shots;
            CameraFlags = new char[MAX_EXPOSURE_PARAMS];
            for (int i=0; i<CameraFlags.length; ++i) CameraFlags[i] = 'S';
            Fs = new double[MAX_EXPOSURE_PARAMS];
            BurstDurations = new int[MAX_EXPOSURE_PARAMS];
            ISOs = new int[MAX_EXPOSURE_PARAMS];
            ShutterSpeeds = new int[MAX_EXPOSURE_PARAMS][2];
        }

        public boolean skip = false;
        public int ref_contact_start = 0; // 1 = TC1, 2=TC2, 0=MAX=(TC2+TC3)/2. 3=TC3. 4=TC4
        public int ref_contact_end = 0; // 1 = TC1, 2=TC2, 0=MAX=(TC2+TC3)/2. 3=TC3. 4=TC4
        public int start_time = 0; // Time in Seconds relative to ref_contact
        public int end_time = 0; // Time in Seconds relative tp ref_contact
        public int number_shots = 0; // numbershots/burst to distribute
        public char[] CameraFlags;
        public int[] BurstDurations;   //          = {-1,             0,       0 }; // 0=END, -1, regular (no burst)
        public double[] Fs;    //          = {0,          0,       0 }; // 0=do not manage/change
        public int[] ISOs;    //          = {400,          400,       0 }; // 0=END
        public int[][] ShutterSpeeds; // = {{1,1000},    {1, 2000}, {0,0}};

        public String get_CFs_list() {
            String cf_list = "";
            for (int k = 0; k < CameraFlags.length; k++)
                if (ISOs[k] > 0)
                    cf_list = cf_list + String.valueOf(CameraFlags[k]) + ",";
                else break;
            return cf_list;
        }
        public String get_ISOs_list() {
            String ISO_list = "";
            for (int k = 0; k < ISOs.length; ++k) {
                if (ISOs[k] > 0) {
                    ISO_list = ISO_list + Integer.toString(ISOs[k]) + ",";
                } else break;
            }
            return ISO_list;
        }
        public String get_BurstDurations_list() {
            String BC_list = "";
            for (int k = 0; k < BurstDurations.length; ++k) {
                if (ISOs[k] > 0) {
                    BC_list = BC_list + Integer.toString(BurstDurations[k]) + ",";
                } else break;
            }
            return BC_list;
        }
        public String get_Fs_list() {
            String F_list = "";
            for (int k = 0; k < Fs.length; ++k) {
                if (ISOs[k] > 0) {
                    F_list = F_list + Double.toString(Fs[k]) + ",";
                } else break;
            }
            return F_list;
        }
        public String get_ShutterSpeeds_list() {
            String SHUTTER_list = "";
            for (int k = 0; k < ShutterSpeeds.length; ++k) {
                if (ISOs[k] > 0) {
                    SHUTTER_list = SHUTTER_list + Integer.toString(ShutterSpeeds[k][0]) + "/" + Integer.toString(ShutterSpeeds[k][1]) + ",";
                } else break;
            }
            return SHUTTER_list;
        }

        public void set_CFs_list(String CFs_list) {
            String[] cf_list = CFs_list.split(",");
            int k;
            for (k = 0; k < cf_list.length && k < CameraFlags.length; k++)
                CameraFlags[k] = cf_list[k].toCharArray()[0];
            for (; k < CameraFlags.length; k++)
                CameraFlags[k] = 'S'; // this will terminate no need to clear the other lists.
        }

        public void set_ISOs_list(String ISO_list) {
            String[] iso_list = ISO_list.split(",");
            int k;
            for (k = 0; k < iso_list.length && k < ISOs.length; k++)
                ISOs[k] = Integer.parseInt(iso_list[k]);
            for (; k < ISOs.length; k++)
                ISOs[k] = 0; // this will terminate no need to clear the other lists.
        }
        public void set_BurstDurations_list(String BC_list) {
            String[] bc_list = BC_list.split(",");
            int k;
            for (k = 0; k < bc_list.length && k < BurstDurations.length; k++)
                BurstDurations[k] = Integer.parseInt(bc_list[k]);
            for (; k < BurstDurations.length; k++)
                BurstDurations[k] = 0;
        }
        public void set_F_list(String F_list) {
            String[] f_list = F_list.split(",");
            int k;
            for (k = 0; k < f_list.length && k < Fs.length; k++)
                Fs[k] = Integer.parseInt(f_list[k]);
        }
        public void set_SHUTTER_SPEEDS_list(String SHUTTER_SPEED_list) {
            String[] ss_list = SHUTTER_SPEED_list.split(",");
            int k;
            for (k = 0; k < ss_list.length && k < ShutterSpeeds.length; k++) {
                String[] ss = ss_list[k].split("/");
                ShutterSpeeds[k][0] = Integer.parseInt(ss[0]);
                ShutterSpeeds[k][1] = Integer.parseInt(ss[1]);
            }
        }

        public int get_start_time() {
                return get_TC(ref_contact_start) + start_time;
        }
        public int get_end_time() {
            return get_TC(ref_contact_end) + end_time;
        }

        public long get_remainingTimeToStart(long as_of_ms) {
            return Math.round((long)get_start_time() * 1000.0 - as_of_ms); // Milli Sec
        }
        public long get_remainingTime(long as_of_ms) {
            return Math.round((long)get_end_time() * 1000.0 - as_of_ms); // Milli Sec
        }
        public int get_TimeOfNext(int count) {
            if (number_shots > 1)
                return get_start_time() + count*(get_end_time()-get_start_time())/(number_shots-1); // time in Sec
            else
                return get_start_time();
        }
        public long get_remainingTimeToNext(int count, long as_of_ms) {
            return Math.round((long)get_TimeOfNext(count) * 1000.0 - as_of_ms); // Milli Sec
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

    int shotCount;
    boolean displayOff;
    boolean silentShutter;
    int fps;    // index
    boolean brs;
    boolean mf;

    Settings() {

        Logger.log("Settings Init Magic Program");
        magic_program = new shoot_program[8]; // Partial1, Contact2, TotalA, TotalMax, TotalB, Contact3, Partial2, END

        // Default Contact Times
        tc1 = 3600*12;
        tc2 = tc1+60*60+10*60;
        tc3 = tc2+4*60;
        tc4 = tc3+60*60+10*60;

        // initialze programn defaults
        int phase=0;    // 0,... 7 for Partial1, Contact2, TotalA, TotalMax, TotalB, Contact3, Partial2

        // C1..C2 Partial1 Shooting
        magic_program[phase] = new shoot_program("Partial1", 1,2,-30, -10, 100);
        int[] P1PartialBursts          = {0,          0,         0,         0 };  // only used for Continuous Shooting Limit (Burst Mode) CFlag = C: Bursting time in sec.
        int[] P1PartialISOs            = {400,        400,       320,       0 }; // 0=END
        int[][] P1PartialShutterSpeeds = {{1,3200},  {1, 2000}, {1, 3200}, {0,0}};
        for (int i=0; P1PartialISOs[i]>0; i++) {
            magic_program[phase].CameraFlags[i]= 'S';
            magic_program[phase].BurstDurations[i]= P1PartialBursts[i];
            magic_program[phase].ISOs[i]       = P1PartialISOs[i];
            magic_program[phase].Fs[i]         = 0;
            magic_program[phase].ShutterSpeeds[i][0] = P1PartialShutterSpeeds[i][0];
            magic_program[phase].ShutterSpeeds[i][1] = P1PartialShutterSpeeds[i][1];
        }
        phase++;

        // C2 Shooting Parameters
        // Diamond Ring, Baily's Beats, ...
        magic_program[phase] = new shoot_program("Contact2", 2,2,-6, +6, -1);
        int[] C2ShootingBursts          = {12,            0}; // Bursting time in sec.
        int[] C2ShootingISOs            = {100,           0}; // 0=END
        int[][] C2ShootingShutterSpeeds = {{1,4000},    {0,0}};
        for (int i=0; C2ShootingISOs[i]>0; i++) {
            magic_program[phase].CameraFlags[i]= 'C';
            magic_program[phase].BurstDurations[i]= C2ShootingBursts[i]; // Contineous shooting Time
            magic_program[phase].ISOs[i]       = C2ShootingISOs[i];
            magic_program[phase].Fs[i]         = 0;
            magic_program[phase].ShutterSpeeds[i][0] = C2ShootingShutterSpeeds[i][0];
            magic_program[phase].ShutterSpeeds[i][1] = C2ShootingShutterSpeeds[i][1];
        }
        phase++;

        // Totality C2..C3 parameters in three sections.
        // Totality C2...Max
        magic_program[phase] = new shoot_program("TotalityA", 2,0, +6, -30, -1);
        int[] TotalityABursts          = {0,          0,        0,        0,        0,        0,        0,        0,        0,        0,        0,         0 };
        int[] TotalityAISOs            = { 50,       100,      400,      800,      800,      800,      800,      800,      800,      800,      800,         0 }; // 0=END
        int[][] TotalityAShutterSpeeds = { {1,4000}, {1,2000}, {1,1000}, {1,1000}, {1, 500}, {1, 250}, {1, 100}, {1,  50}, {1,  20}, {1,   4}, {1,  1},    {0,0} };
        for (int i=0; TotalityAISOs[i]>0; i++) {
            magic_program[phase].CameraFlags[i]= 'S';
            magic_program[phase].BurstDurations[i]= TotalityABursts[i];
            magic_program[phase].ISOs[i]       = TotalityAISOs[i];
            magic_program[phase].Fs[i]         = 0;
            magic_program[phase].ShutterSpeeds[i][0] = TotalityAShutterSpeeds[i][0];
            magic_program[phase].ShutterSpeeds[i][1] = TotalityAShutterSpeeds[i][1];
        }
        phase++;

        // Totality Max..C3
        magic_program[phase] = new shoot_program("MaxTotality", 0,0,-30, +30, -1);
        int[] MaxTotalityBursts          = {0,          0,        0,        0,        0,        0,        0,        0,        0,        0,        0,        0,           0 };
        int[] MaxTotalityISOs            = { 100,      400,      800,      800,      800,      800,      800,      800,      800,      800,      800,      800,          0 }; // 0=END
        int[][] MaxTotalityShutterSpeeds = { {1,1000}, {1,1000}, {1,1000}, {1, 500}, {1,1000}, {1, 500}, {1,1000}, {1, 500}, {1, 100}, {1,  20}, {1,   1}, {2, 1},      {0,0} };
        for (int i=0; MaxTotalityISOs[i]>0; i++) {
            magic_program[phase].CameraFlags[i]= 'S';
            magic_program[phase].BurstDurations[i]= MaxTotalityBursts[i];
            magic_program[phase].ISOs[i]       = MaxTotalityISOs[i];
            magic_program[phase].Fs[i]         = 0;
            magic_program[phase].ShutterSpeeds[i][0] = MaxTotalityShutterSpeeds[i][0];
            magic_program[phase].ShutterSpeeds[i][1] = MaxTotalityShutterSpeeds[i][1];
        }
        phase++;

        // Totality Max..C3
        magic_program[phase] = new shoot_program("TotalityB", 0,3,+30, -6,-1);
        int[] TotalityBBursts          = {0,          0,        0,        0,        0,        0,        0,        0,        0,        0,        0,         0 }; // 0=END, -1, regular (no burst)
        int[] TotalityBISOs            = { 50,       100,      400,      800,      800,      800,      800,      800,      800,      800,      800,         0 }; // 0=END
        int[][] TotalityBShutterSpeeds = { {1,4000}, {1,2000}, {1,1000}, {1,1000}, {1, 500}, {1, 250}, {1, 100}, {1,  50}, {1,  20}, {1,   4}, {1,  1},    {0,0} };
        for (int i=0; TotalityBISOs[i]>0; i++) {
            magic_program[phase].CameraFlags[i]= 'S';
            magic_program[phase].BurstDurations[i]= TotalityBBursts[i];
            magic_program[phase].ISOs[i]       = TotalityBISOs[i];
            magic_program[phase].Fs[i]         = 0;
            magic_program[phase].ShutterSpeeds[i][0] = TotalityBShutterSpeeds[i][0];
            magic_program[phase].ShutterSpeeds[i][1] = TotalityBShutterSpeeds[i][1];
        }
        phase++;

        // C3 Shooting Parameters
        // Diamond Ring, Baily's Beats, ...
        magic_program[phase] = new shoot_program("Contact3", 3,3,-6, +6, -1);
        int[] C3ShootingBursts          = {12,            0};  // Bursting time in sec.
        int[] C3ShootingISOs            = {100,           0}; // 0=END
        int[][] C3ShootingShutterSpeeds = {{1,4000},    {0,0}};
        for (int i=0; C3ShootingISOs[i]>0; i++) {
            magic_program[phase].CameraFlags[i]= 'C';
            magic_program[phase].BurstDurations[i]= C3ShootingBursts[i];
            magic_program[phase].ISOs[i]       = C3ShootingISOs[i];
            magic_program[phase].Fs[i]         = 0;
            magic_program[phase].ShutterSpeeds[i][0] = C3ShootingShutterSpeeds[i][0];
            magic_program[phase].ShutterSpeeds[i][1] = C3ShootingShutterSpeeds[i][1];
        }
        phase++;

        // C3..C4 Partial2 Shooting
        magic_program[phase] = new shoot_program("Partial2", 3,4,+10, +30, 100);
        int[] P2PartialBursts          = {0,          0,         0,         0 };  // only used for Continuous Shooting Limit (Burst Mode) CFlag = C: Bursting time in sec.
        int[] P2PartialISOs            = {400,        400,       320,       0 }; // 0=END
        int[][] P2PartialShutterSpeeds = {{1,3200},  {1, 2000}, {1, 3200}, {0,0}};
        for (int i=0; P2PartialISOs[i]>0; i++) {
            magic_program[phase].CameraFlags[i]= 'S';
            magic_program[phase].BurstDurations[i]= P2PartialBursts[i];
            magic_program[phase].ISOs[i]       = P2PartialISOs[i];
            magic_program[phase].Fs[i]         = 0;
            magic_program[phase].ShutterSpeeds[i][0] = P2PartialShutterSpeeds[i][0];
            magic_program[phase].ShutterSpeeds[i][1] = P2PartialShutterSpeeds[i][1];
        }
        phase++;

        // END BLOCK
        magic_program[phase] = new shoot_program("END", 4,4, +30, +30, 0);

        shotCount = 1;
        displayOff = false;
        silentShutter = true;
        fps = 0;
        brs = true;
        mf = true;
    }

    public Settings(int tc1, int tc2, int tc3, int tc4, boolean displayOff, boolean silentShutter, boolean brs, boolean mf) {
        this.tc1 = tc1;
        this.tc2 = tc2;
        this.tc3 = tc3;
        this.tc4 = tc4;

        this.shotCount = shotCount;
        this.displayOff = displayOff;
        this.silentShutter = silentShutter;
        this.brs = brs;
        this.mf = mf;
    }

    void putInIntent(Intent intent) {
        intent.putExtra(EXTRA_TC1, tc1);
        intent.putExtra(EXTRA_TC2, tc2);
        intent.putExtra(EXTRA_TC3, tc3);
        intent.putExtra(EXTRA_TC4, tc4);

        intent.putExtra(EXTRA_DISPLAYOFF, displayOff);
        intent.putExtra(EXTRA_SILENTSHUTTER, silentShutter);
        intent.putExtra(EXTRA_BRS, brs);
        intent.putExtra(EXTRA_MF, mf);
    }

    static Settings getFromIntent(Intent intent) {
        return new Settings(
                intent.getIntExtra(EXTRA_TC1, 43200),
                intent.getIntExtra(EXTRA_TC2, 46810),
                intent.getIntExtra(EXTRA_TC3, 47050),
                intent.getIntExtra(EXTRA_TC4, 61220),
                intent.getBooleanExtra(EXTRA_DISPLAYOFF, false),
                intent.getBooleanExtra(EXTRA_SILENTSHUTTER, true),
                intent.getBooleanExtra(EXTRA_BRS, false),
                intent.getBooleanExtra(EXTRA_MF, true)
        );
    }

    void save(Context context)
    {
        SharedPreferences sharedPref = getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("silentShutter", silentShutter);
        editor.putInt("fps", fps);
        editor.putBoolean("brs", brs);
        editor.putBoolean("mf", mf);
        editor.putBoolean("displayOff", displayOff);
        editor.apply();
    }

    void load(Context context)
    {
        SharedPreferences sharedPref = getDefaultSharedPreferences(context);
        silentShutter = sharedPref.getBoolean("silentShutter", silentShutter);
        fps = sharedPref.getInt("fps", fps);
        brs = sharedPref.getBoolean("brs", brs);
        mf = sharedPref.getBoolean("mf", mf);
        displayOff = sharedPref.getBoolean("displayOff", displayOff);
    }
}
