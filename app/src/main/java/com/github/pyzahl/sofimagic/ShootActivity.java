package com.github.pyzahl.sofimagic;

import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Time;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import android.util.Pair;

import com.github.ma1co.pmcademo.app.BaseActivity;

import com.sony.scalar.hardware.CameraEx;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ShootActivity extends BaseActivity implements SurfaceHolder.Callback, CameraEx.ShutterListener
{
    private Settings settings;

    private int MagicPhase=0;
    private int shotCount=0;
    private int exposureCount = 0;
    private int repeatCount = 0;
    private long delay_to_next_burst = 0;

    private long remainingTimeToContactPhase = 0;
    private long remainingTimeThisPhase = 0;
    private long remainingTimeNextBurst = 0;

    private TextView tvCount, tvBattery, tvRemaining, tvNextShot, tvNextCT;
    private LinearLayout llEnd;

    private SurfaceView reviewSurfaceView;
    private SurfaceHolder cameraSurfaceHolder;
    private CameraEx cameraEx;
    private Camera camera;
    private CameraEx.AutoPictureReviewControl autoReviewControl;
    private int pictureReviewTime;

    private boolean burstShooting;
    private boolean stopPicturePreview;
    private boolean takingPicture;

    private long shootTime;
    private long shootStartTime;

    private Display display;

    static private final boolean SHOW_END_SCREEN = true;

    int getcnt(){
        if(settings.brs){
            return 3;
        }
        return 1;
    }

    private Handler shootRunnableHandler = new Handler();
    private final Runnable shootRunnable = new Runnable() {
        @Override
        public void run() {
            if (stopPicturePreview) {
                stopPicturePreview = false;
                camera.stopPreview();
                reviewSurfaceView.setVisibility(View.GONE);
                if (settings.displayOff)
                    display.off();
            }

            long now = getMilliSecondsOfDay();
            log("shootRunnable " + getHMSfromMS(now) + " MagicPhase[" + Integer.toString(MagicPhase) + "]" + settings.magic_program[MagicPhase].name + " SC:" + Integer.toString(shotCount) + " EC:" + Integer.toString(exposureCount) + " RC:" + Integer.toString(repeatCount) );
            // if aborted, it will continue at the right phase automatically and skips forward as required!
            do {
                remainingTimeToContactPhase = settings.magic_program[MagicPhase].get_remainingTimeToStart(now);
                remainingTimeThisPhase      = settings.magic_program[MagicPhase].get_remainingTime(now);
                log("shootRunnable: remaining time to MagicPhase " + settings.magic_program[MagicPhase].name + " @"+getHMSfromMS((long)settings.magic_program[MagicPhase].get_start_time()*1000) +" #" + Integer.toString(MagicPhase) + " start in: " + getHMSfromMS(remainingTimeToContactPhase));
                if (remainingTimeThisPhase <= 0) { // this time is up!
                    log("shootRunnable: skipping to next phase...");
                    if (settings.magic_program[MagicPhase].number_shots != 0) {
                        MagicPhase++;
                        shotCount = 0; // reset shoot count for phase
                        exposureCount = 0;
                        repeatCount = 0;
                    }
                }
            }while (remainingTimeThisPhase <= 0 && settings.magic_program[MagicPhase].number_shots != 0); // skip forward if past this phase

            if (remainingTimeToContactPhase <= 150 && settings.magic_program[MagicPhase].number_shots != 0) { // 300ms is vaguely the time this postDelayed is to slow
                log("shootRunnable: set go shot!");
                long remainingTimeToNextContactPhase = settings.magic_program[MagicPhase + 1].get_remainingTimeToStart(now);
                if (remainingTimeToNextContactPhase <= 150) {
                    if (settings.magic_program[MagicPhase + 1].number_shots != 0) { // make sure not at end
                        MagicPhase++;
                        log("shootRunnable: Entering Next MagicPhase " + settings.magic_program[MagicPhase].name + " #" + Integer.toString(MagicPhase));
                    }
                    shotCount = 0; // reset shoot count for phase
                    exposureCount = 0;
                    repeatCount = 0;
                }

                //display.off();
                // Shoot
                if (settings.magic_program[MagicPhase].number_shots != 0) {
                    shoot(settings.magic_program[MagicPhase].ISOs[exposureCount], settings.magic_program[MagicPhase].ShutterSpeeds[exposureCount]);
                    log("shootRunnable: shoot fired, next exposure");
                    exposureCount++;
                }

                // check if exposure set completed
                if ((settings.magic_program[MagicPhase].ISOs[exposureCount] == 0 || exposureCount > 15) && settings.magic_program[MagicPhase].number_shots != 0) { // done with exposure block
                    log("shootRunnable: Exposure Set Completed.");
                    exposureCount = 0; // reset exposure count for phase and repeat exposure block
                    repeatCount++;
                }
            }

            if (settings.magic_program[MagicPhase].number_shots > 0) { // end of exposure list and distributed shots -- else keep going and repeat exposure block
                log("shootRunnable: exposure list completed, repeating.");
                exposureCount = 0; // reset exposure count for phase and repeat exposure block
                int time_of_next_burst = settings.magic_program[MagicPhase].get_TimeOfNext(repeatCount);
                now = getMilliSecondsOfDay();
                remainingTimeNextBurst = settings.magic_program[MagicPhase].get_remainingTimeToNext(repeatCount,now);
                log("shootRunnable: remaining millis to next Exposure Series in " + settings.magic_program[MagicPhase].name + " ##" + Integer.toString(repeatCount) + " next in: " + getHMSfromMS(remainingTimeNextBurst));
                display.on();

                if (remainingTimeNextBurst > 1000) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvCount.setText(Integer.toString(shotCount) + "*" + Integer.toString(repeatCount) + "/" + Integer.toString(settings.magic_program[MagicPhase].number_shots));
                            tvRemaining.setText(getRemainingTime());
                            tvBattery.setText(getBatteryPercentage());
                            int time_of_next_burst = settings.magic_program[MagicPhase].get_TimeOfNext(repeatCount);
                            long now = getMilliSecondsOfDay();
                            remainingTimeNextBurst = settings.magic_program[MagicPhase].get_remainingTimeToNext(repeatCount,now);
                            tvNextCT.setText(getHMSfromMS(remainingTimeToContactPhase));
                            tvNextShot.setText(getHMSfromMS(remainingTimeNextBurst));
                        }
                    });
                } else { // keep shooting!
                    log("shootRunnable: postDelay 200, keep shooting repeating exposure list, minimal delay.");
                    shootRunnableHandler.postDelayed(this, 200); // wait a second and check again
                }

                //long update_next_ms = Math.min(remainingTimeNextBurst-150, 1000);
                //shootRunnableHandler.postDelayed(this, update_next_ms);
                shootRunnableHandler.postDelayed(this, remainingTimeNextBurst-150);

            }

            // END?
            if(settings.magic_program[MagicPhase].number_shots == 0) {
                log("shootRunnable: END of ECLIPSE.");
                display.on();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (SHOW_END_SCREEN) {
                            tvCount.setText("End Of Eclipse.");
                            tvBattery.setVisibility(View.INVISIBLE);
                            tvRemaining.setVisibility(View.INVISIBLE);
                            llEnd.setVisibility(View.VISIBLE);
                        } else {
                            onBackPressed();
                        }
                    }
                });
            }
        }
    };



/*
            if(burstShooting) {
                if (settings.magic_program[3].ISOs [shotCount] > 0)
                    shoot(settings.magic_program[3].ISOs[shotCount], settings.magic_program[3].ShutterSpeeds[shotCount]);
            }
            else if(shotCount < settings.shotCount * getcnt() && settings.magic_program[3].ISOs [shotCount] > 0) {
                long remainingTime = Math.round(shootTime + settings.interval * 1000 - System.currentTimeMillis());
                if(brck.get()>0){
                    remainingTime = -1;
                }

                log("  Remaining Time: " + Long.toString(remainingTime));

                if (remainingTime <= 150) { // 300ms is vaguely the time this postDelayed is to slow
                    brck.getAndDecrement();
                    shoot(settings.magic_program[3].ISOs[shotCount], settings.magic_program[3].ShutterSpeeds[shotCount]);
                    display.on();
                } else {
                    shootRunnableHandler.postDelayed(this, remainingTime-150);
                }
            }
            else {
                display.on();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(SHOW_END_SCREEN) {
                            tvCount.setText("Thank for using this app.");
                            tvBattery.setVisibility(View.INVISIBLE);
                            tvRemaining.setVisibility(View.INVISIBLE);
                            llEnd.setVisibility(View.VISIBLE);
                        }
                        else {
                            onBackPressed();
                        }
                    }
                });
        }
    };
*/

    private Handler manualShutterCallbackCallRunnableHandler = new Handler();
    private final Runnable manualShutterCallbackCallRunnable = new Runnable() {
        @Override
        public void run() {
            onShutter(0, cameraEx);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shoot);

        Intent intent = getIntent();
        settings = Settings.getFromIntent(intent);

        MagicPhase=0;
        delay_to_next_burst = 0;

        exposureCount = 0;
        shotCount = 0;
        repeatCount = 0;

        takingPicture = false;
        burstShooting = settings.interval == 0;

        tvCount = (TextView) findViewById(R.id.tvCount);
        tvBattery = (TextView) findViewById(R.id.tvBattery);
        tvRemaining = (TextView) findViewById(R.id.tvRemaining);
        llEnd = (LinearLayout) findViewById(R.id.llEnd);
        tvNextShot = (TextView) findViewById(R.id.tvNextShot);
        tvNextCT = (TextView) findViewById(R.id.tvNextCT);

        reviewSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        reviewSurfaceView.setZOrderOnTop(false);
        cameraSurfaceHolder = reviewSurfaceView.getHolder();
        cameraSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }


    @Override
    protected void onResume() {
        log("onResume");

        super.onResume();
        cameraEx = CameraEx.open(0, null);
        cameraEx.setShutterListener(this);
        cameraSurfaceHolder.addCallback(this);
        autoReviewControl = new CameraEx.AutoPictureReviewControl();
        //autoReviewControl.setPictureReviewInfoHist(true);
        cameraEx.setAutoPictureReviewControl(autoReviewControl);

        final Camera.Parameters params = cameraEx.getNormalCamera().getParameters();

        try {
            if(settings.mf)
                params.setFocusMode(CameraEx.ParametersModifier.FOCUS_MODE_MANUAL);
            else
                params.setFocusMode("auto");
        }
        catch(Exception ignored)
        {}


        final CameraEx.ParametersModifier modifier = cameraEx.createParametersModifier(params);

        if(burstShooting) {
            try {
                modifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_BURST);
                List driveSpeeds = modifier.getSupportedBurstDriveSpeeds();
                modifier.setBurstDriveSpeed(driveSpeeds.get(driveSpeeds.size() - 1).toString());
                modifier.setBurstDriveButtonReleaseBehave(CameraEx.ParametersModifier.BURST_DRIVE_BUTTON_RELEASE_BEHAVE_CONTINUE);
            } catch (Exception ignored) {
            }
        }
        else {
            modifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_SINGLE);
        }

        // setSilentShutterMode doesn't exist on all cameras
        try {
            modifier.setSilentShutterMode(settings.silentShutter);
        }
        catch(NoSuchMethodError ignored)
        {}

        try{
            //add also AEL if set
            if(settings.ael) {
                modifier.setAutoExposureLock(CameraEx.ParametersModifier.AE_LOCK_ON);
            }
        }
        catch (Exception e){
            //do nothing
        }

        if(settings.brs){
            try{
                modifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_BRACKET);
                modifier.setBracketMode(CameraEx.ParametersModifier.BRACKET_MODE_EXPOSURE);
                modifier.setExposureBracketMode(CameraEx.ParametersModifier.EXPOSURE_BRACKET_MODE_SINGLE);
                modifier.setExposureBracketPeriod(30);
                modifier.setNumOfBracketPicture(3);
            }
            catch (Exception e){
                //do nothing
            }
        }

        cameraEx.getNormalCamera().setParameters(params);

        pictureReviewTime = 2; //autoReviewControl.getPictureReviewTime();
        //log(Integer.toString(pictureReviewTime));


        shotCount = 0;
        shootRunnableHandler.postDelayed(shootRunnable, 500);
        //shootRunnableHandler.postDelayed(shootRunnable, (long) settings.delay * 1000 * 60);
        //shootStartTime = System.currentTimeMillis() + settings.delay * 1000 * 60;

        if(burstShooting) {
            manualShutterCallbackCallRunnableHandler.postDelayed(manualShutterCallbackCallRunnable, 500);
        }

        display = new Display(getDisplayManager());

        if(settings.displayOff) {
            display.turnAutoOff(5000);
        }

        setAutoPowerOffMode(false);

        tvCount.setText(Integer.toString(shotCount)+":"+Integer.toString(repeatCount)+"/"+Integer.toString(settings.magic_program[MagicPhase].number_shots));
        tvRemaining.setText(getRemainingTime());
        tvBattery.setText(getBatteryPercentage());

        int time_of_next_burst = settings.magic_program[MagicPhase].get_start_time() + Math.round(repeatCount * (settings.magic_program[MagicPhase].get_end_time() - settings.magic_program[MagicPhase].get_start_time()) / settings.magic_program[MagicPhase].number_shots);
        long now = getMilliSecondsOfDay();
        remainingTimeNextBurst = Math.round(time_of_next_burst * 1000 - now);
        tvNextCT.setText(getHMSfromMS(remainingTimeToContactPhase));
        tvNextShot.setText(getHMSfromMS(remainingTimeNextBurst));

        //Calendar calendar = getDateTime().getCurrentTime();
        //String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(calendar.getTime());

    }

    @Override
    protected boolean onMenuKeyUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onPause() {
        display.on();
        display.turnAutoOff(Display.NO_AUTO_OFF);

        super.onPause();

        log("on pause");

        shootRunnableHandler.removeCallbacks(shootRunnable);

        if(cameraSurfaceHolder == null)
            log("cameraSurfaceHolder == null");
        else {
            cameraSurfaceHolder.removeCallback(this);
        }

        autoReviewControl = null;

        if(camera == null)
            log("camera == null");
        else {
            camera.stopPreview();
            camera = null;
        }

        if(cameraEx == null)
            log("cameraEx == null");
        else {
            cameraEx.setAutoPictureReviewControl(null);
            cameraEx.release();
            cameraEx = null;
        }

        setAutoPowerOffMode(true);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera = cameraEx.getNormalCamera();
            camera.setPreviewDisplay(holder);
        }
        catch (IOException e) {}
    }

    private void setIso(int iso)
    {
        log("set ISO " + String.valueOf(iso));
        Camera.Parameters params = cameraEx.createEmptyParameters();
        cameraEx.createParametersModifier(params).setISOSensitivity(iso);
        cameraEx.getNormalCamera().setParameters(params);
    }

    private void setShutterSpeed(int sec, int frac)
    {
        int sv = CameraUtilShutterSpeed.getShutterValue(sec, frac);
        int indexSet = CameraUtilShutterSpeed.getShutterValueIndex(sec, frac);
        log("set ShutterSpeed " + String.valueOf(sec) + "/" + String.valueOf(frac) + "s" + "to [i:" + String.valueOf(indexSet) + ", sv:" + String.valueOf(sv) + "] current si: " + String.valueOf(CameraUtilShutterSpeed.getShutterValueIndex(getCurrentShutterSpeed())));

        //cameraEx.adjustShutterSpeed(indexSet);
        //cameraEx.adjustShutterSpeed(sv); // this seams NOT to work or do anything :(

        while (indexSet > CameraUtilShutterSpeed.getShutterValueIndex(getCurrentShutterSpeed())) {
            cameraEx.decrementShutterSpeed();
            log("ShutterSpeed-- " + String.valueOf(CameraUtilShutterSpeed.getShutterValueIndex(getCurrentShutterSpeed())));
        }
        while (indexSet < CameraUtilShutterSpeed.getShutterValueIndex(getCurrentShutterSpeed())) {
            cameraEx.incrementShutterSpeed();
            log("ShutterSpeed++ " + String.valueOf(CameraUtilShutterSpeed.getShutterValueIndex(getCurrentShutterSpeed())));
        }
    }

    private Pair<Integer, Integer> getCurrentShutterSpeed()
    {
        final Camera.Parameters params = cameraEx.getNormalCamera().getParameters();
        final CameraEx.ParametersModifier paramsModifier = cameraEx.createParametersModifier(params);
        return paramsModifier.getShutterSpeed();
    }

    private void shoot(int iso, int[] shutterSpeed) {
        if(takingPicture)
            return;

        setIso(iso);
        setShutterSpeed(shutterSpeed[0],shutterSpeed[1]);

        shootTime = System.currentTimeMillis();
        logshot("Shoot Photo @millis=" + shootTime + " #" + shotCount + " t=" +  String.valueOf(shutterSpeed[0]) +"/" + String.valueOf(shutterSpeed[1]) + "s @ISO" + String.valueOf(iso));

        cameraEx.burstableTakePicture();

        shotCount++;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvCount.setText(Integer.toString(shotCount)+"*"+Integer.toString(repeatCount)+"/"+Integer.toString(settings.magic_program[MagicPhase].number_shots));
                tvRemaining.setText(getRemainingTime());
                tvBattery.setText(getBatteryPercentage());

                int time_of_next_burst = settings.magic_program[MagicPhase].get_start_time() + Math.round(repeatCount * (settings.magic_program[MagicPhase].get_end_time() - settings.magic_program[MagicPhase].get_start_time()) / settings.magic_program[MagicPhase].number_shots);
                long now = getMilliSecondsOfDay();
                remainingTimeNextBurst = Math.round(time_of_next_burst * 1000 - now);
                tvNextCT.setText(getHMSfromMS(remainingTimeToContactPhase));
                tvNextShot.setText(getHMSfromMS(remainingTimeNextBurst));

            }
        });
    }

    private AtomicInteger brck = new AtomicInteger(0);


    // When burst shooting this method is not called automatically
    // Therefore we called it every second manually
    @Override
    public void onShutter(int i, CameraEx cameraEx) {

        if(brck.get()<0){
            brck = new AtomicInteger(0);
            if(getcnt()>1) {
                brck = new AtomicInteger(2);
            }
        }

        if(burstShooting) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvCount.setText(getRemainingTime());
                    tvRemaining.setText("");
                    tvBattery.setText(getBatteryPercentage());
                }
            });

            // just keep shooting until we have all shots
            if (false){ //System.currentTimeMillis() >= shootStartTime + settings.shotCount * 1000) {
                this.cameraEx.cancelTakePicture();
                stopPicturePreview = true;
                display.on();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(SHOW_END_SCREEN) {
                            tvCount.setText("Thanks for using this app!");
                            tvBattery.setVisibility(View.INVISIBLE);
                            tvRemaining.setVisibility(View.INVISIBLE);
                            llEnd.setVisibility(View.VISIBLE);
                        }
                        else {
                            onBackPressed();
                        }
                    }
                });
            }
            else {
                manualShutterCallbackCallRunnableHandler.postDelayed(manualShutterCallbackCallRunnable, 500);
            }
        }
        else {
            this.cameraEx.cancelTakePicture();

            //camera.startPreview();

            if (true) { //shotCount < settings.shotCount * getcnt()) {

                // remaining time to the next shot
                double remainingTime = shootTime + settings.interval * 1000 - System.currentTimeMillis();
                if (brck.get() > 0) {
                    remainingTime = -1;
                }

                log("Remaining Time: " + remainingTime);

                // if the remaining time is negative immediately take the next picture
                if (remainingTime < 0) {
                    stopPicturePreview = false;
                    shootRunnableHandler.post(shootRunnable);
                }
                // show the preview picture for some time
                else {
                    long previewPictureShowTime = Math.round(Math.min(remainingTime, pictureReviewTime * 1000));
                    log("  Stop preview in: " + previewPictureShowTime);
                    reviewSurfaceView.setVisibility(View.VISIBLE);
                    stopPicturePreview = true;
                    shootRunnableHandler.postDelayed(shootRunnable, previewPictureShowTime);
                }
            } else {
                stopPicturePreview = true;
                shootRunnableHandler.postDelayed(shootRunnable, pictureReviewTime * 1000);
            }
        }
    }

    private String getBatteryPercentage()
    {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL ||
                chargePlug == BatteryManager.BATTERY_PLUGGED_USB ||
                chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

        String s = "";
        if(isCharging)
            s = "c ";

        return s + (int)(level / (float)scale * 100) + "%";
    }

    private String getRemainingTime() {
        int time_of_next_burst = settings.magic_program[MagicPhase].get_start_time() + Math.round(repeatCount * (settings.magic_program[MagicPhase].get_end_time() - settings.magic_program[MagicPhase].get_start_time()) / settings.magic_program[MagicPhase].number_shots);
        long now = getMilliSecondsOfDay();
        return "" + Math.round((time_of_next_burst * 1000 - now)/1000) + "s";
        /*
        if(burstShooting)
            return "" + Math.round((settings.shotCount * 1000 - System.currentTimeMillis() + shootStartTime) / 1000) + "s";
        else
            return "" + Math.round((settings.shotCount * getcnt() - shotCount) * settings.interval / 60) + "min";
         */
    }

    @Override
    protected void onAnyKeyDown() {
        display.on();
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}

    @Override
    protected void setColorDepth(boolean highQuality)
    {
        super.setColorDepth(false);
    }


    private void log(String s) {
        Logger.info(s);
    }
    private void logshot(String s) {
        Logger.shootdata(s);
    }

    private void dumpList(List list, String name) {
        log(name);
        log(": ");
        if (list != null)
        {
            for (Object o : list)
            {
                log(o.toString());
                log(" ");
            }
        }
        else
            log("null");
        log("\n");
    }
}
