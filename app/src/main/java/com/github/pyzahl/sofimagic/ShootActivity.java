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
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.TableLayout;

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

public class ShootActivity extends BaseActivity implements SurfaceHolder.Callback, CameraEx.ShutterListener {
    private Settings settings;

    private int MagicPhase = 0;
    private int shotCount = 0;
    private int burstCount = 0;
    private int exposureCount = 0;
    private int repeatCount = 0;
    private int shotErrorCount = 0;
    private long delay_to_next_burst = 0;

    private long remainingTimeToContactPhase = 0;
    private long remainingTimeThisPhase = 0;
    private long remainingTimeNextExposureSet= 0;

    private long endBurstShooting = 0;

    private TextView tvCount, tvBattery, tvRemaining, tvNextShot, tvNextCT;
    private LinearLayout llEnd;

    private PreviewNavView m_previewNavView;

    private TextView m_tvShutter;
    private TextView m_tvAperture;
    private TextView m_tvISO;
    private TextView m_tvExposureCompensation;
    private LinearLayout m_lExposure;
    private TextView m_tvExposure;
    private TextView m_tvLog;
    private TextView m_tvMagnification;
    private TextView m_tvMsg;
    private HistogramView m_vHist;
    private TableLayout m_lInfoBottom;
    private ImageView m_ivDriveMode;
    private ImageView m_ivMode;
    private ImageView m_ivTimelapse;
    private ImageView m_ivBracket;
    private GridView m_vGrid;
    private TextView m_tvHint;
    //private FocusScaleView  m_focusScaleView;
    private View m_lFocusScale;


    // Bracketing
    private int m_bracketStep;  // in 1/3 stops
    private int m_bracketMaxPicCount;
    private int m_bracketPicCount;
    private int m_bracketShutterDelta;
    private boolean m_bracketActive;
    private Pair<Integer, Integer> m_bracketNextShutterSpeed;
    private int m_bracketNeutralShutterIndex;

    // Timelapse
    private int m_autoPowerOffTimeBackup;
    private boolean m_timelapseActive;
    private int m_timelapseInterval;    // ms
    private int m_timelapsePicCount;
    private int m_timelapsePicsTaken;


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
    private long shootEndTime;
    private long shootStartTime;

    private Display display;

    static private final boolean SHOW_END_SCREEN = true;

    int getcnt() {
        if (settings.brs) {
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

            if (MagicPhase==0 && shotCount == 0) { // start preview while waiting first for first shot
                camera.startPreview();
                reviewSurfaceView.setVisibility(View.VISIBLE);
                stopPicturePreview = false;
            }

            boolean shooting = false;

            // Get Time
            long now = getMilliSecondsOfDay();

            // Check in MagicPhase, adjust if needed:
            // if aborted, it will continue at the right phase automatically and skips forward as required!
            do {
                remainingTimeToContactPhase = settings.magic_program[MagicPhase].get_remainingTimeToStart(now);
                remainingTimeThisPhase = settings.magic_program[MagicPhase].get_remainingTime(now);
                if (remainingTimeToContactPhase > 0)
                    log("shootRunnable: remaining time to MagicPhase " + settings.magic_program[MagicPhase].name + " @" + getHMSMSfromMS((long) settings.magic_program[MagicPhase].get_start_time() * 1000) + " #" + Integer.toString(MagicPhase) + " start in: " + getHMSfromMS(remainingTimeToContactPhase));
                else
                    log("shootRunnable " + getHMSMSfromMS(now) + " MagicPhase[" + Integer.toString(MagicPhase) + "]" + settings.magic_program[MagicPhase].name
                            + " SC:" + Integer.toString(shotCount) + " EC:" + Integer.toString(exposureCount) + " RC:" + Integer.toString(repeatCount) + " BC: " + Integer.toString(burstCount)
                            + " ** CF=" + settings.magic_program[MagicPhase].CameraFlags[exposureCount]
                            + ": " + Integer.toString(settings.magic_program[MagicPhase].ShutterSpeeds[exposureCount][0])
                            + "/" + Integer.toString(settings.magic_program[MagicPhase].ShutterSpeeds[exposureCount][1])
                            + " @ISO " + Integer.toString(settings.magic_program[MagicPhase].ISOs[exposureCount]));
                if (remainingTimeThisPhase <= 0) { // this time is up!
                    log("shootRunnable: skipping to next phase...");
                    if (settings.magic_program[MagicPhase].number_shots != 0) {
                        MagicPhase++;
                        exposureCount = 0;
                        repeatCount = 0;
                    }
                }
            } while (remainingTimeThisPhase <= 0 && settings.magic_program[MagicPhase].number_shots != 0); // skip forward if past this phase

            // check if we need to skip forward (in timed series)
            if (settings.magic_program[MagicPhase].number_shots > 0) {
                do {
                    remainingTimeNextExposureSet = settings.magic_program[MagicPhase].get_remainingTimeToNext(repeatCount+1, now);
                    if (remainingTimeNextExposureSet < 0) { // past that shot?
                        log("shootRunnable: skipping to repeat count " + Integer.toString(repeatCount));
                        exposureCount = 0;
                        repeatCount++; // skip
                    }
                } while (remainingTimeNextExposureSet< 0 && settings.magic_program[MagicPhase].ISOs[exposureCount] != 0);
            }

            // CHECK FOR NOT END OF ECLIPSE TO PROCEED
            if (settings.magic_program[MagicPhase].number_shots != 0) {

                // Check: Contineous Drive ? (Burst Operation)
                if (remainingTimeToContactPhase < 150
                        && settings.magic_program[MagicPhase].CameraFlags[exposureCount]=='C' // Burst (Contineous Mode?)
                        && settings.magic_program[MagicPhase].BurstDurations[exposureCount] > 1  // more than 1 shot?
                        && settings.magic_program[MagicPhase].ISOs[exposureCount] != 0){  // not at end of exposure list?

                    if (burstCount == 0) { // Before 1 Burst Shot setup Burst Mode
                        endBurstShooting = 1000*settings.magic_program[MagicPhase].BurstDurations[exposureCount] + System.currentTimeMillis();
                        stopPicturePreview = false;
                        camera.stopPreview();
                        reviewSurfaceView.setVisibility(View.GONE);
                        if (settings.displayOff)
                            display.off();
                        setDriveMode(settings.magic_program[MagicPhase].CameraFlags[exposureCount]);
                    }
                    log("shootRunnable: enter Continueous BurstMode 'C'. BC#" + Integer.toString(burstCount) + " BTime:" + Integer.toString(settings.magic_program[MagicPhase].BurstDurations[exposureCount]) + "s BurstEnd in: " + Long.toString(endBurstShooting-System.currentTimeMillis()) + "ms");

                    if (endBurstShooting > System.currentTimeMillis()) {
                        // this will fire up continuous shooting -- to be canceled.  OnShutter will take over and give control back when burst completed
                        shoot(settings.magic_program[MagicPhase].ISOs[exposureCount], settings.magic_program[MagicPhase].ShutterSpeeds[exposureCount]);
                    } else {
                        log("shootRunnable: BurstShooting END detected. A*** SHOULD NOT GET HERE NORMALLY ** MANAGED by onShutter");
                        shootRunnableHandler.postDelayed(this, 1000); // give some time to store and repeat
                    }
                    return; // DONE with initiate Burst
                } else {
                    if (burstCount > 0) { // end section, reset DriveMode -- just in case of critical timings, should not get here normally
                        log("shootRunnable: BurstShooting END detected. B*** SHOULD NOT GET HERE NORMALLY ** MANAGED by onShutter");
                        setDriveMode('S');
                        burstCount = 0;
                    }
                }

                if (settings.magic_program[MagicPhase].number_shots == -1)
                    remainingTimeNextExposureSet= 0;

                // keep working on exposure list in intervall mode or end of exposure list and distributed shots -- else keep going and repeat exposure block
                if (remainingTimeNextExposureSet > 150
                        || (settings.magic_program[MagicPhase].number_shots > 0 && settings.magic_program[MagicPhase].ISOs[exposureCount] == 0)) {

                    if (settings.magic_program[MagicPhase].ISOs[exposureCount] == 0) {
                        if (exposureCount > 1)
                            log("shootRunnable: exposure list completed, repeating.");
                        exposureCount = 0; // reset exposure count for phase and repeat exposure block
                        repeatCount++;
                    }
                    int time_of_next_burst = settings.magic_program[MagicPhase].get_TimeOfNext(repeatCount);
                    now = getMilliSecondsOfDay();
                    remainingTimeNextExposureSet= settings.magic_program[MagicPhase].get_remainingTimeToNext(repeatCount, now);
                    if (remainingTimeToContactPhase <= 0)  // omit this log when waiting for phase start
                        log("shootRunnable: remaining time to next Exposure Series in " + settings.magic_program[MagicPhase].name + " ##" + Integer.toString(repeatCount) + " next in: " + getHMSMSfromMS(remainingTimeNextExposureSet));

                    if (remainingTimeNextExposureSet < 150) {
                        shoot(settings.magic_program[MagicPhase].ISOs[exposureCount], settings.magic_program[MagicPhase].ShutterSpeeds[exposureCount]);
                    } else {
                        // ...wait for Contact Start Time
                        if (remainingTimeToContactPhase <= 0) // omit this log when waiting for phase start
                            log("shootRunnable: waiting for next exposue block...");
                        if (remainingTimeNextExposureSet > 1500) {
                            display.on();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    update_info();
                                }
                            });
                            shootRunnableHandler.postDelayed(this, 1000); // wait a second and check again, update screen
                        } else {
                            if (remainingTimeNextExposureSet > 250)
                                shootRunnableHandler.postDelayed(this, remainingTimeNextExposureSet- 250); // getting close, wait a little and check again
                            else
                                shootRunnableHandler.postDelayed(this, 250); // getting close, wait a little and check again
                        }
                    }
                    return; // DONE

                } else { // keep shooting, no interval, as many as round of exposure list as possible!
                    if (remainingTimeToContactPhase <= 150 && settings.magic_program[MagicPhase].number_shots != 0) { // 300ms is vaguely the time this postDelayed is to slow
                        long remainingTimeToNextContactPhase = settings.magic_program[MagicPhase + 1].get_remainingTimeToStart(now);
                        if (remainingTimeToNextContactPhase <= 150) {
                            if (settings.magic_program[MagicPhase + 1].number_shots != 0) { // make sure not at end
                                MagicPhase++;
                                log("shootRunnable: Entering Next MagicPhase " + settings.magic_program[MagicPhase].name + " #" + Integer.toString(MagicPhase));
                            }
                            exposureCount = 0;
                            repeatCount = 0;
                        }

                        if (settings.magic_program[MagicPhase].ISOs[exposureCount] == 0) {
                            if (exposureCount > 1)
                                log("shootRunnable: exposure list completed, repeating.");
                            exposureCount = 0; // reset exposure count for phase and repeat exposure block
                            repeatCount++;
                        }

                        //display.off();
                        // Shoot === ***** Shoot, will re-trigger this handler once completed!
                        if (settings.magic_program[MagicPhase].number_shots != 0) {
                            shooting = true;
                            shoot(settings.magic_program[MagicPhase].ISOs[exposureCount], settings.magic_program[MagicPhase].ShutterSpeeds[exposureCount]);
                            //log("shootRunnable: shoot fired, next exposure");
                        }
                    } else {
                        // ...wait for Contact Start Time
                        log("shootRunnable: waiting...");
                        if (remainingTimeToContactPhase > 1500) {
                            display.on();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    update_info();
                                }
                            });
                            shootRunnableHandler.postDelayed(this, 1000); // wait a second and check again, update screen
                        } else {
                            if (remainingTimeToContactPhase > 250)
                                shootRunnableHandler.postDelayed(this, remainingTimeToContactPhase - 250); // wait a little and check again
                            else
                                shootRunnableHandler.postDelayed(this, 250); // wait a little and check again
                        }
                        return; // DONE
                    }

                }
            } else { // END OF ECLIPSE
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


    public void update_info() {
        tvCount.setText(Integer.toString(shotCount) + ":" + Integer.toString(exposureCount) + ":" + Integer.toString(repeatCount) + "/" + Integer.toString(settings.magic_program[MagicPhase].number_shots));
        tvRemaining.setText(getHMSfromMS(remainingTimeNextExposureSet));
        tvBattery.setText(getBatteryPercentage());
        if (remainingTimeToContactPhase > 0)
            tvNextShot.setText(getHMSfromMS(remainingTimeToContactPhase) + " until " + settings.magic_program[MagicPhase].name);
        else
            tvNextShot.setText(getHMSfromMS(remainingTimeThisPhase) + " left for " + settings.magic_program[MagicPhase].name);
        Calendar calendar = getDateTime().getCurrentTime();
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(calendar.getTime());
        tvNextCT.setText(date);
    }

    private Handler manualShutterCallbackCallRunnableHandler = new Handler();
    private final Runnable manualShutterCallbackCallRunnable = new Runnable() {
        @Override
        public void run() {
            onShutter(0, cameraEx);
        }
    };

    private Handler shootPictureCallbackCallRunnableHandler = new Handler();
    private final Runnable shootPictureCallbackCallRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                cameraEx.burstableTakePicture();
                //log("Retry: Shoot Photo" + settings.magic_program[MagicPhase].name + " #" + exposureCount + "#" + repeatCount + " Burst#" + burstCount + " Shot#" + shotCount + " Err#" + shotErrorCount);
            } catch (Exception ignored) {
                shotErrorCount++;
                log("EXCEPTION Retry Camera.burstableTakePicture() fail * " + settings.magic_program[MagicPhase].name + " #" + exposureCount + "#" + repeatCount + " Burst#" + burstCount + " Shot#" + shotCount + " Err#" + shotErrorCount);
                onShutter(1, cameraEx);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shoot);

        Intent intent = getIntent();
        settings = Settings.getFromIntent(intent);

        MagicPhase = 0;
        delay_to_next_burst = 0;

        exposureCount = 0;
        shotCount = 0;
        repeatCount = 0;
        burstCount = 0;
        shotErrorCount = 0;

        endBurstShooting = 0;

        takingPicture = false;
        burstShooting = false;

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


        m_tvMsg = (TextView) findViewById(R.id.tvMsg);

        m_tvAperture = (TextView) findViewById(R.id.tvAperture);
        //m_tvAperture.setOnTouchListener(new ApertureSwipeTouchListener(this));

        m_tvShutter = (TextView) findViewById(R.id.tvShutter);
        //m_tvShutter.setOnTouchListener(new ShutterSwipeTouchListener(this));

        m_tvISO = (TextView) findViewById(R.id.tvISO);
        //m_tvISO.setOnTouchListener(new IsoSwipeTouchListener(this));

        m_tvExposureCompensation = (TextView) findViewById(R.id.tvExposureCompensation);
        //m_tvExposureCompensation.setOnTouchListener(new ExposureSwipeTouchListener(this));
        m_lExposure = (LinearLayout) findViewById(R.id.lExposure);

        m_tvExposure = (TextView) findViewById(R.id.tvExposure);
        //noinspection ResourceType
        m_tvExposure.setCompoundDrawablesWithIntrinsicBounds(SonyDrawables.p_meteredmanualicon, 0, 0, 0);


        m_vHist = (HistogramView) findViewById(R.id.vHist);

        m_tvMagnification = (TextView) findViewById(R.id.tvMagnification);

        m_lInfoBottom = (TableLayout) findViewById(R.id.lInfoBottom);

        m_previewNavView = (PreviewNavView) findViewById(R.id.vPreviewNav);
        m_previewNavView.setVisibility(View.GONE);

/*
        m_ivDriveMode = (ImageView)findViewById(R.id.ivDriveMode);
        m_ivDriveMode.setOnClickListener(this);

        m_ivMode = (ImageView)findViewById(R.id.ivMode);
        m_ivMode.setOnClickListener(this);

        m_ivTimelapse = (ImageView)findViewById(R.id.ivTimelapse);
        //noinspection ResourceType
        m_ivTimelapse.setImageResource(SonyDrawables.p_16_dd_parts_43_shoot_icon_setting_drivemode_invalid);
        m_ivTimelapse.setOnClickListener(this);

        m_ivBracket = (ImageView)findViewById(R.id.ivBracket);
        //noinspection ResourceType
        m_ivBracket.setImageResource(SonyDrawables.p_16_dd_parts_contshot);
        m_ivBracket.setOnClickListener(this);

        m_vGrid = (GridView)findViewById(R.id.vGrid);

        m_tvHint = (TextView)findViewById(R.id.tvHint);
        m_tvHint.setVisibility(View.GONE);

        m_focusScaleView = (FocusScaleView)findViewById(R.id.vFocusScale);
        m_lFocusScale = findViewById(R.id.lFocusScale);
        m_lFocusScale.setVisibility(View.GONE);

        //noinspection ResourceType
        ((ImageView)findViewById(R.id.ivFocusRight)).setImageResource(SonyDrawables.p_16_dd_parts_rec_focuscontrol_far);
        //noinspection ResourceType
        ((ImageView)findViewById(R.id.ivFocusLeft)).setImageResource(SonyDrawables.p_16_dd_parts_rec_focuscontrol_near);

        setDialMode(DialMode.shutter);
*/


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
            if (settings.mf)
                params.setFocusMode(CameraEx.ParametersModifier.FOCUS_MODE_MANUAL);
            else
                params.setFocusMode("auto");
        } catch (Exception ignored) {
        }

        final CameraEx.ParametersModifier modifier = cameraEx.createParametersModifier(params);
        modifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_SINGLE);

        // setSilentShutterMode doesn't exist on all cameras
        try {
            modifier.setSilentShutterMode(settings.silentShutter);
        } catch (NoSuchMethodError ignored) {
        }

        /*
        try {
            //add also AEL if set
            //if (settings.ael) {
                //modifier.setAutoExposureLock(CameraEx.ParametersModifier.AE_LOCK_ON);
            //}
        } catch (Exception e) {
            //do nothing
        }
         */

        if (settings.brs) {
            try {
                modifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_BRACKET);
                modifier.setBracketMode(CameraEx.ParametersModifier.BRACKET_MODE_EXPOSURE);
                modifier.setExposureBracketMode(CameraEx.ParametersModifier.EXPOSURE_BRACKET_MODE_SINGLE);
                modifier.setExposureBracketPeriod(30);
                modifier.setNumOfBracketPicture(3);
            } catch (Exception e) {
                //do nothing
            }
        }

        cameraEx.getNormalCamera().setParameters(params);

        pictureReviewTime = 2; //autoReviewControl.getPictureReviewTime();
        //log(Integer.toString(pictureReviewTime));


        shotCount = 0;
        shootRunnableHandler.postDelayed(shootRunnable, 1000);

        /* // THIS WORKS!
        if (settings.magic_program[MagicPhase].CameraFlags[exposureCount]=='C'){ // Starting with Burst? (Contineous Mode?)
            manualShutterCallbackCallRunnableHandler.postDelayed(manualShutterCallbackCallRunnable, 500);
        }
        */

        display = new Display(getDisplayManager());

        if (settings.displayOff) {
            display.turnAutoOff(5000);
        }

        setAutoPowerOffMode(false);

        update_info();

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

        if (cameraSurfaceHolder == null)
            log("cameraSurfaceHolder == null");
        else {
            cameraSurfaceHolder.removeCallback(this);
        }

        autoReviewControl = null;

        if (camera == null)
            log("camera == null");
        else {
            camera.stopPreview();
            camera = null;
        }

        if (cameraEx == null)
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
        } catch (IOException e) {
        }
    }


    private void setIso(int iso) {
        //log("set ISO " + String.valueOf(iso));
        Camera.Parameters params = cameraEx.createEmptyParameters();
        cameraEx.createParametersModifier(params).setISOSensitivity(iso);
        cameraEx.getNormalCamera().setParameters(params);

        m_tvISO.setText(String.format("\uE488 %d", iso));
        setAp(0); // dummy, just update diusplay
    }

    private void setAp(int ap) {
        final Camera.Parameters params = cameraEx.getNormalCamera().getParameters();
        final CameraEx.ParametersModifier paramsModifier = cameraEx.createParametersModifier(params);
        m_tvAperture.setText(String.format("f%.1f", (float) paramsModifier.getAperture() / 100.0f));
    }

    private void setShutterSpeed(int sec, int frac)
    {
        final int shutterIndex = CameraUtilShutterSpeed.getShutterValueIndex(sec,frac);
        final int shutterDiff = shutterIndex - CameraUtilShutterSpeed.getShutterValueIndex(getCurrentShutterSpeed());
        //log("set ShutterSpeed " + String.valueOf(sec) + "/" + String.valueOf(frac) + "s" + " shutterIndexDiff:" + String.valueOf(shutterDiff));
        if (shutterDiff != 0) {
            cameraEx.adjustShutterSpeed(-shutterDiff);
            final String text = CameraUtilShutterSpeed.formatShutterSpeed(sec, frac);
            m_tvShutter.setText(text);
        }
   }

    private Pair<Integer, Integer> getCurrentShutterSpeed()
    {
        final Camera.Parameters params = cameraEx.getNormalCamera().getParameters();
        final CameraEx.ParametersModifier paramsModifier = cameraEx.createParametersModifier(params);
        return paramsModifier.getShutterSpeed();
    }

    private void setDriveMode(char CFlag) {
        final Camera.Parameters params = cameraEx.getNormalCamera().getParameters();
        final CameraEx.ParametersModifier paramsModifier = cameraEx.createParametersModifier(params);
        switch (CFlag){ // DriveMode Burst Continuous high, middle, low, bracket, single
            case 'C':
                try {
                    log("Setting DriveMode Continuous HIGH");
                    paramsModifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_BURST);
                    paramsModifier.setBurstDriveSpeed(CameraEx.ParametersModifier.BURST_DRIVE_SPEED_HIGH);
                    paramsModifier.setBurstDriveButtonReleaseBehave(CameraEx.ParametersModifier.BURST_DRIVE_BUTTON_RELEASE_BEHAVE_CONTINUE);
                    cameraEx.getNormalCamera().setParameters(params);
                } catch (Exception ignored) {
                    log("EXCEPTION set DriveMode " + CFlag);
                }
                break;
            case 'L':
                try {
                    log("Setting DriveMode Continuous LOW");
                    paramsModifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_BURST);
                    paramsModifier.setBurstDriveSpeed(CameraEx.ParametersModifier.BURST_DRIVE_SPEED_LOW);
                    paramsModifier.setBurstDriveButtonReleaseBehave(CameraEx.ParametersModifier.BURST_DRIVE_BUTTON_RELEASE_BEHAVE_CONTINUE);
                    cameraEx.getNormalCamera().setParameters(params);
                } catch (Exception ignored) {
                    log("EXCEPTION set DriveMode " + CFlag);
                }
                break;
            case 'M':
                try {
                    log("Setting DriveMode Continuous MIDDLE");
                    paramsModifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_BURST);
                    paramsModifier.setBurstDriveSpeed(CameraEx.ParametersModifier.BURST_DRIVE_SPEED_MIDDLE);
                    paramsModifier.setBurstDriveButtonReleaseBehave(CameraEx.ParametersModifier.BURST_DRIVE_BUTTON_RELEASE_BEHAVE_CONTINUE);
                    cameraEx.getNormalCamera().setParameters(params);
                } catch (Exception ignored) {
                    log("EXCEPTION set DriveMode " + CFlag);
                }
                break;
            case 'B':
                try {
                    log("Not yet supported: DriveMode Bracket");
                    paramsModifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_BRACKET);
                    cameraEx.getNormalCamera().setParameters(params);
                } catch (Exception ignored) {
                    log("EXCEPTION set DriveMode " + CFlag);
                }
                break;
            case 'S':
                try {
                    log("Setting DriveMode Single");
                    paramsModifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_SINGLE);
                    cameraEx.getNormalCamera().setParameters(params);
                } catch (Exception ignored) {
                    log("EXCEPTION set DriveMode " + CFlag);
                }
                break;
        }
    }

private void shoot(int iso, int[] shutterSpeed) {
        if(takingPicture)
            return;

        if (burstCount <= 1) { // only at initial burst shot and always otherwise
            setIso(iso);
            setShutterSpeed(shutterSpeed[0], shutterSpeed[1]);
        }

        try {
            shootTime = System.currentTimeMillis(); // " @millis=" + shootTime +
            cameraEx.burstableTakePicture();
            shootEndTime = shootTime+Math.round((double)1000*shutterSpeed[0]/shutterSpeed[1])+150;
            logshot(settings.magic_program[MagicPhase].name, "Shoot Photo E#" + exposureCount + " R#" + repeatCount + " S#" + shotCount + " " +  String.valueOf(shutterSpeed[0]) +"/" + String.valueOf(shutterSpeed[1]) + "s ISO " + String.valueOf(iso));
        } catch (Exception ignored) {
            shotErrorCount++;
            //this.cameraEx.cancelTakePicture();
            log("EXCEPTION Camera.burstableTakePicture() * retry in 500ms * " + settings.magic_program[MagicPhase].name + " #" + exposureCount + "#" + repeatCount + " Shot#" + shotCount + " Err#" + shotErrorCount + " " +  String.valueOf(shutterSpeed[0]) +"/" + String.valueOf(shutterSpeed[1]) + "s ISO " + String.valueOf(iso));
            shootRunnableHandler.postDelayed(shootRunnable, 500);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                update_info();
            }
        });
    }

    private AtomicInteger brck = new AtomicInteger(0);


    // When burst shooting this method is not called automatically
    // Therefore we called it every second manually
    @Override
    public void onShutter(int i, CameraEx cameraEx) {
        // i: 0 = success, 1 = canceled, 2 = error
        //log(String.format("onShutter i: %d\n", i));
        if (i != 0)
        {
            this.cameraEx.cancelTakePicture();
            //log(String.format("onShutter ERROR %d\n", i));
            takingPicture = false;
            log("onShutter ** Canceled/not ready -- delaying, retry shoot picture in 300ms, i: " + Integer.toString(i));
            shootPictureCallbackCallRunnableHandler.postDelayed(shootPictureCallbackCallRunnable, 300);
            return; // not ready/error
        }

        if(brck.get()<0){
            brck = new AtomicInteger(0);
            if(getcnt()>1) {
                brck = new AtomicInteger(2);
            }
        }

        // Burst Shooting?
        if (settings.magic_program[MagicPhase].ISOs[exposureCount]!=0 && settings.magic_program[MagicPhase].CameraFlags[exposureCount] == 'C') {
            if (false) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        update_info();
                    }
                });
            }
            shotCount++;
            if (endBurstShooting > System.currentTimeMillis()) {
                logshot(settings.magic_program[MagicPhase].name, Long.toString(endBurstShooting-System.currentTimeMillis()) + "ms left for Burst Shooting E#" + exposureCount + " R#" + repeatCount + " ~B#" + burstCount + "~ S#" + shotCount);
                burstCount++; // not actual burst count as I do not get the actual Shutter events
                manualShutterCallbackCallRunnableHandler.postDelayed(manualShutterCallbackCallRunnable, 300);
            } else {
                exposureCount++; // next
                burstCount = 0;
                this.cameraEx.cancelTakePicture();
                log("onShutter: Burst completed. cancelTakePicture() now.");
                setDriveMode('S');
                shootRunnableHandler.postDelayed(shootRunnable, 500); // continue manage program
            }
            return;
        }

        // shot completed
        this.cameraEx.cancelTakePicture();
        shotCount++;
        exposureCount++;
        log("onShutter EC:" + Integer.toString(exposureCount)+ ", past TakePicture ms: " +  Long.toString( System.currentTimeMillis() - shootEndTime));

        // end of exposure series?
        if (settings.magic_program[MagicPhase].number_shots > 0 && settings.magic_program[MagicPhase].ISOs[exposureCount] == 0) {
            int time_of_next_burst = settings.magic_program[MagicPhase].get_TimeOfNext(repeatCount + 1);
            long now = getMilliSecondsOfDay();
            // remaining time to the next shot
            long remainingTime = settings.magic_program[MagicPhase].get_remainingTimeToNext(repeatCount + 1, now);

            if (brck.get() > 0) {
                remainingTime = -1;
            }
            log("onShutter: Remaining Time: " + getHMSMSfromMS(remainingTime));

            // if the remaining time is negative or short immediately start over and take the next picture
            if (remainingTime < 2000) {
                stopPicturePreview = true;
                shootRunnableHandler.post(shootRunnable);
            }
            // show the preview picture for some time
            else {
                log("onShutter Preview ON");
                camera.startPreview();
                //long previewPictureShowTime = Math.round(Math.min(remainingTime, pictureReviewTime * 1000));
                reviewSurfaceView.setVisibility(View.VISIBLE);
                stopPicturePreview = false;
                shootRunnableHandler.postDelayed(shootRunnable, 1000); //previewPictureShowTime);
            }
        } else {
            //log("onShutter repeat fast");
            stopPicturePreview = true;
            shootRunnableHandler.postDelayed(shootRunnable, 500);
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
    private void logshot(String p, String s) {
        Logger.shootdata(p, s);
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
