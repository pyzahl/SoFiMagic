package com.github.pyzahl.sofimagic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

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

    int tc1;
    int tc2;
    int tc3;
    int tc4;

    int isoPartial;
    int isoDiamond;
    int isoTotal;

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

        tc1 = 3600*12;
        tc2 = tc1+60*60+10;
        tc3 = tc2+4*60;
        tc4 = tc3+60*60+10;

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
