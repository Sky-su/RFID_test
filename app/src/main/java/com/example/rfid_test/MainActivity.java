package com.example.rfid_test;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.zebra.rfid.api3.Antennas;
import com.zebra.rfid.api3.BEEPER_VOLUME;
import com.zebra.rfid.api3.ENUM_TRANSPORT;
import com.zebra.rfid.api3.ENUM_TRIGGER_MODE;
import com.zebra.rfid.api3.Events;
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.ReaderDevice;
import com.zebra.rfid.api3.Readers;
import com.zebra.rfid.api3.RegionInfo;
import com.zebra.rfid.api3.RegulatoryConfig;
import com.zebra.rfid.api3.RfidEventsListener;
import com.zebra.rfid.api3.RfidReadEvents;
import com.zebra.rfid.api3.RfidStatusEvents;
import com.zebra.rfid.api3.SESSION;
import com.zebra.rfid.api3.START_TRIGGER_TYPE;
import com.zebra.rfid.api3.STATUS_EVENT_TYPE;
import com.zebra.rfid.api3.STOP_TRIGGER_TYPE;
import com.zebra.rfid.api3.TagData;
import com.zebra.rfid.api3.TriggerInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 *  APP Mode in SKY!
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    AsyncTask<Void, String, String> task = null;
    AsyncTask<Void, Void, Void> taskPerform = null;
    AsyncTask<Void, Void, Void> taskStop = null;
    Readers readers = null;
    ReaderDevice device = null;
    static RFIDReader reader = null;
    private List<String> data ;
    ScheduledExecutorService scheduler = null;
    ScheduledFuture<?> taskHandler = null;
    boolean isStarted = false;
    static RfidEventHandler eventHandler = null;
    int battery = 0;
    int temperature = 0;
  //UI控件
    Button buttonLoad;
    Button buttonSave;
    Button buttonStart;
    TextView textViewStatus;
    TextView textViewlocalRead;
    ListView listView;
    public TextView statusTextViewRFID = null;
    List<RfidView> list = new ArrayList<>();
    private SoundPoolHelper soundPoolHelper;
    private LayList layList;
    ProgressDialog progressDialog = null;
    private String power;
    private int powernumber = 297;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonLoad = findViewById(R.id.buttonLoad);
        buttonSave = findViewById(R.id.buttonSave);
        buttonStart = findViewById(R.id.buttonStart);
        textViewlocalRead = findViewById(R.id.textViewReadTotal);
        textViewStatus = findViewById(R.id.textViewStatus);
        listView = findViewById(R.id.listView);
        setupButtonLoadClickListener();
        setupButtonStartClickListener();
        setupButtonSaveClickListener();
        setupProgressDialog();
        setupLoadReaderTask();
        setupStatusMonitorTimer();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                RfidView item = (RfidView)adapterView.getItemAtPosition(i);
                Intent intent = new Intent(getApplicationContext(), FindActivity.class);
                intent.putExtra("id", item.getName());
                startActivity(intent);
            }
        });
        soundPoolHelper = new SoundPoolHelper(4,SoundPoolHelper.TYPE_MUSIC)
                .setRingtoneType(SoundPoolHelper.RING_TYPE_MUSIC)
                //加载默认音频，因为上面指定了，所以其默认是：RING_TYPE_MUSIC
                //happy1,happy2
                .loadDefault(MainActivity.this)
                .load(MainActivity.this,"happy1",R.raw.duka3);

    }
    public String get_power(){
        SharedPreferences sp = getSharedPreferences("power",MODE_PRIVATE);
        String power = sp.getString("power","");
        return power;
    }



    //连接信息
    private void setupProgressDialog() {
        if (progressDialog == null)
            progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.connecting));
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
    }

    private void setupStatusMonitorTimer() {
        if (scheduler != null) return;
        scheduler = Executors.newScheduledThreadPool(1);
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    if (reader != null) {
                        reader.Config.getDeviceStatus(true, true, true);
                    } else {
                        scheduler.shutdown();
                    }
                } catch (InvalidUsageException | OperationFailureException e) {
                    if (e instanceof OperationFailureException) {
                        Log.d(TAG, "OperationFailureException: " + ((OperationFailureException) e).getVendorMessage());
                    }
                    e.printStackTrace();
                }
            }
        };
        taskHandler = scheduler.scheduleAtFixedRate(task, 10, 60, SECONDS);
    }

    //SAVE

    private void setupButtonSaveClickListener() {
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(layList!= null){
                    layList.clear();
                    textViewlocalRead.setText("0");
                }

            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 42) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //开启读取的功能
    private void setupButtonStartClickListener() {
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public synchronized void onClick(View v) {
                if (reader == null) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.err_title)
                            .setMessage(R.string.no_reader)
                            .setNegativeButton(R.string.ok, null)
                            .create()
                            .show();
                } else {
                    if (isStarted) {
                        try {
                            reader.Actions.Inventory.stop();
                        } catch (InvalidUsageException | OperationFailureException e) {
                            e.printStackTrace();
                        }
                        isStarted = false;
                        buttonStart.setText(R.string.start);
                    } else {
                        try {
                            reader.Actions.Inventory.perform();
                            isStarted = true;
                        } catch (InvalidUsageException | OperationFailureException e) {
                            isStarted = false;
                            Toast.makeText(getApplicationContext(), R.string.no_start, Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                        if (isStarted) {
                            buttonStart.setText(R.string.stop);
                        }
                    }
                }
            }
        });
    }

    @SuppressLint("StaticFieldLeak")
    private void setupLoadReaderTask() {
        if (task != null) task.cancel(true);
        if (readers == null) readers = new Readers(this, ENUM_TRANSPORT.SERVICE_SERIAL);
        if (!progressDialog.isShowing()) progressDialog.show();
        task = new AsyncTask<Void, String, String>() {
            @Override
            protected synchronized String doInBackground(Void... voids) {
                InvalidUsageException hj = null;
                if (isCancelled()) return null;
                if (readers == null) return null;
                publishProgress("readers.GetAvailableRFIDReaderList()");
                if (isCancelled()) return null;
                List<ReaderDevice> list = null;
                try {
                    list = readers.GetAvailableRFIDReaderList();
                } catch (InvalidUsageException e) {
                    // e.printStackTrace();
                    hj = e;

                }
                if (hj != null){
                    readers.Dispose();
                    readers = null;
                    if (readers == null) {
                        readers = new Readers(MainActivity.this, ENUM_TRANSPORT.BLUETOOTH);
                    }
                }
                if (list == null || list.isEmpty()) return null;
                publishProgress("device.getRFIDReader()");
                if (isCancelled()) return null;
                for (ReaderDevice readerDevice : list) {
                    device = readerDevice;
                    // Log.d("setupLoadReaderTask", device.getName());
                    reader = device.getRFIDReader();
                    if (reader.isConnected()) return null;
                    publishProgress("reader.connect()");
                    if (isCancelled()) return null;
                    try {

                        reader.connect();
                        configureReader();
                    } catch (InvalidUsageException | OperationFailureException e) {
                        e.printStackTrace();
                    }
                    if (reader.isConnected()) break;
                }
                if (!reader.isConnected()) return null;
                return String.format("Connected to %s", device.getName());
            }

            @Override
            protected void onProgressUpdate(String... values) {
                // if (values.length == 0) return;
                // String s = null;
                // for (String value : values) s = value;
                // Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
            }

            @Override
            protected void onPostExecute(String s) {
                if (s == null) {
                   boolean isstarted = false;
                    if (isstarted == false){
                        setupLoadReaderTask();
                        isstarted = true;
                    }else{
                        setupRetryDialog();
                    }
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            protected void onCancelled() {
                reader = null;
                readers = null;
                Toast.makeText(getApplicationContext(), "Connection Cancelled", Toast.LENGTH_SHORT).show();
            }
        };
        task.execute();
    }

    //RFD设备设置
    private void configureReader()  {
        if (reader == null || !reader.isConnected()) return;
        TriggerInfo triggerInfo = new TriggerInfo();
        triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE);
        triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);
        if (eventHandler == null) eventHandler = new RfidEventHandler();

        try {
            reader.Events.addEventsListener(eventHandler);
            reader.Events.setHandheldEvent(true);
            reader.Events.setTagReadEvent(true);
            reader.Events.setBatteryEvent(true);
            reader.Events.setPowerEvent(true);
            reader.Events.setTemperatureAlarmEvent(true);
            reader.Events.setAttachTagDataWithReadEvent(false);
            reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE,true);
            reader.Config.setStartTrigger(triggerInfo.StartTrigger);
            reader.Config.setStopTrigger(triggerInfo.StopTrigger);
            Antennas.AntennaRfConfig config = null;
            config = reader.Config.Antennas.getAntennaRfConfig(1);
            config.setTransmitPowerIndex(powernumber);
            config.setrfModeTableIndex(0);
            config.setTari(0);
            reader.Config.Antennas.setAntennaRfConfig(1, config);
            Antennas.SingulationControl control = reader.Config.Antennas.getSingulationControl(1);
            control.setSession(SESSION.SESSION_S0);
            reader.Config.Antennas.setSingulationControl(1, control);
           int id =  config.getTransmitPowerIndex();
            intoPower(String.valueOf(id));
            reader.Actions.PreFilters.deleteAll();
        } catch (InvalidUsageException | OperationFailureException e) {
            e.printStackTrace();
        } // Log.d("OperationFailureException", e.getVendorMessage());

    }
    public void intoPower(String pdaid){
        SharedPreferences.Editor editor = getSharedPreferences("power",MODE_PRIVATE).edit();
        editor.clear();
        editor.putString("power",pdaid);
        editor.commit();
    }
    private void setupRetryDialog() {
        if (progressDialog.isShowing()) progressDialog.dismiss();
        new AlertDialog.Builder(this)
                .setTitle(R.string.err_title)
                .setMessage(R.string.retry)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setupLoadReaderTask();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create()
                .show();
    }

    private boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void setupButtonLoadClickListener() {
        buttonLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // aloProgressDialog();
                Intent intent = new Intent(MainActivity.this,SettingActivity.class);
                startActivity(intent);
            }
        });
    }


    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (task != null) task.cancel(true);
        if (reader != null) {
            task = new AsyncTask<Void, String, String>() {
                @Override
                protected String doInBackground(Void... voids) {
                    try {
                        reader.disconnect();
                    } catch (InvalidUsageException | OperationFailureException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
            task.execute();
        }
        if (readers != null) {
            readers.Dispose();
        }
    }



    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this).setTitle("确认退出吗？")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (eventHandler != null) {
                            try {
                                reader.Events.removeEventsListener(eventHandler);
                            } catch (InvalidUsageException e) {
                                e.printStackTrace();
                            } catch (OperationFailureException e) {
                                Log.d(TAG, "onBackPressed: " + e.getVendorMessage());
                                e.printStackTrace();
                            }
                            eventHandler = null;
                        }
                        finish();

                    }
                })
                .setNegativeButton("返回", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 点击“返回”后的操作,这里不设置没有任何操作
                    }
                }).show();

    }
    //RFD设备电池信息
    @SuppressLint("DefaultLocale")
    private void updateStatus() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewStatus.setText(String.format("%s:%d", getString(R.string.battery), battery));
            }
        });
    }



    //音效
    private class RfidEventHandler implements RfidEventsListener {
        @Override
        public void eventReadNotify(RfidReadEvents rfidReadEvents) {
            TagData[] tags = reader.Actions.getReadTags(10);
            if (tags == null) return;
            for (TagData tag : tags) {
                String tagName = tag.getTagID();
                select(tagName);
            }
        }
        private void select(String tagName){
            String Ascii = get_Ascii();
            if (Ascii.equals("true")){
                //if (tagName.substring(0,2).contains("40") == true){
                    soundPoolHelper.play("happy1",false);
                    gotTag(hexToAscii(tagName).replaceAll("@","").replaceAll("BB","").
                            replaceAll("BW","").replaceAll("FB",""));
                //}
            } else{
                soundPoolHelper.play("happy1",false);
                gotTag(tagName);
            }
        }
        public String get_Ascii(){
            SharedPreferences sp = getSharedPreferences("ascii",MODE_PRIVATE);
            return sp.getString("ascii","");
        }

        @SuppressLint("StaticFieldLeak")
        @Override
        public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {
            Events.StatusEventData data = rfidStatusEvents.StatusEventData;
            STATUS_EVENT_TYPE type = data.getStatusEventType();
             Log.d("STATUS", type.toString());
            if (type == STATUS_EVENT_TYPE.BATTERY_EVENT) {
                battery = data.BatteryData.getLevel();
               // Log.d(TAG,"电量："+battery );
                updateStatus();
            } else if (type == STATUS_EVENT_TYPE.TEMPERATURE_ALARM_EVENT) {
                temperature = data.TemperatureAlarmData.getCurrentTemperature();
                updateStatus();
            } else if (type == STATUS_EVENT_TYPE.POWER_EVENT){
              float io =  data.PowerData.getPower();
              //Log.d("Power", String.valueOf(io));

            } else if (type == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {
                HANDHELD_TRIGGER_EVENT_TYPE eventType = data.HandheldTriggerEventData.getHandheldEvent();
                if (eventType == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED) {
                    if (taskPerform != null) taskPerform.cancel(true);
                    taskPerform = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            try {
                                reader.Actions.Inventory.perform();
                                triggerStart();
                            } catch (InvalidUsageException | OperationFailureException e) {
                                triggerStop();
                                e.printStackTrace();
                            }
                            return null;
                        }
                    };
                    taskPerform.execute();
                } else if (eventType == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED) {
                    if (taskStop != null) taskStop.cancel(true);
                    taskStop = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            try {
                                reader.Actions.Inventory.stop();
                            } catch (InvalidUsageException | OperationFailureException e) {
                                e.printStackTrace();
                            }
                            triggerStop();
                            return null;
                        }
                    };
                    taskStop.execute();
                }
            }
        }
    }
    //扳机事件
    private void triggerStop() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                buttonStart.setText(R.string.start);
            }
        });
        isStarted = false;
    }
    private void triggerStart() {
        isStarted = true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                buttonStart.setText(R.string.stop);
                textViewlocalRead.setText("0");
                if(layList!= null)layList.clear();

            }
        });
    }
    //读取标签信息
    private void gotTag(final String tagid) {
        runOnUiThread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void run() {
                if(layList == null){
                    list.add(new RfidView(tagid,1));
                    layList = new LayList(list,MainActivity.this);
                    listView.setAdapter(layList);
                    textViewlocalRead.setText(String.valueOf(layList.getCount()));
                }else{
                    layList.additem(tagid);
                    textViewlocalRead.setText(String.valueOf(layList.getCount()));
                }

            }
        });
    }


    //ASCII转化
    private String hexToAscii(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i += 2) {
            sb.append((char) Integer.parseInt(str.substring(i, i + 2), 16));
        }
        return sb.toString();
    }

    @Override
    protected void onRestart() {
        super.onRestart();

    }

}
