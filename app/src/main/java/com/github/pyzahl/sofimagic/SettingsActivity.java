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

import com.github.ma1co.pmcademo.app.BaseActivity;

public class SettingsActivity extends BaseActivity
{
    private SettingsActivity that = this;

    private Settings settings;

    private SoFiProgramXML soFiProgramXML;

    private TabHost tabHost;

    private Button bnStart, bnClose;

    private EditText edTC1;
    private EditText edTC2;
    private EditText edTC3;
    private EditText edTC4;


    private TextView tvPhaseName, tvPhaseRStart, tvPhaseStart, tvPhaseREnd, tvREnd, tvNumShots, tvCFlags, tvISOs, tvFs, tvShutterSpeeds;

    private AdvancedSeekBar sbPhase;
    private int phase_index=0;
    private TextView tvPhaseIndex;

    private CheckBox cbSilentShutter;
    private CheckBox cbBRS;
    private CheckBox cbMF;
    private CheckBox cbDOFF;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (!(Thread.getDefaultUncaughtExceptionHandler() instanceof CustomExceptionHandler))
            Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler());

        Logger.info("SoFiMagic startup: Hello Eclipse");

        settings = new Settings();

        soFiProgramXML = new SoFiProgramXML(); // check/create XML for default program updates
        //soFiProgramXML.SoFiProgramStoreXML();

        settings.load(this);

        bnStart = (Button) findViewById(R.id.bnStart);
        bnStart.setOnClickListener(bnStartOnClickListener);

        bnClose = (Button) findViewById(R.id.bnClose);
        bnClose.setOnClickListener(bnCloseOnClickListener);

        edTC1 = (EditText) findViewById(R.id.editTextTimeC1);
        edTC2 = (EditText) findViewById(R.id.editTextTimeC2);
        edTC3 = (EditText) findViewById(R.id.editTextTimeC3);
        edTC4 = (EditText) findViewById(R.id.editTextTimeC4);

        sbPhase = (AdvancedSeekBar) findViewById(R.id.sbPhase);
        tvPhaseIndex = (TextView) findViewById(R.id.tvPhaseId);


        tvPhaseName = (TextView) findViewById(R.id.tvPhase);
        tvPhaseRStart = (TextView) findViewById(R.id.tvStartRef);
        tvPhaseStart = (TextView) findViewById(R.id.tvStart);
        tvPhaseREnd = (TextView) findViewById(R.id.tvEndRef);
        tvREnd = (TextView) findViewById(R.id.tvEnd);
        tvNumShots = (TextView) findViewById(R.id.tvNumberShots);
        tvCFlags = (TextView) findViewById(R.id.tvFlagList);
        tvISOs = (TextView) findViewById(R.id.tvISOList);
        tvFs = (TextView) findViewById(R.id.tvFList);
        tvShutterSpeeds = (TextView) findViewById(R.id.tvShutterSpeedList);


        cbSilentShutter = (CheckBox) findViewById(R.id.cbSilentShutter);
        cbBRS  = (CheckBox) findViewById(R.id.cbBRC);
        cbMF   = (CheckBox) findViewById(R.id.cbMF);
        cbDOFF = (CheckBox) findViewById(R.id.cbDOFF);


        cbSilentShutter.setChecked(settings.silentShutter);
        cbSilentShutter.setOnCheckedChangeListener(cbSilentShutterOnCheckListener);
        //cbSilentShutter.setVisibility(View.INVISIBLE);

        cbBRS.setChecked(settings.brs);
        cbBRS.setOnCheckedChangeListener(cbBRSOnCheckListener);

        cbMF.setChecked(settings.mf);
        cbMF.setOnCheckedChangeListener(cbMFOnCheckListener);

        cbDOFF.setChecked(settings.displayOff);
        cbDOFF.setOnCheckedChangeListener(cbDOFFOnCheckListener);

        edTC1.setText(String.format("%02d:%02d:%02d", settings.tc1 / 3600, (settings.tc1 / 60) % 60, settings.tc1 % 60));
        edTC2.setText(String.format("%02d:%02d:%02d", settings.tc2 / 3600, (settings.tc2 / 60) % 60, settings.tc2 % 60));
        edTC3.setText(String.format("%02d:%02d:%02d", settings.tc3 / 3600, (settings.tc3 / 60) % 60, settings.tc3 % 60));
        edTC4.setText(String.format("%02d:%02d:%02d", settings.tc4 / 3600, (settings.tc4 / 60) % 60, settings.tc4 % 60));

        sbPhase.setMax(16);
        sbPhase.setOnSeekBarChangeListener(sbPhaseOnSeekBarChangeListener);
        sbPhase.setProgress(0);
        sbPhaseOnSeekBarChangeListener.onProgressChanged(sbPhase, 0, false);

        //updatePhase(0);

        //try {
            //CameraEx cameraEx = CameraEx.open(0, null);
            //final CameraEx.ParametersModifier modifier = cameraEx.createParametersModifier(cameraEx.getNormalCamera().getParameters());
            //if(modifier.isSupportedSilentShutterMode())
            //    cbSilentShutter.setVisibility(View.VISIBLE);
        /*}
        catch(Exception ignored)
        {}*/
    }

    View.OnClickListener bnStartOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            settings.save(that);
            Intent intent = new Intent(that, ShootActivity.class);
            settings.putInIntent(intent);
            startActivity(intent);
        }
    },
    bnCloseOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
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
            tvPhaseName.setText(settings.magic_program[phase].name);
            tvPhaseRStart.setText(Integer.toString(settings.magic_program[phase].ref_contact_start));
            tvPhaseStart.setText(Integer.toString(settings.magic_program[phase].start_time));
            tvPhaseREnd.setText(Integer.toString(settings.magic_program[phase].ref_contact_end));
            tvREnd.setText(Integer.toString(settings.magic_program[phase].end_time));
            tvNumShots.setText(Integer.toString(settings.magic_program[phase].number_shots));
            tvCFlags.setText(settings.magic_program[phase].CameraFlags);
            tvISOs.setText(settings.magic_program[phase].get_ISOs_list());
            tvFs.setText(settings.magic_program[phase].get_Fs_list());
            tvShutterSpeeds.setText(settings.magic_program[phase].get_ShutterSpeeds_list());
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
