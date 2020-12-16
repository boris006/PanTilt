package com.example.bt_firsttry;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.bt_firsttry.views.CustomView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import io.github.controlwear.virtual.joystick.android.JoystickView;

//TODO camera focus
//TODO preview mit buttons [xdir][###][ydir][###]
public class Timelapse extends AppCompatActivity {
    //Camera and Timelapse
    Camera camera;
    FrameLayout frameLayout; //for camera preview
    SurfaceView mSurfaceView; //for preview while recording timelapse
    SurfaceHolder mHolder; //holder for surfaceView
    ShowCamera showCamera; //Class for camera preview on framelayout
    MediaRecorder recorder;
    CustomView crossView;
    int captureMode;
    SwitchCompat SwitchCapture;

    //Joystick
    JoystickView joystickPan;

    //menu
    Button btnJoystick, btn_i, btnSettings, btnMotion, btnBlock;
    boolean iIsShown = false, settingsIsShown = false, isBlocked = false;
    LinearLayout menuSettings, menuBar;

    //sensor
    TextView textX, textY;

    //bluetooth
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    float[] orientations = new float[3]; //orientation 1 tilt sensor
    int i = 0;
    Boolean recording = Boolean.FALSE;

    //set UUID
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    String address = null;

    //TextView BT Inputstream;
    InputStream mmInputStream;
    TextView textBtOutput, textBtInput;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;


    //Automatic Mode Point Initialisation
    Boolean allPointsSet = false, continueMoving = false, stringsent = true, updatedSensors = false, positionA = false,
    ABSet = false, ABCSet = false;
    Button setPoint, btnA, btnB, btnC, btn_timelapse;
    Point pointA, pointB, pointC, destination, position;
    TextView a_x, a_y, b_x, b_y, c_x, c_y;
    int automaticStep;

    //SeekBar Speed Moving Time
    TextView txtMovingTime, txtSpeedRatio;
    int speed_ratio = 50;
    int counter = 0;
    int movingtime = 60;     //overall moving time in seconds (ab + bc = movingtime)//TODO moving time moved here


    class Point{
        int x_angle;
        int y_angle;
        boolean isSet;
        String name;

        public Point(){
            x_angle = 0;
            y_angle = 0;
            isSet = false;
            name = "";
         }
         public void setFromOrientation(){
             x_angle = Math.round(orientations[0]);
             y_angle = Math.round(orientations[1]);
             isSet = true;
         }
         public void reset(){
             x_angle = 0;
             y_angle = 0;
             isSet = false;
             name = "";
         }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timelapse_layout);
        useSensor();
        //hide nav bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mHolder = mSurfaceView.getHolder();
        frameLayout = (FrameLayout) findViewById(R.id.framelayout);
        if (recorder == null)
            recorder = new MediaRecorder();

        //open camera
        camera = Camera.open();
        showCamera = new ShowCamera(this, camera);
        frameLayout.addView(showCamera);
        frameLayout.setVisibility(View.VISIBLE);
        //showCamera.startShowing();

        //MediaRecorder recorder = new MediaRecorder();*/
        Log.e("state","on create before bt");

        //Bluetooth
        Intent newInt = getIntent();
        address = newInt.getStringExtra(DeviceList.EXTRA_ADDRESS);
        Log.e("state","on create after intent bt");
        new ConnectBTTime().execute();
        //beginListenForData();

        //create Joystick
        crossView = (CustomView) findViewById(R.id.CustomView);
        joystickPan = (JoystickView) findViewById(R.id.joystickCamera);
        Log.e("state","on create before joystick");
        joystickPan.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {

                crossView.adjustCross(joystickPan.getNormalizedX(), joystickPan.getNormalizedY());
                sendPosition();

            }
        },100); //TODO send interval in ms

        //text sensors
        textX = (TextView) findViewById(R.id.textViewX);
        textY = (TextView) findViewById(R.id.textViewY);
        //text Bt
        textBtOutput = (TextView) findViewById(R.id.txt_btSendString);
        textBtInput = (TextView) findViewById(R.id.textViewData);

        //Automatic Mode Point Initialisation
        pointA = new Point();
        pointB = new Point();
        pointC = new Point();
        //set Points A and B
        //setPoint = (Button)findViewById(R.id.setPoint);
        /*setPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                //has a been initialised?
                if (pointA.isSet == false){
                    pointA.setFromOrientation();
                    pointA.name = "A";
                    setPoint.setText("Punkt B setzen");
                    moveToA.setEnabled(true);
                    moveToA.setVisibility(View.VISIBLE);
                }else{
                    if (pointB.isSet == false){
                        pointB.setFromOrientation();
                        pointA.name = "B";
                        setPoint.setText("Punkte Zurücksetzen");
                        allPointsSet = true;
                        moveToB.setEnabled(true);
                        moveToB.setVisibility(View.VISIBLE);
                    }else{
                        pointA.reset();
                        pointB.reset();
                        setPoint.setText("Punkt A setzen");
                        moveToA.setEnabled(false);
                        moveToA.setVisibility(View.INVISIBLE);
                        moveToB.setEnabled(false);
                        moveToB.setVisibility(View.INVISIBLE);
                        allPointsSet = false;
                    }

                }

            }
        });*/
        //set and reset A
        btnA = (Button)findViewById(R.id.btn_a);
        a_x = (TextView) findViewById(R.id.a_x);
        a_y = (TextView) findViewById(R.id.a_y);
        a_x.setText("X_a: N/S");
        a_y.setText("X_a: N/S");
        //btnA.setBackgroundColor(ContextCompat.getColor(Timelapse.this,
          //      R.color.btn_off));
        btnA.setBackgroundResource(R.drawable.button_background_off);
        btnA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!pointA.isSet){
                    pointA.setFromOrientation();
                    a_x.setText("X_a:" + pointA.x_angle);
                    a_y.setText("X_a:" + pointA.y_angle);
                    pointA.name = "A";
                    btnA.setBackgroundResource(R.drawable.button_background_on);
                    if(pointA.isSet && pointB.isSet){
                        ABSet = true;
                    }
                    if(pointA.isSet && pointB.isSet && pointC.isSet){
                        ABCSet = true;
                    }
                }
            }
        });
        btnA.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                pointA.reset();
                a_x.setText("X_a: N/S");
                a_y.setText("X_a: N/S");
                ABSet = false;
                ABCSet = false;
                btnA.setBackgroundResource(R.drawable.button_background_off);
                return true;
            }
        });

        //set and reset B
        btnB = (Button)findViewById(R.id.btn_b);
        b_x = (TextView) findViewById(R.id.b_x);
        b_y = (TextView) findViewById(R.id.b_y);
        b_x.setText("X_b: N/S");
        b_y.setText("X_b: N/S");
        btnB.setBackgroundResource(R.drawable.button_background_off);
        btnB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!pointB.isSet){
                    pointB.setFromOrientation();
                    b_x.setText("X_b:" + pointB.x_angle);
                    b_y.setText("X_b:" + pointB.y_angle);
                    pointB.name = "C";
                    btnB.setBackgroundResource(R.drawable.button_background_on);
                    if(pointA.isSet && pointB.isSet){
                        ABSet = true;
                    }
                    if(pointA.isSet && pointB.isSet && pointC.isSet){
                        ABCSet = true;
                    }
                }
            }
        });
        btnB.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                pointB.reset();
                b_x.setText("X_b: N/S");
                b_y.setText("X_b: N/S");
                ABSet = false;
                ABCSet = false;
                btnB.setBackgroundResource(R.drawable.button_background_off);
                return true;
            }
        });

        //set and reset C
        btnC = (Button) findViewById(R.id.btn_c);
        c_x = (TextView) findViewById(R.id.c_x);
        c_y = (TextView) findViewById(R.id.c_y);
        c_x.setText("X_c: N/S");
        c_y.setText("X_c: N/S");
        btnC.setBackgroundResource(R.drawable.button_background_off);
        btnC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!pointC.isSet){
                    pointC.setFromOrientation();
                    c_x.setText("X_c:" + pointC.x_angle);
                    c_y.setText("X_c:" + pointC.y_angle);
                    pointC.name = "C";
                    btnC.setBackgroundResource(R.drawable.button_background_on);
                    if(pointA.isSet && pointB.isSet && pointC.isSet){
                        ABCSet = true;
                    }
                }
            }
        });
        btnC.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                pointC.reset();
                c_x.setText("X_c: N/S");
                c_y.setText("X_c: N/S");
                ABCSet = false;
                btnC.setBackgroundResource(R.drawable.button_background_off);
                return true;
            }
        });


        //Start automatic Mode with Timelapse Button
        automaticStep = 0;
        btn_timelapse = (Button)findViewById(R.id.timelapse);
        btn_timelapse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    handleAutomatic();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        btnJoystick = (Button)findViewById(R.id.btn_joy);

        //TODO change real Background
        //btn_Joystick.setBackgroundColor(ContextCompat.getColor(Timelapse.this,
        //        R.color.btn_off));
        btnJoystick.setBackgroundResource(R.drawable.joy_icon_white);
        joystickPan.setVisibility(View.GONE);
        crossView.setVisibility(View.GONE);
        btnJoystick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(joystickPan.isShown()){
                    joystickPan.setVisibility(View.GONE);
                    crossView.setVisibility(View.GONE);
                    //btn_Joystick.setBackgroundColor(ContextCompat.getColor(Timelapse.this,
                    //        R.color.btn_off));
                    btnJoystick.setBackgroundResource(R.drawable.joy_icon_white);
                }
                else{
                    joystickPan.setVisibility(View.VISIBLE);
                    crossView.setVisibility(View.VISIBLE);
                    //btnJoystick.setBackgroundColor(ContextCompat.getColor(Timelapse.this,
                      //      R.color.btn_on));
                    btnJoystick.setBackgroundResource(R.drawable.joy_icon_yellow);
                }
            }
        });

        btn_i = (Button)findViewById(R.id.btn_i);

        //TODO change real Background
        //init
        a_x.setVisibility(View.GONE);
        a_y.setVisibility(View.GONE);
        b_x.setVisibility(View.GONE);
        b_y.setVisibility(View.GONE);
        c_x.setVisibility(View.GONE);
        c_y.setVisibility(View.GONE);
        textX.setVisibility(View.GONE);
        textY.setVisibility(View.GONE);
        textBtOutput.setVisibility(View.GONE);
        textBtInput.setVisibility(View.GONE);
        //btn_i.setBackgroundColor(ContextCompat.getColor(Timelapse.this,
          //      R.color.btn_off));
        btn_i.setBackgroundResource(R.drawable.info_icon_white);
        btn_i.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(iIsShown){
                    a_x.setVisibility(View.GONE);
                    a_y.setVisibility(View.GONE);
                    b_x.setVisibility(View.GONE);
                    b_y.setVisibility(View.GONE);
                    c_x.setVisibility(View.GONE);
                    c_y.setVisibility(View.GONE);
                    textX.setVisibility(View.GONE);
                    textY.setVisibility(View.GONE);
                    textBtOutput.setVisibility(View.GONE);
                    textBtInput.setVisibility(View.GONE);
                    //btn_i.setBackgroundColor(ContextCompat.getColor(Timelapse.this,
                      //      R.color.btn_off));
                    btn_i.setBackgroundResource(R.drawable.info_icon_white);
                    iIsShown = false;
                }
                else{
                    a_x.setVisibility(View.VISIBLE);
                    a_y.setVisibility(View.VISIBLE);
                    b_x.setVisibility(View.VISIBLE);
                    b_y.setVisibility(View.VISIBLE);
                    c_x.setVisibility(View.VISIBLE);
                    c_y.setVisibility(View.VISIBLE);
                    textX.setVisibility(View.VISIBLE);
                    textY.setVisibility(View.VISIBLE);
                    textBtOutput.setVisibility(View.VISIBLE);
                    textBtInput.setVisibility(View.VISIBLE);
                    //btn_i.setBackgroundColor(ContextCompat.getColor(Timelapse.this,
                      //      R.color.btn_on));
                    btn_i.setBackgroundResource(R.drawable.info_icon_yellow);
                    iIsShown = true;
                }
            }
        });

        //SeekBar and text
        txtMovingTime = (TextView) findViewById(R.id.txtMovingSpeed);
        txtSpeedRatio = (TextView) findViewById(R.id.txtSpeedRatio);
        SeekBar sBMovingTime = findViewById(R.id.sBMovingTime);
        SeekBar sBSpeedRatio = findViewById(R.id.sBSpeedRatio);
        sBMovingTime.setOnSeekBarChangeListener(seekBarTimeChangeListener);
        sBSpeedRatio.setOnSeekBarChangeListener(seekBarRatioChangeListener);
       //sBMovingTime.setOnSeekBarChangeListener(seekBarChangeListener1);
        int progress = sBMovingTime.getProgress();
        setMovingTime(3);

        //Edit text Motionspeed
        /*InputMovingTime = (EditText) findViewById(R.id.InputMovingTime);
        txtMovingTime = (TextView) findViewById(R.id.txtMovingTime);
        InputMovingTime.addTextChangedListener(new TextWatcher() {//TODO get running
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                //movingtime = Integer.parseInt(InputMovingTime.getText().toString());
                //txtMovingTime.setText(movingtime);

            }
        });*/

        //Switch for Timelapse Capture rate
        SwitchCapture = (SwitchCompat) findViewById(R.id.switchCapture);
        SwitchCapture.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position
                if(isChecked){
                    captureMode = 3;
                    Log.e("Switch","Timelapse on");
                }else{
                    captureMode = 24;//TODO put real rate
                    Log.e("Switch","Timelapse off");
                }
            }
        });

        //menu bar
        menuBar = (LinearLayout) findViewById(R.id.menuBar);
        //Motion menu
        btnMotion = (Button) findViewById(R.id.btn_motion);
        btnMotion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnMotion.setBackgroundResource(R.drawable.motion_icon_yellow);
                showMotionMenu(v);
            }
        });
        //Settings menu

        btnSettings = (Button) findViewById(R.id.btn_settings);
        menuSettings = (LinearLayout) findViewById(R.id.menuSettings);
        menuSettings.setVisibility(View.GONE);
        btnSettings.setBackgroundResource(R.drawable.settings_icon_whit);
        //btn_settings.setBackgroundColor(ContextCompat.getColor(Timelapse.this,
        //        R.color.btn_off));
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(settingsIsShown){
                    menuSettings.setVisibility(View.GONE);
                    //btn_settings.setBackgroundColor(ContextCompat.getColor(Timelapse.this,
                    //        R.color.btn_off));
                    btnSettings.setBackgroundResource(R.drawable.settings_icon_whit);
                    settingsIsShown = false;
                }
                else{
                    menuSettings.setVisibility(View.VISIBLE);
                    //btn_settings.setBackgroundColor(ContextCompat.getColor(Timelapse.this,
                    //        R.color.btn_on));
                    btnSettings.setBackgroundResource(R.drawable.settings_icon_yellow);
                    settingsIsShown = true;
                }
            }
        });

        btnBlock = (Button) findViewById(R.id.btn_block);
        btnBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isBlocked){
                    btnBlock.setBackgroundResource(R.drawable.block_icon_white);
                    isBlocked = false;
                    joystickPan.setEnabled(true);
                    btnMotion.setEnabled(true);
                    btnA.setEnabled(true);
                    btnB.setEnabled(true);
                    btnC.setEnabled(true);
                    sBMovingTime.setEnabled(true);
                    sBSpeedRatio.setEnabled(true);
                    SwitchCapture.setEnabled(true);

                }else{
                    btnBlock.setBackgroundResource(R.drawable.block_icon_yellow);
                    isBlocked = true;
                    joystickPan.setEnabled(false);
                    btnMotion.setEnabled(false);
                    btnA.setEnabled(false);
                    btnB.setEnabled(false);
                    btnC.setEnabled(false);
                    sBMovingTime.setEnabled(false);
                    sBSpeedRatio.setEnabled(false);
                    SwitchCapture.setEnabled(false);
                }
            }
        });
    }
    @Override
    protected void onResume(){
        super.onResume();
        Log.e("state", " on resume ");
        if(camera == null) {
            Log.e("state", " on resume camera null ");
            camera = Camera.open();
            showCamera = new ShowCamera(this, camera);
            frameLayout.addView(showCamera);
            frameLayout.setVisibility(View.VISIBLE);
            //showCamera.startShowing();

        }

    }
    @Override
    protected void onPause() {
        super.onPause();

        if (recording = Boolean.TRUE) {

            recording = Boolean.FALSE; //Change Text of Button
            buttonHandler();
            recorder.reset();
        }

        if (camera != null) {
            //camera.release();
            camera = null;
            Log.e("state", " onpause camera released");
        }
        if ((progress != null) && progress.isShowing())
            progress.dismiss();
        progress = null;


    }
    //hide nav bar on focus change
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    SeekBar.OnSeekBarChangeListener seekBarTimeChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBarSpeed, int progress, boolean fromUser) {
            // updated continuously as the user slides the thumb
            setMovingTime(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBarSpeed) {
            // called when the user first touches the SeekBar
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBarSpeed) {
            // called after the user finishes moving the SeekBar
        }
    };

    SeekBar.OnSeekBarChangeListener seekBarRatioChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            speed_ratio = progress;
            txtSpeedRatio.setText("Time ratio at B: " + progress + "%");
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    private void showMotionMenu(View v){
        PopupMenu settingsMenu = new PopupMenu(Timelapse.this,v);
        settingsMenu.getMenuInflater().inflate(R.menu.motionmenu,settingsMenu.getMenu());
        settingsMenu.show();
        settingsMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                btnMotion.setBackgroundResource(R.drawable.motion_icon_white);
            }
        });
        settingsMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                btnMotion.setBackgroundResource(R.drawable.motion_icon_white);
                if(item.getItemId() == R.id.menuToA){
                    try {
                        destination = pointA;
                        moveToPoint(pointA,1,"a");
                        Log.e("Move","a");

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if(item.getItemId() == R.id.menuToB){
                    try {
                        destination = pointB;
                        moveToPoint(pointB,1,"b");

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if(item.getItemId() == R.id.menuToC){
                    try {
                        destination = pointC;
                        moveToPoint(pointC,1,"c");

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }
        });
    }

    private String getOutputVideoFilePath() {
        // Create a media file name
        File folder_gui = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "temp");

        if(!folder_gui.exists()){
            folder_gui.mkdir();
        }
        String formatted_date = new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss").format(new Date(System.currentTimeMillis()));
        String mediaFile = Environment.getExternalStorageDirectory().getPath() + File.separator + "temp" + File.separator + "VID_" + formatted_date + ".mp4";
        return mediaFile;
    }


    public void buttonHandler() {
        //Button timelapse = (Button) findViewById(R.id.timelapse);
        if (recording == Boolean.FALSE) {
            //btn_timelapse.setText("Timelapse Starten");
            btn_timelapse.setBackgroundResource(R.drawable.icon_camera_white);
        } else {
            //btn_timelapse.setText("Timelapse Stoppen");
            btn_timelapse.setBackgroundResource(R.drawable.icon_camera_yellow);
        }


    }

    public void startTimelapse() throws IOException {
        //
        if (recording == Boolean.FALSE) { //not recording yet but starting
            //MediaRecorder recorder = new MediaRecorder();
            showCamera.stopShowing();   //Turn Off Camera Preview
            frameLayout.setVisibility(View.INVISIBLE); //Set Framelayout invisble, to show recording of timelapse
            Log.e("state","trying started1");
            Camera.Parameters params = camera.getParameters();
            String fileName = getOutputVideoFilePath(); //Video file path with name
            //if (recorder == null)
             //   recorder = new MediaRecorder();
            if (mSurfaceView.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) { //set orientation of preview and recorded video
                params.set("orientation", "landscape");
                camera.setDisplayOrientation(0);
                params.setRotation(0);
            } else {
                params.set("orientation", "portrait");
                camera.setDisplayOrientation(90);
                params.setRotation(270);
            }
            if (params.getSupportedFocusModes().contains(
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            camera.setParameters(params); //apply changed parameters to camera object
            camera.unlock();
            //recorder.getSurface(camera);
            //Log.e("state","trying started2");
            recorder.setCamera(camera);
            Log.e("state","trying started2");
            recorder.setPreviewDisplay(mHolder.getSurface());
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
            if (mSurfaceView.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                recorder.setOrientationHint(0);

            } else {
                recorder.setOrientationHint(90);
            }
            //recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            //recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            //recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);


            recorder.setOutputFile(fileName);
            recorder.setCaptureRate(3);
            //recorder.setMaxDuration(5 * 1000);
            //recorder.setVideoSize(1920,1080);
            //recorder.setPreviewDisplay(mHolder.getSurface());
            Log.e("state","before trying to record");
            try {
                recorder.prepare();
                //Thread.sleep(1000);
                recorder.start(); //Recording starts here
                Log.e("state","recording started");

            } catch (IOException e) {
                e.printStackTrace();

            }
            recording = Boolean.TRUE;
            buttonHandler(); //Change Text of Button

        } else {
            recorder.stop(); //stop recording
            recorder.reset();
            Log.e("state","recording has been stopped4");
            //camera.lock();
            //camera.release();
            frameLayout.setVisibility(View.VISIBLE);
            showCamera.startShowing();

            recording = Boolean.FALSE; //Change Text of Button
            buttonHandler();
        }


    }

    public void useSensor(){
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor rotSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        float[] rotationMatrix = new float[16];
        float[] remappedRotationMatrix = new float[16];


        //TextView textZ = (TextView) findViewById(R.id.textViewZ);
        SensorEventListener rotListener = new SensorEventListener(){
            @Override
            public void onSensorChanged(SensorEvent event) {
                //TODO code gets executed if movetopoint is running but Sensor Event has no updated data
                //SensorManager.getRotationMatrixFromVector(rotationMatrix,);
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                SensorManager.remapCoordinateSystem(rotationMatrix,SensorManager.AXIS_X,SensorManager.AXIS_Z,remappedRotationMatrix);
                SensorManager.getOrientation(remappedRotationMatrix,orientations);
                for(int i = 0; i < 3; i++) {
                    orientations[i] = (float)(Math.toDegrees(orientations[i]));
                }
                textX.setText("X: " + Math.round(orientations[0]));
                textY.setText("Y: " + Math.round(orientations[1])); //Sensor value for tilt
                if (Math.round(orientations[1])==0){
                    textY.setBackgroundColor(Color.GREEN);
                }else{
                    textY.setBackgroundColor(0x00000000);
                }

            }


            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        sensorManager.registerListener(rotListener,rotSensor,SensorManager.SENSOR_DELAY_UI);
    }

    public void setMovingTime(int progress){
        switch(progress){
            case 0:{//10sec
                movingtime = 10;
                txtMovingTime.setText("Capture Time:10s");
                break;
            } case 1:{//30s
                movingtime = 30;
                txtMovingTime.setText("Capture Time: 30s");
                break;
            }case 2:{//1min
                movingtime = 60;
                txtMovingTime.setText("Capture Time: 1min");
                break;
            }case 3:{//3min
                movingtime = 180;
                txtMovingTime.setText("Capture Time: 3min");
                break;
            }case 4:{//10min
                movingtime = 600;
                txtMovingTime.setText("Capture Time: 10min");
                break;
            }case 5:{//20min
                movingtime = 1200;
                txtMovingTime.setText("Capture Time: 20min");
                break;
            }case 6: {//40min
                movingtime = 2400;
                txtMovingTime.setText("Capture Time: 40min");
                break;
            }case 7:{//1h
                movingtime = 3600;
                txtMovingTime.setText("Capture Time: 1h");
                break;
            }case 8:{//1,5h
                movingtime = 5400;
                txtMovingTime.setText("Capture Time: 1.5h");
                break;
            }case 9:{//2h
                movingtime = 7200;
                txtMovingTime.setText("Capture Time: 2h");
                break;
            }case 10:{//2,5h
                movingtime = 9000;
                txtMovingTime.setText("Capture Time: 2.5h");
                break;
            }
        }
    }


    //UI thread
    private class ConnectBTTime extends AsyncTask<Void, Void, Void> {
        //high probability connection was successful
        private boolean ConnectSuccess = true;

        @Override
        protected void onPreExecute()
        {
            //show progressDialog on screen
            progress = ProgressDialog.show(Timelapse.this, "Connecting...",
                    "Please wait!",true);
        }
        @Override
        protected Void doInBackground(Void... devices)
        {
            try
            {
                //connect if socket is not used or connection flag is not set
                if(/*btSocket == null ||*/ !isBtConnected)
                {   Log.e("state", " bt if");;
                    //get device bluetooth adapter
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    //connects to address given from deviceList intent
                    BluetoothDevice btDevice = myBluetooth.getRemoteDevice(address);
                    //create a RfComm connection
                    btSocket = btDevice.createInsecureRfcommSocketToServiceRecord(myUUID);
                    //end discovering bt modules
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    //start connection over socket
                    btSocket.connect();
                    mmInputStream = btSocket.getInputStream();
                    Looper.prepare();
                    beginListenForData();
                }
            }
            catch (IOException e)
            {
                //if an error occurs the flag will be set to 0
                ConnectSuccess = false;
                Log.e("state", " bt failure");
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);
            if(!ConnectSuccess)
            {
                msg("Connection Failed. Pls try again.");
                //finish();
            }
            else
            {
                msg("Connected");
                isBtConnected = true;
            }

            if ((progress != null) && progress.isShowing()) {
                progress.dismiss();
            }
        }
    }

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(Timelapse.this,s,Toast.LENGTH_LONG).show();
    }

    public void sendPosition(){
        int xSteps = 0, ySteps = 0, xJoy = 0, yJoy = 0, xSpeed = speed_ratio*10, ySpeed = speed_ratio*10;
        String xDir = "0", yDir = "0";
        xJoy = joystickPan.getNormalizedX();
        yJoy = joystickPan.getNormalizedY();
        if(xJoy < 50){
            xSteps = 50 - xJoy;
            //left
            xDir = "0";
        }
        if(yJoy < 50){
            ySteps = 50 - yJoy;
            yDir = "0";
        }
        if(xJoy > 50){
            xSteps = xJoy - 50;
            //left
            xDir = "1";
        }
        if(yJoy > 50){
            ySteps = yJoy - 50;
            yDir = "1";
        }

        String msgXY =  String.format("%s%05d%04d%s%05d%04d4",xDir,xSteps,xSpeed,yDir,ySteps,ySpeed);
       // String msgXY =  String.format("%s%05d%s%05d",xDir,xSteps,yDir,ySteps);
        //msg(msgXY);
        Log.e("Output string", msgXY);
        textBtOutput.setText("Out:" + msgXY);
        //msg("try to send joystick position");
        BluetoothSendString(msgXY);
    }

    private void BluetoothSendString(String s){
        if (btSocket!=null)
        {
            try
            {

                btSocket.getOutputStream().write(s.getBytes());
                Log.e("bluetooth string", s);
                textBtOutput.setText("Out:" + s);
                stringsent = true;
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    public void moveToPoint(Point p, int time,String msg) throws InterruptedException {
        int delta_x = 0, delta_y = 0 , y_ratio = 111, xSteps = 0, ySteps = 0, xOutPutSteps= 0, yOutPutSteps=0; //34 and 111 steps per degree
        String xDir = "0", yDir = "0";
        float x_ratio = (float) 33.77;
        int xSpeed, ySpeed = 0;
        int current_x_angle = Math.round(orientations[0]); //current orientations
        int current_y_angle = Math.round(orientations[1]);


        //get delta of orientations
        delta_x = p.x_angle - current_x_angle;
        delta_y = p.y_angle - current_y_angle;
        Log.e("state","MoveToPoint delta x: " + delta_x + " delta y: " + delta_y);

        //convert from angle to steps
        xSteps = Math.abs(Math.round(delta_x * x_ratio));
        ySteps = Math.abs(delta_y * y_ratio);

        //get directions
        if (delta_x > 0){
            xDir = "0";
        }else{
            xDir = "1";
        }
        if (delta_y > 0){
            yDir = "0";
        }else{
            yDir = "1";
        }

        //calculate Speeds in step/s at a given time in seconds
        if (time == 0){
            //as fast as possible
            xSpeed = 1000;
            ySpeed = 1000;
        }else{
            //speed according to range of steps and available time
            xSpeed = Math.round(xSteps/time);
            ySpeed = Math.round(ySteps/time);
            //xSpeed = 25;
            //ySpeed = 25;

        }

        yOutPutSteps = ySteps;
        xOutPutSteps = xSteps;
        String msgXY =  String.format("%s%05d%04d%s%05d%04d%s",xDir,xOutPutSteps,xSpeed,yDir,yOutPutSteps,ySpeed,msg);
        stringsent = false;
        BluetoothSendString(msgXY);



    }

    void beginListenForData(){
        //final Handler handler = new Handler();
        final Handler handlerInput = new Handler(Looper.getMainLooper());
        final byte delimiter = 78; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    Log.e("Input string", "delimiter correct");
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    handlerInput.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            textBtInput.setText("In:" + data);
                                            Log.e("Input string", data);

                                            try {
                                                handleResponse(data);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });

                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    public void handleResponse(String data) throws InterruptedException {
        if (automaticStep > 0){
            try {
                handleAutomatic();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        else{
            if (allPointsSet || ABSet) {
                moveToPoint(destination, 0, "0");
            }
        }
    }

    public void handleAutomatic() throws InterruptedException, IOException {
        int ab_time, bc_time; //time in seconds from a to b and from b to c
        //TODO movingtime in settings tab not hard coded here

        if (ABCSet){
            //move from a to b and c
            ab_time = Math.round((speed_ratio*movingtime)/100);
            bc_time = movingtime - ab_time;
        }else{
            //move from a to b
            ab_time = movingtime;
            bc_time = 0;
        }
        switch(automaticStep){
            case 0: {
                //move to A
                moveToPoint(pointA, 0, "1");
                automaticStep = 1;
                //calculate ab and bc time

                break;
            } case 1:{
                //error check Point A
                moveToPoint(pointA, 0, "1");
                automaticStep = 2;
                break;
            }

            case 2: {

                //start recording
                startTimelapse();
                msg("Recording started");


                //move to b, speed
                moveToPoint(pointB, ab_time, "2");
                automaticStep = 3;
                break;
            }

            case 3: {
                //error check Point B

                moveToPoint(pointB, ab_time, "2");
                automaticStep = 4;
                break;
            }
            case 4: {
                //error check Point B

                moveToPoint(pointB, ab_time,"2");
                automaticStep = 5;
                break;
            }
            case 5: {
                //error check Point B

                moveToPoint(pointB, ab_time,"2");
                if (ABCSet){
                    //move to point C
                    automaticStep = 6;
                }else{
                    //stop recording
                    automaticStep = 9;
                }
                break;
            }
            case 6: {
                //move to point C

                moveToPoint(pointC, bc_time,"2");
                automaticStep = 7;
                break;
            }
            case 7: {
                //error check Point  C

                moveToPoint(pointC, 0,"2");
                automaticStep = 9;
                break;
            }

//            case 8: {
//                //error check Point  C
//
//                moveToPoint(pointC, bc_time, "3");
//                automaticStep = 9;
//                break;
//            }


            case 9: {

                //stop recording
                startTimelapse();
                msg("Recording stopped");

                //reset handleAutomatic
                automaticStep = 0;

                //reset all points
                resetPoints();

                break;
            }
        }




    }

    public void resetPoints(){
        allPointsSet = false;
        ABSet = false;
        ABCSet = false;
        pointA.reset();
        btnA.setBackgroundResource(R.drawable.button_background_off);
        pointB.reset();
        btnB.setBackgroundResource(R.drawable.button_background_off);
        pointC.reset();
        btnC.setBackgroundResource(R.drawable.button_background_off);

    }

    public void times(){
        //TODO calculate time between automatic steps to know how much time is left --> set speed accordingly

        SimpleDateFormat format = new SimpleDateFormat("ss");
        Date startTime = new Date();
        Date endTime = new Date();


        //long mills = startTime.getTime() - endTime.getTime();
        //int hours = millis/(1000 * 60 * 60);
    }


}