package com.example.rfid_test;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.RfidEventsListener;
import com.zebra.rfid.api3.RfidReadEvents;
import com.zebra.rfid.api3.RfidStatusEvents;
import com.zebra.rfid.api3.TagData;

public class FindActivity extends AppCompatActivity {
    private static final String TAG = "FindActivity";
    String tagId = null;

    Button buttonCancel = null;
    EditText editText = null;
    FindBar findBar = null;

    RFIDReader reader = MainActivity.reader;
    AsyncTask<Void, Void, Boolean> task = null;

    class LocatingEventHandler implements RfidEventsListener {
        FindActivity context = null;

        LocatingEventHandler(FindActivity context) {
            this.context = context;
        }

        short dis;
        @Override
        public void eventReadNotify(RfidReadEvents rfidReadEvents) {
            final TagData[] tags = reader.Actions.getReadTags(100);
            if (tags != null) {
                for (TagData tag : tags) {
                    if (tag != null) {
                        if (tag.isContainsLocationInfo()) {
                            final short distance = tag.LocationInfo.getRelativeDistance();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    context.findBar.set(distance / 100f);
                                }
                            });
                            break;
                        }
                    }
                }
            }
        }

        @Override
        public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {

        }
    }

    private Handler handlerlook = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case 0x11:
            }
        }
    };

    LocatingEventHandler handler = new LocatingEventHandler(this);

    private SoundPoolHelper soundPoolHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find);
        tagId = getIntent().getStringExtra("id");
        editText = findViewById(R.id.editText);
        editText.setText(tagId);
        findBar = findViewById(R.id.findBar);
        buttonCancel = findViewById(R.id.button);
        soundPoolHelper = new SoundPoolHelper(4,SoundPoolHelper.TYPE_MUSIC)
                .setRingtoneType(SoundPoolHelper.RING_TYPE_MUSIC)
                //加载默认音频，因为上面指定了，所以其默认是：RING_TYPE_MUSIC
                //happy1,happy2
                .loadDefault(FindActivity.this)
                .load(FindActivity.this,"happy1",R.raw.duka3);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("StaticFieldLeak")
            @Override
            public void onClick(View v) {
                if (task != null) task.cancel(true);
                task = new AsyncTask<Void, Void, Boolean>() {
                    @Override
                    protected Boolean doInBackground(Void... voids) {
                        if (isCancelled()) return false;
                        if (reader == null) return false;
                        if (!reader.isConnected()) return false;
                        boolean success = false;
                        try {
                            reader.Events.removeEventsListener(handler);
                            reader.Events.addEventsListener(MainActivity.eventHandler);
                            reader.Actions.TagLocationing.Stop();
                            success = true;
                        } catch (InvalidUsageException e) {
                            e.printStackTrace();
                        } catch (OperationFailureException e) {
                            Log.d(TAG, "onCancelled: " + e.getVendorMessage());
                            e.printStackTrace();
                        }
                        return success;
                    }
                };
                task.execute();
                finish();
            }
        });

        startLocating();
    }

    public String get_Ascii(){
        SharedPreferences sp = getSharedPreferences("ascii",MODE_PRIVATE);
        return sp.getString("ascii","");
    }
    @SuppressLint("StaticFieldLeak")
    private void startLocating() {
        if (task != null) task.cancel(true);
        Log.d(TAG, "startLocating: ");
        task = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                if (isCancelled()) return false;
                if (reader == null) return false;
                if (!reader.isConnected()) return false;
                boolean success = false;
                try {
                    reader.Events.removeEventsListener(MainActivity.eventHandler);
                    reader.Events.addEventsListener(handler);
                    if (get_Ascii().equals("true")){
                        reader.Actions.TagLocationing.Perform(getTagID(), null, null);
                    }else{
                        reader.Actions.TagLocationing.Perform(tagId, null, null);
                    }
                    success = true;
                } catch (InvalidUsageException e) {
                    e.printStackTrace();
                } catch (OperationFailureException e) {
                    Log.d(TAG, "doInBackground: " + e.getVendorMessage());
                    e.printStackTrace();
                }
                return success;
            }
        };
        task.execute();
    }

    private String str;
    private String getTagID() {
             str = "@" + tagId;
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            sb.append(Integer.toHexString((int) c));
        }
        Log.d(TAG, "getTagID: " + sb.toString());
        return sb.toString();
    }


    @Override
    protected void onDestroy() {
        if (task != null) task.cancel(true);
        super.onDestroy();
    }
}
