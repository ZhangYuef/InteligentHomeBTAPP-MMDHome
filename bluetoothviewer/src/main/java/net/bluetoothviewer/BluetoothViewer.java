/*
     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
*/

package net.bluetoothviewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.TextView;

import net.bluetoothviewer.library.R;
import net.bluetoothviewer.util.ApplicationUtils;
import net.bluetoothviewer.util.FloatWindowService;


public class BluetoothViewer extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = BluetoothViewer.class.getSimpleName();
    private static final boolean D = true;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_LAUNCH_EMAIL_APP = 3;
    private static final int MENU_SETTINGS = 4;

    private static final String SAVED_PENDING_REQUEST_ENABLE_BT = "PENDING_REQUEST_ENABLE_BT";

    // Layout Views
    private TextView mStatusView;
    private EditText mOutEditText;
    private View mSendTextContainer;

    // Toolbar
    private ImageButton mToolbarConnectButton;
    private ImageButton mToolbarDisconnectButton;
    private ImageButton mToolbarPauseButton;
    private ImageButton mToolbarPlayButton;


    private ArrayAdapter<String> mConversationArrayAdapter;
    private DeviceConnector mDeviceConnector = new NullDeviceConnector();

    // State variables
    private boolean paused = false;
    private boolean connected = false;

    // do not resend request to enable Bluetooth
    // if there is a request already in progress
    // See: https://code.google.com/p/android/issues/detail?id=24931#c1
    private boolean pendingRequestEnableBt = false;

    // controlled by user settings
    private boolean recordingEnabled;
    private String defaultEmail;
    private boolean mockDevicesEnabled;

    private String deviceName;

    private boolean temp=true;
    private boolean window_open=false;

    private final StringBuilder recording = new StringBuilder();

    //database
    private DatabaseHelper dbhelper;
    //检测值
    public static String number_show;
    private SensorManager sensorManager;
    private float value;
    private boolean isLight=true;
    private   SensorEventListener listener=new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            value=event.values[0];
            Log.d("lxlxlx",value+"");
            float a=10;
            if((value < a) &&isLight){
                isLight=false;
                Log.d("lx","booooooooooooooooooooooooom");
                sendMessage("r");
            }else if((value>a)&&!isLight){
                isLight=true;
                sendMessage("s");
                Log.d("lx","Opennnnnnnnnnnnnnnnnnnnnnnnnnn");
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    //
    private ImageButton scheme_in_button;
    private ImageButton sound_in_button;
    private ImageButton voice_in_button;

    private String[]set;
    // The Handler that gets information back from the BluetoothService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessageHandler.MSG_CONNECTED:
                    connected = true;
                    mStatusView.setText(formatStatusMessage(R.string.btstatus_connected_to_fmt, msg.obj));
                    onBluetoothStateChanged();
                    recording.setLength(0);
                    deviceName = msg.obj.toString();
                    break;
                case MessageHandler.MSG_CONNECTING:
                    connected = false;
                    mStatusView.setText(formatStatusMessage(R.string.btstatus_connecting_to_fmt, msg.obj));
                    onBluetoothStateChanged();
                    break;
                case MessageHandler.MSG_NOT_CONNECTED:
                    connected = false;
                    mStatusView.setText(R.string.btstatus_not_connected);
                    onBluetoothStateChanged();
                    break;
                case MessageHandler.MSG_CONNECTION_FAILED:
                    connected = false;
                    mStatusView.setText(R.string.btstatus_not_connected);
                    onBluetoothStateChanged();
                    break;
                case MessageHandler.MSG_CONNECTION_LOST:
                    connected = false;
                    mStatusView.setText(R.string.btstatus_not_connected);
                    onBluetoothStateChanged();
                    break;
                case MessageHandler.MSG_BYTES_WRITTEN:
                    String written = new String((byte[]) msg.obj);
                    mConversationArrayAdapter.add(">>> " + written);
                    Log.i(TAG, "written = '" + written + "'");
                    break;
                case MessageHandler.MSG_LINE_READ:
                    if (paused) break;
                    if(temp)
                        temp=false;
                    else {
                        String line = (String) msg.obj;
                        //mConversationArrayAdapter.add(line);
                        //if (recordingEnabled) {
                        //    recording.append(line).append("\n");
                        //}

                        set = line.split(" ");

                        TextView number_in = (TextView) findViewById(R.id.number_in);
                        TextView set_in = (TextView) findViewById(R.id.set_in);
                        TextView temp_in = (TextView) findViewById(R.id.temp_in);
                        TextView scheme_in = (TextView) findViewById(R.id.scheme_in);
                        TextView sound_in = (TextView) findViewById(R.id.sound_in);
                        TextView voice_in = (TextView) findViewById(R.id.voice_in);

                        number_show = set[1];

                        if(set[7].equals("1")){
                           NotificationManager notificationManager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                            Notification notification=new Notification(R.drawable.launcher_main,"", System.currentTimeMillis());
                            notification.setLatestEventInfo(BluetoothViewer.this,"PM2.5超标","打开智能管家",null);
                            notification.ledARGB= Color.GREEN;
                            notification.ledOnMS=1000;
                            notification.ledOffMS=1000;
                            notification.flags=Notification.FLAG_SHOW_LIGHTS;
                            long[]vibrates={0,200,100,200,100,200,100,200};
                            notification.vibrate=vibrates;
                            notificationManager.notify(1,notification);
                        }

                        number_in.setText(set[1]);
                        set_in.setText(set[2]);
                        temp_in.setText(set[3]);
                       /*if (set[4].equals("0")) {
                            scheme_in_B
                        } else scheme_in.setText("自动");
                        if (set[4].equals("0")) {
                            sound_in.setText("关闭");
                        } else sound_in.setText("开启");
                        if (set[5].equals("0")) {
                            voice_in.setText("关闭");
                        } else voice_in.setText("开启");
                        */
                        //init value
                        TableRow table1=(TableRow)findViewById(R.id.table1);
                        TableRow table2=(TableRow)findViewById(R.id.table2);
                        if(set[4].equals("0")){
                            scheme_in.setText("手动");
                      //      scheme_in_button.setChecked(true);
                            table1.setVisibility(View.VISIBLE);
                            table2.setVisibility(View.VISIBLE);
                        }else if(set[4].equals("1")){
                            scheme_in.setText("自动");
                       //     scheme_in_button.setChecked(false);
                            table1.setVisibility(View.GONE);
                            table2.setVisibility(View.GONE);
                        }

                        if(set[5].equals("0")){
                            sound_in.setText("关闭");
                        //    sound_in_button.setChecked(true);
                        }else if(set[5].equals("1")){
                            sound_in.setText("开启");
                        //    sound_in_button.setChecked(false);
                        }
                        if(set[6].equals("1")){
                            voice_in.setText("开启");
                         //   voice_in_button.setChecked(true);
                        }else if(set[6].equals("0")){
                            voice_in.setText("关闭");
                         //   voice_in_button.setChecked(false);
                        }

                    }
                    break;
            }
        }
    };



    private TextView.OnEditorActionListener mWriteListener =
            new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                        sendMessage(view.getText());
                    }
                    return true;
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "++onCreate");
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            pendingRequestEnableBt = savedInstanceState.getBoolean(SAVED_PENDING_REQUEST_ENABLE_BT);
        }

        setContentView(R.layout.bluetoothviewer);

        //数据库?????????????????????????/
        //TODO:database
      /*  SQLiteDatabase db =openOrCreateDatabase("test.db",Context.MODE_PRIVATE,null);
        db.execSQL("DROP TABLE IF EXISTS record");
        //create record table
        db.execSQL("CREATE TABLE record(_id INTEGER PRIMARY KEY AUTOINCREMENT,time float, Y AUTOINCREMENTT,data float)");
        db.execSQL("intsert int record values(NULL,?,?)",new String[]{"2016-7-19","0.010"});

        dbHelper=new MyDatabaseHelper(this,"Number.db",null,1);
        Log.d("database","coming in");
        boolean database_find=false;
        if(!database_find) {
            database_find=true;
            dbHelper.getWritableDatabase();
        }*/
        dbhelper=new DatabaseHelper(this,"BookStore.db",null,1);
        Button createDatabase=(Button)findViewById(R.id.create_database);
        createDatabase.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dbhelper.getWritableDatabase();
                Log.d("database","yeeeeeeeeeeeeeee");
            }
        });

        //悬浮窗
        Button startFloatWindow=(Button)findViewById(R.id.start_float_window);
        startFloatWindow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(BluetoothViewer.this,FloatWindowService.class);
                intent.putExtra("number_show",number_show);
                startService(intent);
                finish();
            }
        });

        //通知
        final Button sendNotice=(Button)findViewById(R.id.send_notice);
        sendNotice.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationManager notificationManager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                Notification notification=new Notification(R.drawable.launcher_main,"", System.currentTimeMillis());
                notification.setLatestEventInfo(BluetoothViewer.this,"PM2.5超标","打开智能管家",null);
                notification.ledARGB= Color.GREEN;
                notification.ledOnMS=1000;
                notification.ledOffMS=1000;
                notification.flags=Notification.FLAG_SHOW_LIGHTS;
                long[]vibrates={0,200,100,200};
                notification.vibrate=vibrates;
                notificationManager.notify(1,notification);
            }
        });

        //更改当前设置
        scheme_in_button=(ImageButton)findViewById(R.id.scheme_in_button);
        scheme_in_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(set[4].equals("0")){
                    sendMessage("a");
                }else {
                    sendMessage("b");
                }
            }
        });

        sound_in_button = (ImageButton) findViewById(R.id.sound_in_button);
        sound_in_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(set[5].equals("0")){
                    sendMessage("n");
                }else {
                    sendMessage("o");
                }
            }
        });

        voice_in_button=(ImageButton) findViewById(R.id.voice_in_button);
        voice_in_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(set[6].equals("0")){
                    sendMessage("l");
                }else {
                    sendMessage("m");
                }
            }
        });

        Button window_in_button1=(Button) findViewById(R.id.window_in_button1);
        window_in_button1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage("c");
            }
        });
        Button window_in_button2=(Button) findViewById(R.id.window_in_button2);
        window_in_button2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage("e");
            }
        });

        Button air_in_button1=(Button) findViewById(R.id.air_in_button1);
        air_in_button1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage("g");
            }
        });
        Button air_in_button2=(Button) findViewById(R.id.air_in_button2);
        air_in_button2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage("i");
            }
        });

        Button broadcast_button=(Button)findViewById(R.id.broadcast_button);
        broadcast_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage("k");
            }
        });

        //voice down up
        Button voice_up=(Button)findViewById(R.id.voice_up);
        voice_up.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage("p");
            }
        });
        Button voice_down=(Button)findViewById(R.id.voice_down);
        voice_down.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage("q");
            }
        });
        mStatusView = (TextView) findViewById(R.id.btstatus);

        mSendTextContainer = findViewById(R.id.send_text_container);

        //文字输入框
        TextView set_in=(TextView)findViewById(R.id.set_in);
        set_in.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSendTextContainer.setVisibility(View.VISIBLE);
            }
        });

        //light sensor
        sensorManager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor=sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener(listener,sensor,SensorManager.SENSOR_DELAY_NORMAL);

        mToolbarConnectButton = (ImageButton) findViewById(R.id.toolbar_btn_connect);
        mToolbarConnectButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                startDeviceListActivity();
            }
        });

        mToolbarDisconnectButton = (ImageButton) findViewById(R.id.toolbar_btn_disconnect);
        mToolbarDisconnectButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                disconnectDevices();
            }

        });

        mToolbarPauseButton = (ImageButton) findViewById(R.id.toolbar_btn_pause);
        mToolbarPauseButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                paused = true;
                onPausedStateChanged();
            }
        });

        mToolbarPlayButton = (ImageButton) findViewById(R.id.toolbar_btn_play);
        mToolbarPlayButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                paused = false;
                onPausedStateChanged();
            }
        });

        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        ListView mConversationView = (ListView) findViewById(R.id.in);
        mConversationView.setAdapter(mConversationArrayAdapter);

        boolean isLiteVersion = ApplicationUtils.isLiteVersion(getApplication());

        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener for click events
        Button mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                //sendMessage(view.getText());
                String s=view.getText().toString();
                char[] s1=new char[10];
               // s.getChars(0,3,s1,0);
                char a=s.charAt(0);
                char b=s.charAt(1);
                char c=s.charAt(2);

                /*sendMessage(s1[0]+"");
                sendMessage(s1[1]+"");
                sendMessage(s1[2]+"");
            */
                String x=String.valueOf(a);
                String y=String.valueOf(b);
                String z=String.valueOf(c);
                Log.d("lalalalala",x);
                Log.d("lalalalala",y);
                Log.d("lalalalala",z);
                sendMessage(x);
                try {
                    Thread.sleep(2000);
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                sendMessage(y);
                try {
                    Thread.sleep(2000);
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                sendMessage(z);
            }
        });

        onBluetoothStateChanged();

        if (!mockDevicesEnabled) {
            requestEnableBluetooth();
        }
    }


    private void updateParamsFromSettings() {
        recordingEnabled = getSharedPreferences().getBoolean(getString(R.string.pref_record), false);
        defaultEmail = getSharedPreferences().getString(getString(R.string.pref_default_email), "");
        mockDevicesEnabled = getSharedPreferences().getBoolean(getString(R.string.pref_enable_mock_devices), false);
    }

    private void startDeviceListActivity() {
        Intent intent = new Intent(this, DeviceListActivity.class);
        intent.putExtra(DeviceListActivity.EXTRA_MOCK_DEVICES_ENABLED, mockDevicesEnabled);
        startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
    }

    private void requestEnableBluetooth() {
        if (!isBluetoothAdapterEnabled() && !pendingRequestEnableBt) {
            pendingRequestEnableBt = true;
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    private boolean isBluetoothAdapterEnabled() {
        return getBluetoothAdapter().isEnabled();
    }

    private BluetoothAdapter getBluetoothAdapter() {
        return BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDeviceConnector.disconnect();
    }

    private void sendMessage(CharSequence chars) {
        if (chars.length() > 0) {
            mDeviceConnector.sendAsciiMessage(chars);
            mOutEditText.setText("");
        }
    }

    private String formatStatusMessage(int formatResId, Object obj) {
        String deviceName = (String) obj;
        return getString(formatResId, deviceName);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with connection info to connect devices
                // TODO it would be better to return a Parcelable instance of a DeviceConnector,
                //      as in the current approach both the sender and the receiver must know
                //      how to create a DeviceConnector from its pieces (constructor args).
                //      It would be better if that logic was in one place,
                //      and definitely not in this activity
                if (resultCode == Activity.RESULT_OK) {
                    String connectorTypeMsgId = DeviceListActivity.Message.DeviceConnectorType.toString();
                    DeviceListActivity.ConnectorType connectorType =
                            (DeviceListActivity.ConnectorType) data.getSerializableExtra(connectorTypeMsgId);
                    MessageHandler messageHandler = new MessageHandlerImpl(mHandler);
                    switch (connectorType) {
                        case Mock:
                            String filenameMsgId = DeviceListActivity.Message.MockFilename.toString();
                            String filename = data.getStringExtra(filenameMsgId);
                            mDeviceConnector = new MockLineByLineConnector(messageHandler, getAssets(), filename);
                            break;
                        case Bluetooth:
                            String addressMsgId = DeviceListActivity.Message.BluetoothAddress.toString();
                            String address = data.getStringExtra(addressMsgId);
                            mDeviceConnector = new BluetoothDeviceConnector(messageHandler, address);
                            break;
                        default:
                            return;
                    }
                    mDeviceConnector.connect();
                }
                break;
            case MENU_SETTINGS:
                updateParamsFromSettings();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(item.getItemId()==R.id.menu_graphic){
           // Intent intent=new Intent(BluetoothViewer.this, Graphic.class);
           // startActivity(intent);
          //  Toast.makeText(this,"laaaa",Toast.LENGTH_SHORT).show();
            AlertDialog.Builder dialog =new AlertDialog.Builder(BluetoothViewer.this);
            dialog.setTitle("PM2.5记录表");
            LayoutInflater factory=LayoutInflater.from(BluetoothViewer.this);
            final View view =factory.inflate(R.layout.graphic,null);
            dialog.setView(view);
            dialog.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            dialog.show();
            Log.d("menu","2");
        }else{
            Log.d("menu",item.getItemId()+"");
        }
        return true;
    }

    private void disconnectDevices() {
        mDeviceConnector.disconnect();
        onBluetoothStateChanged();
    }

    private void onBluetoothStateChanged() {
        if (connected) {
            mToolbarConnectButton.setVisibility(View.GONE);
            mToolbarDisconnectButton.setVisibility(View.VISIBLE);
          //  mSendTextContainer.setVisibility(View.VISIBLE);
        } else {
            mToolbarConnectButton.setVisibility(View.VISIBLE);
            mToolbarDisconnectButton.setVisibility(View.GONE);
          //  mSendTextContainer.setVisibility(View.GONE);
        }
        paused = false;
        onPausedStateChanged();
    }

    private void onPausedStateChanged() {
        if (connected) {
            if (paused) {
                mToolbarPlayButton.setVisibility(View.VISIBLE);
                mToolbarPauseButton.setVisibility(View.GONE);
            } else {
                mToolbarPlayButton.setVisibility(View.GONE);
                mToolbarPauseButton.setVisibility(View.VISIBLE);
            }
        } else {
            mToolbarPlayButton.setVisibility(View.GONE);
            mToolbarPauseButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "++onSaveInstanceState");
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_PENDING_REQUEST_ENABLE_BT, pendingRequestEnableBt);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String prefName) {
        Log.d(TAG, "++onSharedPreferenceChanged");
        if (prefName.equals(getString(R.string.pref_record))) {
            updateParamsFromSettings();
        }
    }

    private SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    protected void onResume() {
        getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    protected void onPause() {
        getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }
}
