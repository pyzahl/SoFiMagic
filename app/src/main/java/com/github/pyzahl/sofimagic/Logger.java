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

import java.util.Timer;
import java.util.TimerTask;
public class Logger
{
    public static File getFile() {
        return new File(Environment.getExternalStorageDirectory(), "SOFIMAGI/LOG.TXT");
    }

    protected static void log(String msg) {
        try {
            Calendar calendar = DateTime.getInstance().getCurrentTime();
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(calendar.getTime());
            //int timeZoneOffset = calendar.getTimeZone().getRawOffset() / 1000 / 3600;
            //date + " (GMT" + (timeZoneOffset >= 0 ? "+" : "") + timeZoneOffset + ":00)"

            getFile().getParentFile().mkdirs();
            BufferedWriter writer = new BufferedWriter(new FileWriter(getFile(), true));
            writer.append(date + " " + msg);
            writer.newLine();
            writer.close();
        } catch (IOException e) {}
    }
    protected static void log(String type, String msg) { log("[" + type + "] " + msg); }

    public static void info(String msg) { log("INFO", msg); }
    public static void error(String msg) { log("ERROR", msg); }
}
