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
    private long delay_to_next_burst = 0;

    private long remainingTimeToContactPhase = 0;
    private long remainingTimeThisPhase = 0;
    private long remainingTimeNextBurst = 0;

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
            log("shootRunnable " + getHMSMSfromMS(now) + " MagicPhase[" + Integer.toString(MagicPhase) + "]" + settings.magic_program[MagicPhase].name
                    + " SC:" + Integer.toString(shotCount) + " EC:" + Integer.toString(exposureCount) + " RC:" + Integer.toString(repeatCount) + " BC: " + Integer.toString(burstCount)
                    + " ** CF=" + settings.magic_program[MagicPhase].CameraFlags[exposureCount][0]
                    + ": " + Integer.toString(settings.magic_program[MagicPhase].ShutterSpeeds[exposureCount][0])
                    + "/" + Integer.toString(settings.magic_program[MagicPhase].ShutterSpeeds[exposureCount][1])
                    + " @ISO " + Integer.toString(settings.magic_program[MagicPhase].ISOs[exposureCount]));

            // Check in MagicPhase, adjust if needed:
            // if aborted, it will continue at the right phase automatically and skips forward as required!
            do {
                remainingTimeToContactPhase = settings.magic_program[MagicPhase].get_remainingTimeToStart(now);
                remainingTimeThisPhase = settings.magic_program[MagicPhase].get_remainingTime(now);
                log("shootRunnable: remaining time to MagicPhase " + settings.magic_program[MagicPhase].name + " @" + getHMSMSfromMS((long) settings.magic_program[MagicPhase].get_start_time() * 1000) + " #" + Integer.toString(MagicPhase) + " start in: " + getHMSfromMS(remainingTimeToContactPhase));
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
                    remainingTimeNextBurst = settings.magic_program[MagicPhase].get_remainingTimeToNext(repeatCount+1, now);
                    if (remainingTimeNextBurst < 0) { // past that shot?
                        log("shootRunnable: skipping to repeat count " + Integer.toString(repeatCount));
                        exposureCount = 0;
                        repeatCount++; // skip
                    }
                } while (remainingTimeNextBurst < 0 && settings.magic_program[MagicPhase].ISOs[exposureCount] != 0);
            }

            // CHECK FOR NOT END OF ECLIPSE TO PROCEED
            if (settings.magic_program[MagicPhase].number_shots != 0) {

                // Check: Contineous Drive ? (Burst Operation)
                if (remainingTimeToContactPhase < 150
                        && settings.magic_program[MagicPhase].CameraFlags[exposureCount][0]=='C' // Burst (Contineous Mode?)
                        && settings.magic_program[MagicPhase].number_shots > 1            // more than 1 shot?
                        && settings.magic_program[MagicPhase].ISOs[exposureCount] != 0){  // not at end of exposure list?
                    log("shootRunnable: BurstMode B#" + Integer.toString(burstCount));

                    if (burstCount == 0) // Before 1 Burst Shot setup Burst Mode
                        setDriveMode(settings.magic_program[MagicPhase].CameraFlags[exposureCount][0]);

                    if (burstCount < settings.magic_program[MagicPhase].BurstCounts[exposureCount]) {
                        burstCount++;
                        int tmp = shotCount;
                        shoot(settings.magic_program[MagicPhase].ISOs[exposureCount], settings.magic_program[MagicPhase].ShutterSpeeds[exposureCount]);
                        if (tmp == shotCount) { // Shot failed
                            burstCount--;
                        } else
                            repeatCount++;
                        return; // DONE here, keep bursting
                    } else {
                        exposureCount++;
                        setDriveMode('S');
                        burstCount = 0;
                        shootRunnableHandler.postDelayed(this, 1000); // give some time to store and repeat
                        return; // DONE with this burst
                    }
                } else {
                    if (burstCount > 0) { // end section, reset DriveMode
                        setDriveMode('S');
                        burstCount = 0;
                    }
                }

                if (settings.magic_program[MagicPhase].number_shots == -1)
                    remainingTimeNextBurst = 0;

                if (remainingTimeNextBurst > 150
                        || (settings.magic_program[MagicPhase].number_shots > 0 && settings.magic_program[MagicPhase].ISOs[exposureCount] == 0)) { // end of exposure list and distributed shots -- else keep going and repeat exposure block

                    if (settings.magic_program[MagicPhase].ISOs[exposureCount] == 0) {
                        log("shootRunnable: exposure list completed, repeating.");
                        exposureCount = 0; // reset exposure count for phase and repeat exposure block
                        repeatCount++;
                    }
                    int time_of_next_burst = settings.magic_program[MagicPhase].get_TimeOfNext(repeatCount);
                    now = getMilliSecondsOfDay();
                    remainingTimeNextBurst = settings.magic_program[MagicPhase].get_remainingTimeToNext(repeatCount, now);
                    log("shootRunnable: remaining time to next Exposure Series in " + settings.magic_program[MagicPhase].name + " ##" + Integer.toString(repeatCount) + " next in: " + getHMSMSfromMS(remainingTimeNextBurst));

                    if (remainingTimeNextBurst < 150) {
                        int tmp = shotCount;
                        shoot(settings.magic_program[MagicPhase].ISOs[exposureCount], settings.magic_program[MagicPhase].ShutterSpeeds[exposureCount]);
                        if (shotCount > tmp) // Shot OK
                            exposureCount++;
                    } else {
                        // ...wait for Contact Start Time
                        log("shootRunnable: waiting for next exposue block...");
                        if (remainingTimeNextBurst > 1500) {
                            display.on();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    update_info();
                                }
                            });
                            shootRunnableHandler.postDelayed(this, 1000); // wait a second and check again, update screen
                        } else {
                            if (remainingTimeNextBurst > 250)
                                shootRunnableHandler.postDelayed(this, remainingTimeNextBurst - 250); // getting close, wait a little and check again
                            else
                                shootRunnableHandler.postDelayed(this, 250); // getting close, wait a little and check again
                        }
                    }
                    return; // DONE

                } else { // keep shooting!
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
                            exposureCount++;
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
        tvCount.setText(Integer.toString(shotCount) + ":" + Integer.toString(repeatCount) + ":" + Integer.toString(exposureCount) + "/" + Integer.toString(settings.magic_program[MagicPhase].number_shots));
        tvRemaining.setText(getHMSfromMS(remainingTimeNextBurst));
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
        if (settings.magic_program[MagicPhase].CameraFlags[exposureCount][0]=='C'){ // Starting with Burst? (Contineous Mode?)
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
        log("set DriveMode " + CFlag);
        final Camera.Parameters params = cameraEx.getNormalCamera().getParameters();
        final CameraEx.ParametersModifier paramsModifier = cameraEx.createParametersModifier(params);
        switch (CFlag){ // DriveMode Burst
            case 'C':
                try {
                    log("Setting DriveMode " + CFlag);
                    this.cameraEx.cancelTakePicture();
                    paramsModifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_BURST);
                    List driveSpeeds = paramsModifier.getSupportedBurstDriveSpeeds();
                    //paramsModifier.setBurstDriveSpeed(driveSpeeds.get(driveSpeeds.size() - 1).toString()); // Speed: 2-1=1 => high
                    paramsModifier.setBurstDriveSpeed(driveSpeeds.get(driveSpeeds.size() - 2).toString()); // Speed: 2-2=0 => low
                    log("INFO: set DriveMode Burst Speed: " + Integer.toString(driveSpeeds.size()-2) + " => " + driveSpeeds.get(driveSpeeds.size() - 2).toString());
                    paramsModifier.setBurstDriveButtonReleaseBehave(CameraEx.ParametersModifier.BURST_DRIVE_BUTTON_RELEASE_BEHAVE_CONTINUE);
                } catch (Exception ignored) {
                    log("EXCEPTION set DriveMode " + CFlag);
                }
                break;
            case 'B':
                log("Not yet supported: DriveMode " + CFlag);
                paramsModifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_SINGLE);
                break;
            case 'S':
                log("Setting DriveMode " + CFlag);
                paramsModifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_SINGLE);
                break;
        }
    }

private void shoot(int iso, int[] shutterSpeed) {
        if(takingPicture)
            return;

        if (burstCount <= 1) { // only at initial burst shot and always otherwise
            setIso(iso);
            setShutterSpeed(shutterSpeed[0], shutterSpeed[1]);
            logshot("Shoot Photo " + settings.magic_program[MagicPhase].name + " #" + exposureCount + "#" + repeatCount + " " +  String.valueOf(shutterSpeed[0]) +"/" + String.valueOf(shutterSpeed[1]) + "s ISO " + String.valueOf(iso));
        } else
            logshot(Long.toString(System.currentTimeMillis()) + "ms: BurstShoot Photo " + settings.magic_program[MagicPhase].name + " #" + exposureCount + "#" + repeatCount + " Burst#" + burstCount + " Shot#" + shotCount);

        try {
            shootTime = System.currentTimeMillis(); // " @millis=" + shootTime +
            cameraEx.burstableTakePicture();
            shootEndTime = shootTime+Math.round((double)1000*shutterSpeed[0]/shutterSpeed[1])+150;
            shotCount++;
        } catch (Exception ignored) {
            this.cameraEx.cancelTakePicture();
            log("EXCEPTION Camera.burstableTakePicture() failed #" + Integer.toString(shotCount));
            log("EXCEPTION trying to continue after 2s");
            shootRunnableHandler.postDelayed(shootRunnable, 2000);
        }

        if (burstCount == 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    update_info();
                }
            });
        }
    }

    private AtomicInteger brck = new AtomicInteger(0);


    // When burst shooting this method is not called automatically
    // Therefore we called it every second manually
    @Override
    public void onShutter(int i, CameraEx cameraEx) {
        log("onShutter [" + Integer.toString(i) + "] -- past Shutter ms: " +  Long.toString( System.currentTimeMillis() - shootEndTime));

        if(brck.get()<0){
            brck = new AtomicInteger(0);
            if(getcnt()>1) {
                brck = new AtomicInteger(2);
            }
        }

        // Burst Shooting?
        if (settings.magic_program[MagicPhase].CameraFlags[exposureCount][0]=='C') {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    update_info();
                }
            });
            if (burstCount++ < settings.magic_program[MagicPhase].BurstCounts[exposureCount]) {
                logshot(Long.toString(System.currentTimeMillis()) + "ms: BurstShoot Photo " + settings.magic_program[MagicPhase].name + " #" + exposureCount + "#" + repeatCount + " Burst#" + burstCount + " Shot#" + shotCount);
                manualShutterCallbackCallRunnableHandler.postDelayed(manualShutterCallbackCallRunnable, 333);
            } else {
                this.cameraEx.cancelTakePicture();
                log("onShutter: Burst completed. cancelTakePicture()");
                shootRunnableHandler.postDelayed(shootRunnable, 500);
            }
            return;
        }

        if (settings.magic_program[MagicPhase].number_shots > 0 && settings.magic_program[MagicPhase].ISOs[exposureCount] == 0) {
            log("onShutter: exposure list completed.");
            int time_of_next_burst = settings.magic_program[MagicPhase].get_TimeOfNext(repeatCount + 1);
            long now = getMilliSecondsOfDay();
            // remaining time to the next shot
            double remainingTime = settings.magic_program[MagicPhase].get_remainingTimeToNext(repeatCount + 1, now);

            if (brck.get() > 0) {
                remainingTime = -1;
            }
            log("Remaining Time: " + remainingTime);

            // if the remaining time is negative immediately take the next picture
            if (remainingTime < 1500) {
                stopPicturePreview = true;
                shootRunnableHandler.post(shootRunnable);
            }
            // show the preview picture for some time
            else {
                log("onShutter Pic Review Time");
                camera.startPreview();
                long previewPictureShowTime = Math.round(Math.min(remainingTime, pictureReviewTime * 1000));
                log("  Stop preview in: " + previewPictureShowTime);
                reviewSurfaceView.setVisibility(View.VISIBLE);
                stopPicturePreview = false;
                shootRunnableHandler.postDelayed(shootRunnable, 1000); //previewPictureShowTime);
            }
        } else {
            log("onShutter repeat fast");
            stopPicturePreview = true;
            shootRunnableHandler.postDelayed(shootRunnable, 1000);
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
