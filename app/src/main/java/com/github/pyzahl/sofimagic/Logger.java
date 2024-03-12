package com.github.pyzahl.sofimagic;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import android.text.format.Time;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import com.github.ma1co.openmemories.framework.DateTime;
import com.github.ma1co.pmcademo.app.BaseActivity;

import java.util.Timer;
import java.util.TimerTask;
public class Logger
{
    public static File getFile() {
        return new File(Environment.getExternalStorageDirectory(), "SOFIMAGI/LOG.TXT");
    }

    static private int level=2;

    protected static void log_time_ms(String msg) {
        try {
            String date=BaseActivity.getHMSMSfromMS(BaseActivity.getMilliSecondsOfDay());
            getFile().getParentFile().mkdirs();
            BufferedWriter writer = new BufferedWriter(new FileWriter(getFile(), true));
            writer.append(date + " " + msg);
            writer.newLine();
            writer.close();
        } catch (IOException e) {}
    }

    protected static void log(String msg) {
        try {
            Calendar calendar = DateTime.getInstance().getCurrentTime();
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(calendar.getTime());
            int ms = calendar.get(Calendar.MILLISECOND);
            getFile().getParentFile().mkdirs();
            BufferedWriter writer = new BufferedWriter(new FileWriter(getFile(), true));
            writer.append(String.format("%s.%03d %s",date,ms,msg));
            writer.newLine();
            writer.close();
        } catch (IOException e) {}
    }

    public static void set_verbose_level(int l) { level=l; };
    protected static void log(int L, String type, String msg) { if (level>L) log("[" + type + "] " + msg); }
    public static void info_debug(String msg) { log(2, "DEBUG", msg); }
    public static void info(String msg) { log(1, "INFO", msg); }
    public static void info_progress(String msg) { log(0, "INFO", msg); }
    public static void shootdata(String phase, String msg) { log(-1, "PHOTO of "+phase, msg); }
    public static void error(String msg) { log(-1, "ERROR", msg); }
}
