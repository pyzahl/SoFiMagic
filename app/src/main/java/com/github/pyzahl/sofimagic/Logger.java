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

    protected static void log(String type, String msg) { log("[" + type + "] " + msg); }

    public static void info(String msg) { log("INFO", msg); }
    public static void shootdata(String msg) { log("PHOTO", msg); }
    public static void error(String msg) { log("ERROR", msg); }
}
