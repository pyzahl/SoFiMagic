package com.github.pyzahl.sofimagic;

import static com.github.pyzahl.sofimagic.CameraUtilISOs.getApertureValueIndex;
import static com.github.pyzahl.sofimagic.CameraUtilShutterSpeed.SHUTTER_SPEEDS;
import static com.github.pyzahl.sofimagic.CameraUtilShutterSpeed.getShutterValueIndex;

import static java.lang.Math.round;

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
    private long remainingTimeNextExposureSet = 0;
    private long remainingTimeThisExposureSet = 0;

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

    private char currentDriveMode;
    private boolean stopPicturePreview;
    private boolean takingPicture;

    private long shootTime;
    private long shootEndTime;
    private long shutterDuration = 0;
    private long shootStartTime;

    private Display display;

    static private final boolean SHOW_END_SCREEN = true;

    int getcnt() {
        if (settings.brs) {
            return 3;
        }
        return 1;
    }

    void wait_for_next_exposure() {
        // ...wait for Contact Start Time
        log_debug("shootRunnable: waiting... " + getHMSMSfromMS(remainingTimeThisExposureSet));
        if (remainingTimeThisExposureSet > 3000) {

            //stopPicturePreview = false;
            //camera.stopPreview();
            //reviewSurfaceView.setVisibility(View.GONE);
            //if (settings.displayOff)
            //    display.off();
            //else
            //    display.on();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    update_info();
                }
            });
            shootRunnableHandler.postDelayed(shootRunnable, 1000); // wait a second and check again, update screen
        } else {
            if (remainingTimeThisExposureSet > 500)
                shootRunnableHandler.postDelayed(shootRunnable, remainingTimeThisExposureSet - 300); // wait a little and check again
            else
                shootRunnableHandler.postDelayed(shootRunnable, 150); // wait a little and check again
        }
    }

    private Handler shootRunnableHandler = new Handler();
    private final Runnable shootRunnable = new Runnable() {
        @Override
        public void run() {
            if (stopPicturePreview) {
                stopPicturePreview = false;
                camera.stopPreview();
                reviewSurfaceView.setVisibility(View.GONE);
                //if (settings.displayOff)
                //    display.off();
            }

            if (MagicPhase==0 && shotCount == -1) { // start preview while waiting first for first shot
                setDriveMode('S', 0); // set to single
                camera.startPreview();
                reviewSurfaceView.setVisibility(View.VISIBLE);
                stopPicturePreview = false;
                display.on();
                display.turnAutoOff(10000);
                shotCount=0;
            }

            boolean shooting = false;

            // Get Time
            long now = getMilliSecondsOfDay();

            if (settings.magic_program[MagicPhase].ISOs[exposureCount] == 0) {
                if (exposureCount > 0)
                    log_progress("shootRunnable: exposure list completed, repeating.");
                exposureCount = 0; // reset exposure count for phase and repeat exposure block
                repeatCount++;
            }

            // Check in MagicPhase, adjust if needed:
            // if aborted, it will continue at the right phase automatically and skips forward as required!
            do {
                remainingTimeToContactPhase = settings.magic_program[MagicPhase].get_remainingTimeToStart(now); // ms
                remainingTimeThisPhase = settings.magic_program[MagicPhase].get_remainingTime(now); // mm
                remainingTimeNextExposureSet = settings.magic_program[MagicPhase].get_remainingTimeToNext(repeatCount+1, now);

                if (remainingTimeToContactPhase > 0 && ((remainingTimeToContactPhase/1000)%60) == 0)
                        log_progress("shootRunnable: remaining time to MagicPhase " + settings.magic_program[MagicPhase].name + " @" + getHMSMSfromMS((long) settings.magic_program[MagicPhase].get_start_time() * 1000) + " #" + Integer.toString(MagicPhase) + " start in: " + getHMSfromMS(remainingTimeToContactPhase));
                if (remainingTimeToContactPhase < 2000 && settings.magic_program[MagicPhase].ISOs[exposureCount] > 0)
                    log_info("shootRunnable " + getHMSMSfromMS(now) + " MagicPhase[" + Integer.toString(MagicPhase) + "]" + settings.magic_program[MagicPhase].name
                            + " SC:" + Integer.toString(shotCount) + " EC:" + Integer.toString(exposureCount) + " RC:" + Integer.toString(repeatCount) + " BC: " + Integer.toString(burstCount)
                            + " ** CF=" + settings.magic_program[MagicPhase].CameraFlags[exposureCount] + settings.magic_program[MagicPhase].BurstDurations[exposureCount]
                            + ": " + Integer.toString(settings.magic_program[MagicPhase].ShutterSpeeds[exposureCount][0])
                            + "/" + Integer.toString(settings.magic_program[MagicPhase].ShutterSpeeds[exposureCount][1])
                            + " @ISO " + Integer.toString(settings.magic_program[MagicPhase].ISOs[exposureCount])
                            + " Next set in " + getHMSMSfromMS(remainingTimeNextExposureSet)
                    );
                if (remainingTimeThisPhase <= 250) { // this time is up!
                    log_info("shootRunnable: skipping to next phase...");
                    if (settings.magic_program[MagicPhase].number_shots != 0) {
                        MagicPhase++;
                        exposureCount = 0;
                        burstCount = 0;
                        burstShooting = false;
                        repeatCount = 0;
                    }
                }
            } while (remainingTimeThisPhase <= 250 && settings.magic_program[MagicPhase].number_shots != 0); // skip forward if past this phase

            // check if we need to skip forward (in timed series)
            if (settings.magic_program[MagicPhase].number_shots > 0) {
                do {
                    remainingTimeNextExposureSet = settings.magic_program[MagicPhase].get_remainingTimeToNext(repeatCount+1, now);
                    if (remainingTimeNextExposureSet < 0) { // past that shot?
                        log_progress("shootRunnable: skipping to repeat count " + Integer.toString(repeatCount));
                        exposureCount = 0;
                        repeatCount++; // skip
                    }
                } while (remainingTimeNextExposureSet < 0 && settings.magic_program[MagicPhase].ISOs[exposureCount] != 0);
            }
            remainingTimeThisExposureSet = settings.magic_program[MagicPhase].get_remainingTimeToNext(repeatCount, now); // returns Start of Phase if NShots == -1 (no interval)

            // CHECK FOR NOT END OF ECLIPSE TO PROCEED
            if (settings.magic_program[MagicPhase].number_shots != 0) {

                // check for next set start and wait if needed
                if (remainingTimeThisExposureSet > 250) {
                    wait_for_next_exposure();
                    return;
                }

                // CHECK CFLAGS FOR MODE OF OPERATION

                // Check: Contineous Drive ? (Burst Operation)
                if ((settings.magic_program[MagicPhase].CameraFlags[exposureCount] == 'C' // Burst Modes, (Contineous Shooting) High
                        || settings.magic_program[MagicPhase].CameraFlags[exposureCount] == 'L' // Burst Modes, (Contineous Shooting) Low
                        || settings.magic_program[MagicPhase].CameraFlags[exposureCount] == 'M') // Burst Modes, (Contineous Shooting) Medium
                        && settings.magic_program[MagicPhase].BurstDurations[exposureCount] > 1  // more than 1 shot?
                        && settings.magic_program[MagicPhase].ISOs[exposureCount] != 0) {  // not at end of exposure list?

                    if (burstCount == 0) { // Before 1 Burst Shot setup Burst Mode
                        endBurstShooting = 1000 * settings.magic_program[MagicPhase].BurstDurations[exposureCount] + System.currentTimeMillis();
                        stopPicturePreview = false;
                        camera.stopPreview();
                        reviewSurfaceView.setVisibility(View.GONE);
                        setDriveMode(settings.magic_program[MagicPhase].CameraFlags[exposureCount], settings.magic_program[MagicPhase].BurstDurations[exposureCount]);
                    }
                    log_debug("shootRunnable: enter Continueous BurstMode " + Character.toString(settings.magic_program[MagicPhase].CameraFlags[exposureCount]) + " BC#" + Integer.toString(burstCount) + " BTime:" + Integer.toString(settings.magic_program[MagicPhase].BurstDurations[exposureCount]) + "s BurstEnd in: " + Long.toString(endBurstShooting - System.currentTimeMillis()) + "ms");

                    if (endBurstShooting > System.currentTimeMillis()) {
                        burstShooting = true;
                        // this will fire up continuous shooting -- to be canceled.  OnShutter will take over and give control back when burst completed
                        shoot(settings.magic_program[MagicPhase].ISOs[exposureCount], settings.magic_program[MagicPhase].Fs[exposureCount], settings.magic_program[MagicPhase].ShutterSpeeds[exposureCount]);
                    } else {
                        burstShooting = false;
                        log_debug("shootRunnable: BurstShooting END detected. A*** SHOULD NOT GET HERE NORMALLY ** MANAGED by onShutter");
                        shootRunnableHandler.postDelayed(this, 1000); // give some time to store and repeat
                    }
                    return; // DONE with initiate Burst
                } else {
                    if (burstCount > 0) { // end section, reset DriveMode -- just in case of critical timings, should not get here normally
                        log_debug("shootRunnable: BurstShooting END detected. B*** SHOULD NOT GET HERE NORMALLY ** MANAGED by onShutter");
                        burstCount = 0;
                        burstShooting = false;
                        exposureCount++; // next
                        shootRunnableHandler.postDelayed(this, 250); // continue
                        return;
                    }
                }
                // DONE BURST OP

                // Check: Bracket Operation
                if (settings.magic_program[MagicPhase].CameraFlags[exposureCount] == 'B' // Bracketing Mode
                        && settings.magic_program[MagicPhase].BurstDurations[exposureCount] > 1  // more than 1 shot?
                        && settings.magic_program[MagicPhase].ISOs[exposureCount] != 0) {  // not at end of exposure list?

                    if (m_bracketPicCount == 0) { // Before 1 Burst Shot setup Burst Mode
                        m_bracketMaxPicCount = settings.magic_program[MagicPhase].BurstDurations[exposureCount]; // 3, 5, 7
                        m_bracketStep = (settings.magic_program[MagicPhase].BurstDurations[exposureCount] - 1) / 2; // #+/- steps in 1/3 stops
                        if (m_bracketStep > 2) {
                            m_bracketShutterDelta = 2;
                            m_bracketMaxPicCount /= 2;
                        } else
                            m_bracketShutterDelta = 1;

                        m_bracketActive = true;
                        m_bracketNeutralShutterIndex = CameraUtilShutterSpeed.getShutterValueIndex(settings.magic_program[MagicPhase].ShutterSpeeds[exposureCount][0], settings.magic_program[MagicPhase].ShutterSpeeds[exposureCount][1]);
                        setDriveMode(settings.magic_program[MagicPhase].CameraFlags[exposureCount], settings.magic_program[MagicPhase].BurstDurations[exposureCount]);
                    }
                    log_debug("shootRunnable: enter Bracketing Mode 'B'. BC#" + Integer.toString(burstCount) + " #NB:" + Integer.toString(settings.magic_program[MagicPhase].BurstDurations[exposureCount]));

                    if (m_bracketPicCount < m_bracketMaxPicCount) {
                        // this will fire up bracket shooting -- to be canceled.  OnShutter will take over and give control back when burst completed
                        shootPicture(settings.magic_program[MagicPhase].ISOs[exposureCount], settings.magic_program[MagicPhase].Fs[exposureCount], settings.magic_program[MagicPhase].ShutterSpeeds[exposureCount]);
                    } else {
                        log_debug("shootRunnable: BracketShooting END detected. A*** SHOULD NOT GET HERE NORMALLY ** MANAGED by onShutter");
                        m_bracketActive = false;
                        m_bracketPicCount = 0;
                        shootRunnableHandler.postDelayed(this, 250); // give some time to store and repeat ** 1000
                    }
                    return; // DONE with initiate Burst
                }
                else // SINGLE DRIVE SHOOTING MODE 'S':
                {
                    // make sure BURST is ENDED now
                    if (burstCount > 0) { // end section, reset DriveMode -- just in case of critical timings, should not get here normally
                        log_debug("shootRunnable: BracketShooting END detected. B*** SHOULD NOT GET HERE NORMALLY ** MANAGED by onShutter");
                        burstCount = 0;
                        exposureCount++; // next
                        m_bracketActive = false;
                        shootRunnableHandler.postDelayed(this, 250); // continue shortly
                        return;
                    }
                    // set single shot drive mode and shoot normally
                    setDriveMode('S', 0);
                    shoot(settings.magic_program[MagicPhase].ISOs[exposureCount], settings.magic_program[MagicPhase].Fs[exposureCount], settings.magic_program[MagicPhase].ShutterSpeeds[exposureCount]);
                }
            }
            else // END OF ECLIPSE
            {
                log_progress("shootRunnable: END of ECLIPSE.");
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
        tvRemaining.setText(getHMSfromMS(remainingTimeThisExposureSet));
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
                if (m_bracketActive) {
                    cameraEx.getNormalCamera().takePicture(null, null, null);
                    log_debug("Retry: Normal Take Picture" + settings.magic_program[MagicPhase].name + " EC#" + exposureCount + "RC#" + repeatCount + " Burst#" + burstCount + " Shot#" + shotCount + " Err#" + shotErrorCount);
                }else {
                    cameraEx.burstableTakePicture();
                    log_debug("Retry: Burstable Take Picture" + settings.magic_program[MagicPhase].name + " EC#" + exposureCount + "RC#" + repeatCount + " Burst#" + burstCount + " Shot#" + shotCount + " Err#" + shotErrorCount);
                }
            } catch (Exception ignored) {
                shotErrorCount++;
                cameraEx.cancelTakePicture(); // ***
                log_exception("EXCEPTION Retry Camera.burstableTakePicture() fail * " + settings.magic_program[MagicPhase].name + " EC#" + exposureCount + "RC#" + repeatCount + " Burst#" + burstCount + " Shot#" + shotCount + " Err#" + shotErrorCount);
                onShutter(1, cameraEx);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log_debug("onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shoot);

        Intent intent = getIntent();
        settings = Settings.getFromIntent(intent);

        MagicPhase = 0;
        delay_to_next_burst = 0;

        exposureCount = 0;
        shotCount = -1;
        repeatCount = 0;
        burstCount = 0;
        shotErrorCount = 0;

        currentDriveMode = 'S';

        endBurstShooting = 0;
        m_bracketPicCount = 0;

        takingPicture = false;
        burstShooting = false;
        m_bracketActive = false;

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
        log_debug("onResume");

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

        // modifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_SINGLE);

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

        /*
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
        */

        cameraEx.getNormalCamera().setParameters(params);

        pictureReviewTime = 2; //autoReviewControl.getPictureReviewTime();
        //log_debug(Integer.toString(pictureReviewTime));

        /*
        // TEST -- BKT works!!
        setIso(800);
        setShutterSpeed(1, 125);
        setDriveMode('B', 10); // 1EV 3Pic
        logshot("000","TEST PHOTO 1/125s ISO800 BKT10");
        shootTime = System.currentTimeMillis(); // " @millis=" + shootTime +
        cameraEx.getNormalCamera().takePicture(null, null, null);
        logshot("000","TEST PHOTO EXECUTED");
        // END TEST
        */

        cameraEx.cancelTakePicture();

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
    protected boolean onPlayKeyDown() {
        display.on();
        //if (settings.displayOff) {
        display.turnAutoOff(5000);
        //}
        return true;
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

        log_debug("on pause");

        shootRunnableHandler.removeCallbacks(shootRunnable);
        cameraEx.cancelTakePicture();

        if (cameraSurfaceHolder == null)
            log_debug("cameraSurfaceHolder == null");
        else {
            cameraSurfaceHolder.removeCallback(this);
        }

        autoReviewControl = null;

        if (camera == null)
            log_debug("camera == null");
        else {
            camera.stopPreview();
            camera = null;
        }

        if (cameraEx == null)
            log_debug("cameraEx == null");
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
        //log_debug("set ISO " + String.valueOf(iso));
        Camera.Parameters params = cameraEx.createEmptyParameters();
        cameraEx.createParametersModifier(params).setISOSensitivity(iso);
        cameraEx.getNormalCamera().setParameters(params);

        m_tvISO.setText(String.format("\uE488 %d", iso));
    }

    private void setAp(float ap) {
        final Camera.Parameters params = cameraEx.getNormalCamera().getParameters();
        final CameraEx.ParametersModifier paramsModifier = cameraEx.createParametersModifier(params);

        if (ap>0.0) {
            float a = (float)paramsModifier.getAperture() / 100.0f;
            int apertureIndex = getApertureValueIndex(a);
            int apertureDiff = apertureIndex - getApertureValueIndex(ap);
            log_debug("set Aperture: current: " + String.valueOf(a) + " i:" + String.valueOf(apertureIndex) + " new:" + String.valueOf(ap) + " i:" + String.valueOf(getApertureValueIndex(ap)) + " diff:" + String.valueOf(apertureDiff));
            if (apertureDiff != 0) {
                while (apertureDiff > 0) {
                    apertureDiff--;
                    cameraEx.decrementAperture();
                }
                while (apertureDiff < 0) {
                    apertureDiff++;
                    cameraEx.incrementAperture();
                }
                //cameraEx.adjustAperture(-apertureDiff); // not working
            }
        }
        m_tvAperture.setText(String.format("f%.1f", (float) paramsModifier.getAperture() / 100.0f));
    }

    private void setShutterSpeed(int sec, int frac)
    {
        final int shutterIndex = getShutterValueIndex(sec,frac);
        final int shutterDiff = shutterIndex - getShutterValueIndex(getCurrentShutterSpeed());
        //log_debug("set ShutterSpeed " + String.valueOf(sec) + "/" + String.valueOf(frac) + "s" + " shutterIndexDiff:" + String.valueOf(shutterDiff));
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

    private void setDriveMode(char CFlag, int num) {
        if (currentDriveMode == 'S' && CFlag == 'S')
            return;

        currentDriveMode = CFlag;
        final Camera.Parameters params = cameraEx.getNormalCamera().getParameters();
        final CameraEx.ParametersModifier paramsModifier = cameraEx.createParametersModifier(params);
        switch (CFlag){ // DriveMode Burst Continuous high, middle, low, bracket, single
            case 'C':
                try {
                    log_debug("Setting DriveMode Continuous HIGH");
                    if (settings.silentShutter)
                        paramsModifier.setSilentShutterMode(false); // must disable for burst most
                    paramsModifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_BURST);
                    paramsModifier.setBurstDriveSpeed(CameraEx.ParametersModifier.BURST_DRIVE_SPEED_HIGH);
                    paramsModifier.setBurstDriveButtonReleaseBehave(CameraEx.ParametersModifier.BURST_DRIVE_BUTTON_RELEASE_BEHAVE_CONTINUE);
                    //paramsModifier.setNumOfBurstPicture(num); // going by duration
                    cameraEx.getNormalCamera().setParameters(params);
                } catch (Exception ignored) {
                    log_debug("EXCEPTION set DriveMode " + CFlag);
                }
                break;
            case 'L':
                try {
                    log_debug("Setting DriveMode Continuous LOW");
                    if (settings.silentShutter)
                        paramsModifier.setSilentShutterMode(false); // must disable for burst most
                    paramsModifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_BURST);
                    paramsModifier.setBurstDriveSpeed(CameraEx.ParametersModifier.BURST_DRIVE_SPEED_LOW);
                    paramsModifier.setBurstDriveButtonReleaseBehave(CameraEx.ParametersModifier.BURST_DRIVE_BUTTON_RELEASE_BEHAVE_CONTINUE);
                    //paramsModifier.setNumOfBurstPicture(num);
                    cameraEx.getNormalCamera().setParameters(params);
                } catch (Exception ignored) {
                    log_debug("EXCEPTION set DriveMode " + CFlag);
                }
                break;
            case 'M':
                try {
                    log_debug("Setting DriveMode Continuous MIDDLE");
                    if (settings.silentShutter)
                        paramsModifier.setSilentShutterMode(false); // must disable for burst most
                    paramsModifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_BURST);
                    paramsModifier.setBurstDriveSpeed(CameraEx.ParametersModifier.BURST_DRIVE_SPEED_MIDDLE);
                    paramsModifier.setBurstDriveButtonReleaseBehave(CameraEx.ParametersModifier.BURST_DRIVE_BUTTON_RELEASE_BEHAVE_CONTINUE);
                    //paramsModifier.setNumOfBurstPicture(num);
                    cameraEx.getNormalCamera().setParameters(params);
                } catch (Exception ignored) {
                    log_debug("EXCEPTION set DriveMode " + CFlag);
                }
                break;
            case 'B':
                try {
                    if (settings.silentShutter)
                        paramsModifier.setSilentShutterMode(settings.silentShutter);
                    paramsModifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_BRACKET);
                    paramsModifier.setDriveMode(CameraEx.ParametersModifier.BRACKET_MODE_EXPOSURE);
                    //paramsModifier.setExposureBracketMode(CameraEx.ParametersModifier.EXPOSURE_BRACKET_MODE_SINGLE);
                    paramsModifier.setExposureBracketMode(CameraEx.ParametersModifier.EXPOSURE_BRACKET_MODE_CONTINUE);
                    //paramsModifier.setExposureBracketMode(CameraEx.ParametersModifier.BRACKET_STEP_PERIOD_HIGH);
                    //paramsModifier.setExposureBracketMode(CameraEx.ParametersModifier.BRACKET_STEP_PERIOD_LOW);
                    //paramsModifier.setBracketOrder(CameraEx.ParametersModifier.BRACKET_ORDER_START_MINUS);
                    switch (num){
                        case 3: paramsModifier.setExposureBracketPeriod(3); paramsModifier.setNumOfBracketPicture(3); m_bracketMaxPicCount=3; log_debug("Setting DriveMode Bracket 0.3EV,3Pic(experimental)"); break; // 0.3EV
                        case 4: paramsModifier.setExposureBracketPeriod(3); paramsModifier.setNumOfBracketPicture(5); m_bracketMaxPicCount=5; log_debug("Setting DriveMode Bracket 0.3EV,5Pic(experimental)"); break;
                        case 5: paramsModifier.setExposureBracketPeriod(5); paramsModifier.setNumOfBracketPicture(3); m_bracketMaxPicCount=3; log_debug("Setting DriveMode Bracket 0.5EV,3Pic(experimental)"); break; // 0.5EV
                        case 6: paramsModifier.setExposureBracketPeriod(5); paramsModifier.setNumOfBracketPicture(5); m_bracketMaxPicCount=5; log_debug("Setting DriveMode Bracket 0.5EV,5Pic(experimental)"); break;
                        case 7: paramsModifier.setExposureBracketPeriod(7); paramsModifier.setNumOfBracketPicture(3); m_bracketMaxPicCount=3; log_debug("Setting DriveMode Bracket 0.7EV,3Pic(experimental)"); break; // 0.7EV
                        case 8: paramsModifier.setExposureBracketPeriod(7); paramsModifier.setNumOfBracketPicture(5); m_bracketMaxPicCount=5; log_debug("Setting DriveMode Bracket 0.7EV,5Pic(experimental)"); break;
                        case 9: paramsModifier.setExposureBracketPeriod(7); paramsModifier.setNumOfBracketPicture(9); m_bracketMaxPicCount=9; log_debug("Setting DriveMode Bracket 0.7EV,9Pic(experimental)"); break; // ** no all cameras
                        case 10: paramsModifier.setExposureBracketPeriod(10); paramsModifier.setNumOfBracketPicture(3); m_bracketMaxPicCount=3; log_debug("Setting DriveMode Bracket 1EV,3Pic(experimental)"); break; // 1EV
                        case 11: paramsModifier.setExposureBracketPeriod(10); paramsModifier.setNumOfBracketPicture(5); m_bracketMaxPicCount=5; log_debug("Setting DriveMode Bracket 1EV,5Pic(experimental)"); break;
                        case 12: paramsModifier.setExposureBracketPeriod(10); paramsModifier.setNumOfBracketPicture(9); m_bracketMaxPicCount=9; log_debug("Setting DriveMode Bracket 1EV,9Pic(experimental)"); break; // ** no all cameras
                        case 20: paramsModifier.setExposureBracketPeriod(20); paramsModifier.setNumOfBracketPicture(3); m_bracketMaxPicCount=3; log_debug("Setting DriveMode Bracket 2EV,3Pic(experimental)"); break; // 2EV
                        case 21: paramsModifier.setExposureBracketPeriod(20); paramsModifier.setNumOfBracketPicture(5); m_bracketMaxPicCount=5; log_debug("Setting DriveMode Bracket 2EV,5Pic(experimental)"); break;
                        case 22: paramsModifier.setExposureBracketPeriod(20); paramsModifier.setNumOfBracketPicture(9); m_bracketMaxPicCount=9; log_debug("Setting DriveMode Bracket 2EV,9Pic(experimental)"); break; // ** no all cameras
                        case 30: paramsModifier.setExposureBracketPeriod(30); paramsModifier.setNumOfBracketPicture(3); m_bracketMaxPicCount=3; log_debug("Setting DriveMode Bracket 3EV,3Pic(experimental)"); break; // 3EV
                        case 31: paramsModifier.setExposureBracketPeriod(30); paramsModifier.setNumOfBracketPicture(5); m_bracketMaxPicCount=5; log_debug("Setting DriveMode Bracket 3EV,5Pic(experimental)"); break;
                        case 32: paramsModifier.setExposureBracketPeriod(30); paramsModifier.setNumOfBracketPicture(9); m_bracketMaxPicCount=9; log_debug("Setting DriveMode Bracket 3EV,9Pic(experimental)"); break; // ** no all cameras
                        default: paramsModifier.setExposureBracketPeriod(30); paramsModifier.setNumOfBracketPicture(3); m_bracketMaxPicCount=3; log_debug("Setting DriveMode Bracket *3EV,3Pic(experimental)"); break;
                    }
                    //log_debug("SupportedBracketStepPeriods: " + paramsModifier.getSupportedExposureBracketPeriods().toString()); // [3, 5, 7, 10, 20, 30]
                    //log_debug("SupportedBracketStepPeriods: " + paramsModifier.getSupportedBracketStepPeriods().toString()); // [low, high]
                    //log_debug("SupportedNumsOfBracketPicture: " + paramsModifier.getSupportedNumsOfBracketPicture().toString()); // [3,5,9]
                    //log_debug("SupportedExposureBracketmodus: " + paramsModifier.getSupportedExposureBracketModes().toString()); // single, continue]

                    cameraEx.getNormalCamera().setParameters(params);
                } catch (Exception ignored) {
                    log_exception("EXCEPTION set DriveMode " + CFlag);
                }
                break;
            case 'S':
                try {
                    log_debug("Setting DriveMode Single");
                    if (settings.silentShutter)
                        paramsModifier.setSilentShutterMode(settings.silentShutter);
                    paramsModifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_SINGLE);
                    cameraEx.getNormalCamera().setParameters(params);
                } catch (Exception ignored) {
                    log_exception("EXCEPTION set DriveMode " + CFlag);
                }
                break;
        }
    }

    private void shootPicture(int iso, float ap, int[] shutterSpeed) {
        //this.cameraEx.cancelTakePicture(); // TEST

        setIso(iso);
        setShutterSpeed(shutterSpeed[0], shutterSpeed[1]);
        setAp(ap);
        shutterDuration = round((double) 1000 * shutterSpeed[0] / shutterSpeed[1]);
        shootTime = System.currentTimeMillis(); // " @millis=" + shootTime +
        try {
            cameraEx.getNormalCamera().takePicture(null, null, null);
            logshot(settings.magic_program[MagicPhase].name, "Taking Photo Bracket EC#" + exposureCount + " RC#" + repeatCount + " SC#" + shotCount + " "
                    + String.valueOf(shutterSpeed[0]) + "/" + String.valueOf(shutterSpeed[1]) + "s ISO " + String.valueOf(iso) + " F" + String.valueOf(ap));
        } catch (Exception ignored) {
            shotErrorCount++;
            cameraEx.cancelTakePicture();
            log_exception("EXCEPTION in shootPicture: Camera.TakePicture() * retry in 500ms * " + settings.magic_program[MagicPhase].name + " EC#" + exposureCount + "RC#" + repeatCount + " Shot#" + shotCount + " Err#" + shotErrorCount + " " + String.valueOf(shutterSpeed[0]) + "/" + String.valueOf(shutterSpeed[1]) + "s ISO " + String.valueOf(iso));
            shootRunnableHandler.postDelayed(shootRunnable, 500);
        }
    }

    private void shoot(int iso, float ap, int[] shutterSpeed) {
        if (takingPicture)
            return;

        //this.cameraEx.cancelTakePicture(); // TEST

        setIso(iso);
        setShutterSpeed(shutterSpeed[0], shutterSpeed[1]);
        setAp(ap);
        shutterDuration = round((double) 1000 * shutterSpeed[0] / shutterSpeed[1]);
        shootTime = System.currentTimeMillis(); // " @millis=" + shootTime +

        try {
            cameraEx.burstableTakePicture();
            //cameraEx.startDirectShutter();
            //cameraEx.startSelfTimerShutter();
            logshot(settings.magic_program[MagicPhase].name, "Shoot Photo EC#" + exposureCount + " RC#" + repeatCount + " SC#" + shotCount + " "
                    + String.valueOf(shutterSpeed[0]) + "/" + String.valueOf(shutterSpeed[1]) + "s ISO " + String.valueOf(iso) + " F" + String.valueOf(ap));
        } catch (Exception ignored) {
            shotErrorCount++;
            cameraEx.cancelTakePicture();
            log_exception("EXCEPTION in shoot: Camera.burstableTakePicture() * retry in 500ms * " + settings.magic_program[MagicPhase].name + " #" + exposureCount + "#" + repeatCount + " Shot#" + shotCount + " Err#" + shotErrorCount + " " + String.valueOf(shutterSpeed[0]) + "/" + String.valueOf(shutterSpeed[1]) + "s ISO " + String.valueOf(iso));
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

    @Override
    protected boolean onShutterKeyDown() {
        setDriveMode(settings.magic_program[MagicPhase].CameraFlags[exposureCount], settings.magic_program[MagicPhase].BurstDurations[exposureCount]);
        setIso(settings.magic_program[MagicPhase].ISOs[exposureCount]);
        setShutterSpeed(settings.magic_program[MagicPhase].ShutterSpeeds[exposureCount][0], settings.magic_program[MagicPhase].ShutterSpeeds[exposureCount][1]);
        try {
            cameraEx.cancelTakePicture(); // make sure nothing is stuck
            cameraEx.getNormalCamera().takePicture(null, null, null);
        } catch (Exception ignored) {
            log_exception("ERR: Manual Take Picture Failed");
            shootRunnableHandler.postDelayed(shootRunnable, 500);
        }
        log_debug("onShutterKeyDown: Drive Mode: " + settings.magic_program[MagicPhase].CameraFlags[exposureCount] + ", Manual Shot Fired");
        return true;
    }

    @Override
    protected boolean onShutterKeyUp() {
        cameraEx.cancelTakePicture();
        return true;
    }

    // When burst shooting this method is not called automatically
    // Therefore we called it every second manually
    @Override
    public void onShutter(int i, CameraEx cameraEx) {
        // i: 0 = success, 1 = canceled, 2 = error
        //log_debug(String.format("onShutter i: %d\n", i));
        if (shootEndTime == 0) {
            if (m_bracketActive)
                shootEndTime = 5*shutterDuration + System.currentTimeMillis();
            else
                shootEndTime = shutterDuration + System.currentTimeMillis();
        }
        log_debug("onShutter: ");
        if (i != 0) {
            //** this.cameraEx.cancelTakePicture();
            takingPicture = false;
            log_debug("onShutter ** take picture canceled or not ready -- delaying, retry shoot picture in 300ms, i: " + Integer.toString(i));
            shootPictureCallbackCallRunnableHandler.postDelayed(shootPictureCallbackCallRunnable, 300);
            return; // not ready/error
        }

        if (brck.get() < 0) {
            brck = new AtomicInteger(0);
            if (getcnt() > 1) {
                brck = new AtomicInteger(2);
            }
        }

        // Burst Shooting?
        if (burstShooting){
            shotCount++;
            if (endBurstShooting > System.currentTimeMillis()) {
                logshot(settings.magic_program[MagicPhase].name, Long.toString(endBurstShooting - System.currentTimeMillis()) + "ms left for Burst Shooting E#" + exposureCount + " R#" + repeatCount + " ~B#" + burstCount + "~ S#" + shotCount);
                burstCount++; // not actual burst count as I do not get the actual Shutter events
                if (settings.magic_program[MagicPhase].CameraFlags[exposureCount] == 'C')
                    manualShutterCallbackCallRunnableHandler.postDelayed(manualShutterCallbackCallRunnable, 200);
                else
                    manualShutterCallbackCallRunnableHandler.postDelayed(manualShutterCallbackCallRunnable, 500);
            } else {
                exposureCount++; // next
                burstCount = 0;
                burstShooting = false;
                this.cameraEx.cancelTakePicture();
                log_debug("onShutter: Burst completed. cancelTakePicture() now.");
                setDriveMode('S', 0);
                shootRunnableHandler.postDelayed(shootRunnable, 500); // continue manage program
            }
            return;
        }

        // shot completed?

        long remaining_exposure_time = System.currentTimeMillis() - shootEndTime;
        if (remaining_exposure_time < 0) {
            manualShutterCallbackCallRunnableHandler.postDelayed(manualShutterCallbackCallRunnable, -remaining_exposure_time); // wait for long exposure to complete
            log_debug("onShutter delaying for long exposure waitup, recall onShutter in " + Long.toString(-remaining_exposure_time) + "ms EC:" + Integer.toString(exposureCount));
            return;
        }
        shootEndTime = 0;

        if (m_bracketActive) {
            log_debug("onShutter: Bracketing Shooting completed.");
            m_bracketActive = false;
            m_bracketPicCount = 0;
            shotCount += m_bracketMaxPicCount;
        } else
            shotCount++;

        exposureCount++;

        // shot completed
        cameraEx.cancelTakePicture();

        log_debug("onShutter EC:" + Integer.toString(exposureCount) + ", past TakePicture ms: " + Long.toString(remaining_exposure_time));

        // end of exposure series?
        if (settings.magic_program[MagicPhase].number_shots > 0 && settings.magic_program[MagicPhase].ISOs[exposureCount] == 0) {
            int time_of_next_burst = settings.magic_program[MagicPhase].get_TimeOfNext(repeatCount + 1);
            long now = getMilliSecondsOfDay();
            // remaining time to the next shot
            long remainingTime = settings.magic_program[MagicPhase].get_remainingTimeToNext(repeatCount + 1, now);

            if (brck.get() > 0) {
                remainingTime = -1;
            }
            log_progress("onShutter: Remaining Time: " + getHMSMSfromMS(remainingTime));

            // if the remaining time is negative or short immediately start over and take the next picture
            if (remainingTime < 3000) {
                stopPicturePreview = true;
                shootRunnableHandler.post(shootRunnable);
            }
            // show the preview picture for some time
            else {
                log_debug("onShutter Preview ON");
                camera.startPreview();
                //long previewPictureShowTime = Math.round(Math.min(remainingTime, pictureReviewTime * 1000));
                reviewSurfaceView.setVisibility(View.VISIBLE);
                stopPicturePreview = false;
                shootRunnableHandler.postDelayed(shootRunnable, 1000); //previewPictureShowTime);
            }
        } else {
            //log_debug("onShutter repeat fast");
            stopPicturePreview = true;
            shootRunnableHandler.post(shootRunnable);
            //shootRunnableHandler.postDelayed(shootRunnable, 500);
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


    private void log_exception(String s) {
        Logger.error(s);
    }
    private void log_debug(String s) {
        Logger.info_debug(s);
    }
    private void log_info(String s) {
        Logger.info(s);
    }
    private void log_progress(String s) {
        Logger.info_progress(s);
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
