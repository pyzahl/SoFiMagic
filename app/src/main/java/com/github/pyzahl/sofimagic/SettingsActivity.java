package com.github.pyzahl.sofimagic;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;

import android.text.TextWatcher;

import com.github.ma1co.pmcademo.app.BaseActivity;

public class SettingsActivity extends BaseActivity
{
    private SettingsActivity that = this;

    private Settings settings;

    private SoFiProgramXML soFiProgramXML;

    private TabHost tabHost;

    private Button bnStart, bnClose;

    private HHMMSSEntry edTC1;
    private HHMMSSEntry edTC2;
    private HHMMSSEntry edTC3;
    private HHMMSSEntry edTC4;

    private AdvancedSeekBar sbPhase;
    private int phase_index=0;
    private TextView tvPhaseIndex;

    private int phase_loaded=-1;

    private TextView tvPhaseName;
    private IndexEntry tvPhaseRStart, tvPhaseStart, tvPhaseREnd, tvPhaseEnd, tvNumShots;

    private TableLayout exposureParamTable;

    private ListEntry exposureFlags[];
    private ListEntry BurstDurations[];
    private ListEntry exposureISOs[];
    private ListEntry exposureFs[];
    private ListEntry exposureShutters[];

    private CheckBox cbSilentShutter;
    //private CheckBox cbBRS;
    private CheckBox cbMF;
    private CheckBox cbDOFF;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        phase_loaded = -1;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (!(Thread.getDefaultUncaughtExceptionHandler() instanceof CustomExceptionHandler))
            Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler());

        Logger.info("SoFiMagic startup: Hello Eclipse");

        settings = new Settings();

        soFiProgramXML = new SoFiProgramXML(); // check/create XML for default program updates

        settings.load(this);

        bnStart = (Button) findViewById(R.id.bnStart);
        bnStart.setOnClickListener(bnStartOnClickListener);

        bnClose = (Button) findViewById(R.id.bnClose);
        bnClose.setOnClickListener(bnCloseOnClickListener);

        edTC1 = (HHMMSSEntry) findViewById(R.id.editTextTimeC1);
        edTC2 = (HHMMSSEntry) findViewById(R.id.editTextTimeC2);
        edTC3 = (HHMMSSEntry) findViewById(R.id.editTextTimeC3);
        edTC4 = (HHMMSSEntry) findViewById(R.id.editTextTimeC4);

        tvPhaseRStart = (IndexEntry) findViewById(R.id.tvStartRef);
        tvPhaseRStart.setRange(0, 4);
        tvPhaseRStart.setPrefix("CT");

        tvPhaseStart = (IndexEntry) findViewById(R.id.tvStart);
        tvPhaseStart.setRange(-60, 60);
        tvPhaseStart.setUnit("s");

        tvPhaseREnd = (IndexEntry) findViewById(R.id.tvEndRef);
        tvPhaseREnd.setRange(0, 4);
        tvPhaseREnd.setPrefix("CT");

        tvPhaseEnd = (IndexEntry) findViewById(R.id.tvEnd);
        tvPhaseEnd.setRange(-60, 60);
        tvPhaseEnd.setUnit("s");

        tvNumShots = (IndexEntry) findViewById(R.id.tvNumberShots);
        tvNumShots.setRange(-1, 9999);
        tvNumShots.setPrefix("#");

        TableRow trFlags       = (TableRow)findViewById(R.id.rowFlagsList);
        TableRow trBurstDurations = (TableRow)findViewById(R.id.rowBurstCount);
        TableRow trFs          = (TableRow)findViewById(R.id.rowFList);
        TableRow trISOs        = (TableRow)findViewById(R.id.rowISOsList);
        TableRow trShutters    = (TableRow)findViewById(R.id.rowShutterSpeedsList);

        exposureFlags    = new ListEntry[Settings.MAX_EXPOSURE_PARAMS];
        BurstDurations      = new ListEntry[Settings.MAX_EXPOSURE_PARAMS];
        exposureISOs     = new ListEntry[Settings.MAX_EXPOSURE_PARAMS];
        exposureFs       = new ListEntry[Settings.MAX_EXPOSURE_PARAMS];
        exposureShutters = new ListEntry[Settings.MAX_EXPOSURE_PARAMS];

        for (int k=0; k<Settings.MAX_EXPOSURE_PARAMS; ++k){
            exposureFlags[k] = new ListEntry(getApplicationContext());
            exposureFlags[k].setMaxIndex(CameraUtilISOs.CFlags.length-1);
            exposureFlags[k].setLookup(CameraUtilISOs.getCFString.instance);
            trFlags.addView(exposureFlags[k]);

            BurstDurations[k] = new ListEntry(getApplicationContext());
            BurstDurations[k].setMaxIndex(99);
            BurstDurations[k].setLookup(CameraUtilISOs.getBurstDurationstring.instance);
            trBurstDurations.addView(BurstDurations[k]);

            exposureISOs[k] = new ListEntry(getApplicationContext());
            exposureISOs[k].setMaxIndex(CameraUtilISOs.ISOs.length-1);
            exposureISOs[k].setLookup(CameraUtilISOs.getISOString.instance);
            trISOs.addView(exposureISOs[k]);

            exposureFs[k] = new ListEntry(getApplicationContext());
            exposureFs[k].setMaxIndex(CameraUtilISOs.Apertures.length-1);
            exposureFs[k].setLookup(CameraUtilISOs.getFString.instance);
            trFs.addView(exposureFs[k]);

            exposureShutters[k] = new ListEntry(getApplicationContext());
            exposureShutters[k].setMaxIndex(CameraUtilShutterSpeed.SHUTTER_SPEEDS.length-1);
            exposureShutters[k].setLookup(CameraUtilShutterSpeed.getShutterSpeedString.instance);
            trShutters.addView(exposureShutters[k]);
        }

        cbSilentShutter = (CheckBox) findViewById(R.id.cbSilentShutter);
        cbMF   = (CheckBox) findViewById(R.id.cbMF);
        cbDOFF = (CheckBox) findViewById(R.id.cbDOFF);


        cbSilentShutter.setChecked(settings.silentShutter);
        cbSilentShutter.setOnCheckedChangeListener(cbSilentShutterOnCheckListener);
        //cbSilentShutter.setVisibility(View.INVISIBLE);

        //cbBRS.setChecked(settings.brs);
        //cbBRS.setOnCheckedChangeListener(cbBRSOnCheckListener);

        cbMF.setChecked(settings.mf);
        cbMF.setOnCheckedChangeListener(cbMFOnCheckListener);

        cbDOFF.setChecked(settings.displayOff);
        cbDOFF.setOnCheckedChangeListener(cbDOFFOnCheckListener);

        edTC1.setSec(settings.tc1);
        edTC2.setSec(settings.tc2);
        edTC3.setSec(settings.tc3);
        edTC4.setSec(settings.tc4);
        // SetText(String.format("%02d:%02d:%02d", settings.tc4 / 3600, (settings.tc4 / 60) % 60, settings.tc4 % 60));

        sbPhase = (AdvancedSeekBar) findViewById(R.id.sbPhase);
        tvPhaseIndex = (TextView) findViewById(R.id.tvPhaseId);
        tvPhaseName = (TextView) findViewById(R.id.tvPhase);

        // check for end
        int last_phase=1;
        for (; settings.magic_program[last_phase].number_shots != 0 && last_phase < (settings.magic_program.length-1); ++last_phase);
        sbPhase.setMax(last_phase);
        sbPhase.setOnSeekBarChangeListener(sbPhaseOnSeekBarChangeListener);
        sbPhase.setProgress(0);
        sbPhaseOnSeekBarChangeListener.onProgressChanged(sbPhase, 0, false);

        //try {
            //CameraEx cameraEx = CameraEx.open(0, null);
            //final CameraEx.ParametersModifier modifier = cameraEx.createParametersModifier(cameraEx.getNormalCamera().getParameters());
            //if(modifier.isSupportedSilentShutterMode())
            //    cbSilentShutter.setVisibility(View.VISIBLE);
        /*}
        catch(Exception ignored)
        {}*/
    }


    public void update_and_store_xml() {
        if (phase_loaded >= 0) {
            settings.tc1 = edTC1.getSec();
            settings.tc2 = edTC2.getSec();
            settings.tc3 = edTC3.getSec();
            settings.tc4 = edTC4.getSec();
            getPhase(phase_loaded);
            soFiProgramXML.SoFiProgramStoreXML();
        }
    }

    View.OnClickListener bnStartOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            update_and_store_xml();
            settings.save(that);
            Intent intent = new Intent(that, ShootActivity.class);
            settings.putInIntent(intent);
            startActivity(intent);
        }
    },
    bnCloseOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            update_and_store_xml();
            settings.save(that);
            finish();
        }
    };

    SeekBar.OnSeekBarChangeListener sbPhaseOnSeekBarChangeListener
            = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
            phase_index = i;
            if (phase_index < 0) phase_index=0;
            if (phase_index > 16) phase_index=16;
            updatePhase(phase_index);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    };



    CheckBox.OnCheckedChangeListener cbSilentShutterOnCheckListener = new CheckBox.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            settings.silentShutter = b;
        }
    };


    CheckBox.OnCheckedChangeListener cbBRSOnCheckListener = new CheckBox.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            settings.brs = b;
        }
    };

    CheckBox.OnCheckedChangeListener cbMFOnCheckListener = new CheckBox.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            settings.mf = b;
        }
    };

    CheckBox.OnCheckedChangeListener cbDOFFOnCheckListener = new CheckBox.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            settings.displayOff = b;
        }
    };

    void updatePhase(int phase) {
        tvPhaseIndex.setText(Integer.toString(phase));
        if (phase >= 0 && phase < settings.magic_program.length) {

            if (phase_loaded >= 0)
                getPhase(phase_loaded);

            tvPhaseName.setText(settings.magic_program[phase].name);

            tvPhaseRStart.setIndex(settings.magic_program[phase].ref_contact_start);
            tvPhaseStart.setIndex(settings.magic_program[phase].start_time);
            tvPhaseREnd.setIndex(settings.magic_program[phase].ref_contact_end);
            tvPhaseEnd.setIndex(settings.magic_program[phase].end_time);

            tvNumShots.setIndex(settings.magic_program[phase].number_shots);

            for (int k=0; k<settings.magic_program[phase].ISOs.length; ++k) {
                if (settings.magic_program[phase].ISOs[k] > 0) {
                    exposureISOs[k].setIndex(CameraUtilISOs.getISOIndex(settings.magic_program[phase].ISOs[k]));
                    exposureFlags[k].setIndex(CameraUtilISOs.getCFlagIndex(settings.magic_program[phase].CameraFlags[k]));
                    BurstDurations[k].setIndex(settings.magic_program[phase].BurstDurations[k]);
                    exposureFs[k].setIndex(CameraUtilISOs.getFIndex(settings.magic_program[phase].Fs[k]));
                    exposureShutters[k].setIndex(CameraUtilShutterSpeed.getShutterValueIndex(settings.magic_program[phase].ShutterSpeeds[k][0], settings.magic_program[phase].ShutterSpeeds[k][1]));
                } else {
                    exposureISOs[k].setIndex(-1);
                    exposureFlags[k].setIndex(-1);
                    BurstDurations[k].setIndex(-1);
                    exposureFs[k].setIndex(-1);
                    exposureShutters[k].setIndex(-1);
                }
            }
            phase_loaded = phase;
        }
    }

    void getPhase(int phase) {
        if (phase >= 0 && phase < settings.magic_program.length) {
            settings.magic_program[phase].ref_contact_start = tvPhaseRStart.getIndex();
            settings.magic_program[phase].start_time = tvPhaseStart.getIndex();
            settings.magic_program[phase].ref_contact_end = tvPhaseREnd.getIndex();
            settings.magic_program[phase].end_time = tvPhaseEnd.getIndex();

            settings.magic_program[phase].number_shots = tvNumShots.getIndex();

            for (int k=0; k<settings.magic_program[phase].ISOs.length; ++k) {
                int iso=CameraUtilISOs.ISOs[exposureISOs[k].getIndex()];
                if (iso > 0) {
                    settings.magic_program[phase].ISOs[k] = CameraUtilISOs.ISOs[exposureISOs[k].getIndex()];
                    settings.magic_program[phase].BurstDurations[k] = BurstDurations[k].getIndex();
                    settings.magic_program[phase].CameraFlags[k] = CameraUtilISOs.CFlags[exposureFlags[k].getIndex()].toCharArray()[0];
                    int i = exposureFs[k].getIndex();
                    settings.magic_program[phase].Fs[k] = CameraUtilISOs.Apertures[i>=0?i:0];
                    settings.magic_program[phase].ShutterSpeeds[k][0] = CameraUtilShutterSpeed.SHUTTER_SPEEDS[exposureShutters[k].getIndex()][0];
                    settings.magic_program[phase].ShutterSpeeds[k][1] = CameraUtilShutterSpeed.SHUTTER_SPEEDS[exposureShutters[k].getIndex()][1];
                } else {
                    settings.magic_program[phase].ISOs[k] = CameraUtilISOs.ISOs[0];
                    settings.magic_program[phase].BurstDurations[k] = 0;
                    settings.magic_program[phase].CameraFlags[k] = CameraUtilISOs.CFlags[0].toCharArray()[0];
                    settings.magic_program[phase].Fs[k] = CameraUtilISOs.ISOs[0];
                    settings.magic_program[phase].ShutterSpeeds[k][0] = CameraUtilShutterSpeed.SHUTTER_SPEEDS[0][0];
                    settings.magic_program[phase].ShutterSpeeds[k][1] = CameraUtilShutterSpeed.SHUTTER_SPEEDS[0][1];
                }
            }
        }
    }



    protected boolean onUpperDialChanged(int value) {
        sbPhase.dialChanged(value);
        return true;
    }

    protected boolean onLowerDialChanged(int value) {
        sbPhase.dialChanged(value);
        return true;
    }

    protected boolean onThirdDialChanged(int value) {
        sbPhase.dialChanged(value);
        return true;
    }

    protected boolean onKuruDialChanged(int value) {
        sbPhase.dialChanged(value);
        return true;
    }

    @Override
    protected boolean onMenuKeyUp()
    {
        onBackPressed();
        return true;
    }
}
