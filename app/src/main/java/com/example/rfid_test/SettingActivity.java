package com.example.rfid_test;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.warkiz.widget.IndicatorSeekBar;
import com.warkiz.widget.OnSeekChangeListener;
import com.warkiz.widget.SeekParams;
import com.zebra.rfid.api3.Antennas;
import com.zebra.rfid.api3.INVENTORY_STATE;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.RfidEventsListener;
import com.zebra.rfid.api3.RfidReadEvents;
import com.zebra.rfid.api3.RfidStatusEvents;
import com.zebra.rfid.api3.SESSION;
import com.zebra.rfid.api3.SL_FLAG;

public class SettingActivity extends AppCompatActivity {
    private static final String TAG = "Seetting";
    private EditText powerText;
    private Switch   hexToAscii;
    private Spinner Antennas;
    private String[] m;
    ArrayAdapter<String> adapterdoor;
    RFIDReader reader = MainActivity.reader;
    AsyncTask<Void, Void, Boolean> task = null;

    //
    private IndicatorSeekBar seekBarui;
    private String textpower;
    class Settings implements RfidEventsListener {

        @Override
        public void eventReadNotify(RfidReadEvents rfidReadEvents) {
        }

        @Override
        public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {

        }
    }
    String name = "S0";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        init();
        textpower = get_power();
        if (reader.getHostName().contains("MC33"))seekBarui.setMax(297);
        if (reader.getHostName().contains("RFD8500"))seekBarui.setMax(300);
        seekBarui.setProgress(Integer.valueOf(textpower));
        if (!textpower.equals("")) powerText.setText(textpower);
        if (get_Ascii().equals("true"))hexToAscii.setChecked(true);
        hexToAscii.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){
                    intoascii("true");
                }else {
                    intoascii("fase");
                }
            }
        });

        Antennas.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                 name =  adapterdoor.getItem(i).toString();
                intoAntennas(name);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        seekBarui();
        connect();
    }
    private void init() {
        powerText = (EditText) findViewById(R.id.PowerData);
        hexToAscii = (Switch) findViewById(R.id.Asciistart);
        Antennas = (Spinner) findViewById(R.id.Antennas);
        m = getResources().getStringArray(R.array.antennas);
        seekBarui =  findViewById(R.id.seek_barui);
        adapterdoor = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, m);
        //设置下拉列表的风格
        adapterdoor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //将adapter 添加到spinner中
        Antennas.setAdapter(adapterdoor);

    }
    private void seekBarui(){

        seekBarui.setOnSeekChangeListener( new OnSeekChangeListener() {


            @Override
            public void onSeeking(SeekParams seekParams) {

            }
            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar) {

               // Toast.makeText(SettingActivity.this,"ggg",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
               // Toast.makeText(SettingActivity.this,String.valueOf(seekBar.getProgress()),Toast.LENGTH_SHORT).show();
                powerText.setText(String.valueOf(seekBar.getProgress()));
            }



    });
    }

    public void connect() {
        if (task != null) task.cancel(true);
        Log.d(TAG, "Setting: ");
        task = new AsyncTask<Void,Void, Boolean>(){

            @Override
            protected Boolean doInBackground(Void... voids) {
                if (isCancelled()) return false;
                if (reader == null) return false;
                if (!reader.isConnected()) return false;
                boolean success = false;

                return success;
            }
        };

    }
    public String get_power(){
        SharedPreferences sp = getSharedPreferences("power",MODE_PRIVATE);
        String power = sp.getString("power","");
        return power;
    }
    public void intoPower(String pdaid){
        SharedPreferences.Editor editor = getSharedPreferences("power",MODE_PRIVATE).edit();
        editor.clear();
        editor.putString("power",pdaid);
        editor.commit();
    }
    public String get_Ascii(){
        SharedPreferences sp = getSharedPreferences("ascii",MODE_PRIVATE);
        return sp.getString("ascii","");
    }
    public void intoascii(String Ascii){
        SharedPreferences.Editor editor = getSharedPreferences("ascii",MODE_PRIVATE).edit();
        editor.clear();
        editor.putString("ascii",Ascii);
        editor.commit();
    }
    public String get_Antennas(){
        SharedPreferences sp = getSharedPreferences("antennas",MODE_PRIVATE);
        return sp.getString("antennas","");
    }
    public void intoAntennas(String Antennas){
        SharedPreferences.Editor editor = getSharedPreferences("antennas",MODE_PRIVATE).edit();
        editor.clear();
        editor.putString("antennas",Antennas);
        editor.commit();
    }
    private void alertDialog() {
        com.zebra.rfid.api3.Antennas.AntennaRfConfig config = null;
        String yu = powerText.getText().toString();
        intoPower(yu);
        try {
            config = reader.Config.Antennas.getAntennaRfConfig(1);
            config.setTransmitPowerIndex(Integer.decode(yu));
            config.setrfModeTableIndex(0);
            config.setTari(0);
            reader.Config.Antennas.setAntennaRfConfig(1, config);

            Antennas.SingulationControl control = reader.Config.Antennas.getSingulationControl(1);
            switch(name){
                case "S0":
                    control.setSession(SESSION.SESSION_S0);
                    control.Action.getInventoryState();
                    control.Action.setSLFlag(SL_FLAG.SL_ALL);
                    control.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
                    Toast.makeText(getApplicationContext(),"S0",Toast.LENGTH_SHORT).show();
                    break;
                case "S1":
                    control.setSession(SESSION.SESSION_S1);
                    Toast.makeText(getApplicationContext(),"S1",Toast.LENGTH_SHORT).show();
                    break;
                case "S2":
                    control.setSession(SESSION.SESSION_S2);
                    Toast.makeText(getApplicationContext(),"S2",Toast.LENGTH_SHORT).show();
                    break;
                case "S3":
                    control.setSession(SESSION.SESSION_S3);
                    Toast.makeText(getApplicationContext(),"S3",Toast.LENGTH_SHORT).show();
                    break;
            }
            reader.Config.Antennas.setSingulationControl(1, control);
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        alertDialog();
        finish();
    }
}