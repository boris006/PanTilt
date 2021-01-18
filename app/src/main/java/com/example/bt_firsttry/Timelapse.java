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
import android.os.CountDownTimer;
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
import android.widget.ProgressBar;
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

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class Timelapse extends AppCompatActivity {
    //init Camera and Timelapse
    Camera camera; //camera
    FrameLayout frameLayout; //for camera preview
    SurfaceView mSurfaceView; //for preview while recording timelapse
    SurfaceHolder mHolder; //holder for surfaceView
    ShowCamera showCamera; //Class for camera preview on framelayout
    MediaRecorder recorder;
    int captureRate; //capture Rate of the camera
    SwitchCompat switchCapture;
    Boolean recording = FALSE;

    //Joystick
    JoystickView joystickPan; //joystick for manual panTilt adjustments
    CustomView crossView; //projects a cross to visualise joystick data on screen

    //init menu components
    Button btnJoystick, btnInfo, btnSettings, btnMotion, btnBlock;
    boolean iIsShown = false, settingsIsShown = false, isBlocked = false;
    LinearLayout menuSettings, menuBar;

    //sensor
    TextView textX, textY;
    float[] orientations = new float[3]; //orientation 1 tilt sensor

    //bluetooth
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //set UUID
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    String address = null;
    //Input stream;
    InputStream mmInputStream;
    TextView textBtOutput, textBtInput;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;


    //Automatic Mode Point Initialisation
    Boolean allPointsSet = false, ABSet = false, ABCSet = false;
    Button btnA, btnB, btnC, btnTimeLapse;
    Point pointA, pointB, pointC, destination;
    TextView a_x, a_y, b_x, b_y, c_x, c_y;
    int automaticStep;

    //SeekBar Speed Moving Time
    TextView textMovingTime, textSpeedRatio;
    int speedRatio = 50;
    int movingTime = 60;     //overall moving time in seconds (ab + bc = movingtime)
    SeekBar sBMovingTime;
    SeekBar sBSpeedRatio;


    //Progressbar
    ProgressBar myProgressBar;
    CountDownTimer myProgressCountdown;
    int timerProgress = 0;

    //initialize Point class
    class Point{
        int x_angle;
        int y_angle;
        boolean isSet;
        String name;

        public Point(){//constructor
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
        Log.e("state","on create before bt");

        //Bluetooth
        Intent newInt = getIntent();
        address = newInt.getStringExtra(DeviceList.EXTRA_ADDRESS);
        Log.e("state","on create after intent bt");
        new ConnectBTTime().execute();

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
        },100); // send interval in ms

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

        //set and reset A
        btnA = (Button)findViewById(R.id.btn_a);
        a_x = (TextView) findViewById(R.id.a_x);
        a_y = (TextView) findViewById(R.id.a_y);
        a_x.setText("X_a: N/S");
        a_y.setText("Y_a: N/S");
        btnA.setBackgroundResource(R.drawable.button_background_off);
        btnA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!pointA.isSet){
                    pointA.setFromOrientation();
                    a_x.setText("X_a:" + pointA.x_angle);
                    a_y.setText("Y_a:" + pointA.y_angle);
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
                a_y.setText("Y_a: N/S");
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
        b_y.setText("Y_b: N/S");
        btnB.setBackgroundResource(R.drawable.button_background_off);
        btnB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!pointB.isSet){
                    pointB.setFromOrientation();
                    b_x.setText("X_b:" + pointB.x_angle);
                    b_y.setText("Y_b:" + pointB.y_angle);
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
                b_y.setText("Y_b: N/S");
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
        c_y.setText("Y_c: N/S");
        btnC.setBackgroundResource(R.drawable.button_background_off);
        btnC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!pointC.isSet){
                    pointC.setFromOrientation();
                    c_x.setText("X_c:" + pointC.x_angle);
                    c_y.setText("Y_c:" + pointC.y_angle);
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
                c_y.setText("Y_c: N/S");
                ABCSet = false;
                btnC.setBackgroundResource(R.drawable.button_background_off);
                return true;
            }
        });


        //Start automatic Mode with time lapse Button
        automaticStep = 0;
        btnTimeLapse = (Button)findViewById(R.id.timelapse);
        //init seek Bars
        sBMovingTime = findViewById(R.id.sBMovingTime);
        sBSpeedRatio = findViewById(R.id.sBSpeedRatio);
        btnTimeLapse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pointA.isSet && pointB.isSet){
                    if(automaticStep == 0){
                        try {
                            handleAutomatic();
                            btnA.setEnabled(FALSE);
                            btnB.setEnabled(FALSE);
                            btnC.setEnabled(FALSE);
                            btnMotion.setEnabled(FALSE);
                            switchCapture.setEnabled(FALSE);
                            joystickPan.setEnabled(FALSE);
                            sBMovingTime.setEnabled(FALSE);
                            sBSpeedRatio.setEnabled(FALSE);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }else{
                        automaticStep = 0;
                        myProgressBar.setVisibility(View.GONE);
                        myProgressCountdown.cancel();
                        timerProgress = 0;
                        btnA.setEnabled(TRUE);
                        btnB.setEnabled(TRUE);
                        btnC.setEnabled(TRUE);
                        btnMotion.setEnabled(TRUE);
                        switchCapture.setEnabled(TRUE);
                        joystickPan.setEnabled(TRUE);
                        sBMovingTime.setEnabled(TRUE);
                        sBSpeedRatio.setEnabled(TRUE);
                        if(recording == TRUE){
                            recorder.stop(); //stop recording
                            recorder.reset();
                            Log.e("state","recording has been stopped4");
                            frameLayout.setVisibility(View.VISIBLE);
                            showCamera.startShowing();
                            recording = Boolean.FALSE; //Change Text of Button
                            buttonHandler();
                            msg("Recording stopped");
                        }

                    }
                }else{
                    msg("Please set points");
                }

            }
        });

        //Show or hide Joystick
        btnJoystick = (Button)findViewById(R.id.btn_joy);
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
                    btnJoystick.setBackgroundResource(R.drawable.joy_icon_white);
                }
                else{
                    joystickPan.setVisibility(View.VISIBLE);
                    crossView.setVisibility(View.VISIBLE);
                    btnJoystick.setBackgroundResource(R.drawable.joy_icon_yellow);
                }
            }
        });

        //Show or Hide Info layout
        btnInfo = (Button)findViewById(R.id.btn_i);
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
        btnInfo.setBackgroundResource(R.drawable.info_icon_white);
        btnInfo.setOnClickListener(new View.OnClickListener() {
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
                    btnInfo.setBackgroundResource(R.drawable.info_icon_white);
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
                    btnInfo.setBackgroundResource(R.drawable.info_icon_yellow);
                    iIsShown = true;
                }
            }
        });

        //SeekBar and text
        textMovingTime = (TextView) findViewById(R.id.txtMovingSpeed);
        textSpeedRatio = (TextView) findViewById(R.id.txtSpeedRatio);
        sBMovingTime.setOnSeekBarChangeListener(seekBarTimeChangeListener);
        sBSpeedRatio.setOnSeekBarChangeListener(seekBarRatioChangeListener);
        setMovingTime(3);

        //Switch for Timelapse Capture rate
        switchCapture = (SwitchCompat) findViewById(R.id.switchCapture);
        switchCapture.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position
                if(isChecked){
                    captureRate = 3;
                    Log.e("Switch","Timelapse on");
                }else{
                    captureRate = 30;
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
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(settingsIsShown){
                    menuSettings.setVisibility(View.GONE);
                    btnSettings.setBackgroundResource(R.drawable.settings_icon_whit);
                    settingsIsShown = false;
                }
                else{
                    menuSettings.setVisibility(View.VISIBLE);
                    btnSettings.setBackgroundResource(R.drawable.settings_icon_yellow);
                    settingsIsShown = true;
                }
            }
        });

        //Block irrelevant objects
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
                    switchCapture.setEnabled(true);

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
                    switchCapture.setEnabled(false);
                }
            }
        });

        //progress bar
        myProgressBar = (ProgressBar) findViewById(R.id.progressBarTimer);
        myProgressBar.setProgress(0);
        myProgressBar.setVisibility(View.GONE);
        myProgressCountdown = new CountDownTimer(movingTime * 1000,1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                timerProgress++;
                myProgressBar.setProgress((int)timerProgress*100/(movingTime));

            }

            @Override
            public void onFinish() {
                //Do what you want
                timerProgress++;
                myProgressBar.setProgress(100);
            }
        };


    }

    @Override
    protected void onResume(){//what happens when you tap in or enter application
        super.onResume();
        Log.e("state", " on resume ");
        if(camera == null) {
            Log.e("state", " on resume camera null ");
            camera = Camera.open();
            showCamera = new ShowCamera(this, camera);
            frameLayout.addView(showCamera);
            frameLayout.setVisibility(View.VISIBLE);

        }

    }
    @Override
    protected void onPause() {//what happens when you tap out or leave application
        super.onPause();
        myProgressBar.setVisibility(View.GONE);
        timerProgress = 0;
        myProgressCountdown.cancel();

        if (recording = Boolean.TRUE) {//stop recording if recording

            recording = FALSE; //Change Text of Button
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

    //hide nav bar
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
            speedRatio = progress + 10;
            textSpeedRatio.setText("Time ratio at B: " + speedRatio + "%");
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    //show Pop up
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


    public void buttonHandler() {//handler start Timelapse button
        //Button timelapse = (Button) findViewById(R.id.timelapse);
        if (recording == FALSE) {
            //btn_timelapse.setText("Timelapse Starten");
            btnTimeLapse.setBackgroundResource(R.drawable.icon_camera_white);
        } else {
            //btn_timelapse.setText("Timelapse Stoppen");
            btnTimeLapse.setBackgroundResource(R.drawable.icon_camera_yellow);
        }


    }

    public void startTimelapse() throws IOException {
        //
        if (recording == FALSE) { //not recording yet but starting
            showCamera.stopShowing();   //Turn Off Camera Preview
            frameLayout.setVisibility(View.INVISIBLE); //Set Framelayout invisble, to show recording of timelapse
            Log.e("state","trying started1");
            Camera.Parameters params = camera.getParameters();
            String fileName = getOutputVideoFilePath(); //Video file path with name
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

            recorder.setOutputFile(fileName);
            recorder.setCaptureRate(captureRate);
            Log.e("state","before trying to record");
            try {
                recorder.prepare();
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
            frameLayout.setVisibility(View.VISIBLE);
            showCamera.startShowing();

            recording = FALSE; //Change Text of Button
            buttonHandler();
        }
    }

    public void useSensor(){//use of Type Rotation Vector to calculate orientation
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor rotSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        float[] rotationMatrix = new float[16];
        float[] remappedRotationMatrix = new float[16];

        SensorEventListener rotListener = new SensorEventListener(){
            @Override
            public void onSensorChanged(SensorEvent event) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                SensorManager.remapCoordinateSystem(rotationMatrix,SensorManager.AXIS_X,SensorManager.AXIS_Z,remappedRotationMatrix);
                SensorManager.getOrientation(remappedRotationMatrix,orientations);
                for(int i = 0; i < 3; i++) {
                    orientations[i] = (float)(Math.toDegrees(orientations[i]));
                }
                textX.setText("X: " + Math.round(orientations[0])); //Sensor value for pan
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
                movingTime = 10;
                textMovingTime.setText("Capture Time:10s");
                break;
            } case 1:{//30s
                movingTime = 30;
                textMovingTime.setText("Capture Time: 30s");
                break;
            }case 2:{//1min
                movingTime = 60;
                textMovingTime.setText("Capture Time: 1min");
                break;
            }case 3:{//3min
                movingTime = 180;
                textMovingTime.setText("Capture Time: 3min");
                break;
            }case 4:{//10min
                movingTime = 600;
                textMovingTime.setText("Capture Time: 10min");
                break;
            }case 5:{//20min
                movingTime = 1200;
                textMovingTime.setText("Capture Time: 20min");
                break;
            }case 6: {//40min
                movingTime = 2400;
                textMovingTime.setText("Capture Time: 40min");
                break;
            }case 7:{//1h
                movingTime = 3600;
                textMovingTime.setText("Capture Time: 1h");
                break;
            }case 8:{//1,5h
                movingTime = 5400;
                textMovingTime.setText("Capture Time: 1.5h");
                break;
            }case 9:{//2h
                movingTime = 7200;
                textMovingTime.setText("Capture Time: 2h");
                break;
            }case 10:{//2,5h
                movingTime = 9000;
                textMovingTime.setText("Capture Time: 2.5h");
                break;
            }
        }
    }


    //Bluetooth connection thread
    private class ConnectBTTime extends AsyncTask<Void, Void, Void> {
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
                if(!isBtConnected)
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

    // toast simplifier
    private void msg(String s) {

        Toast.makeText(Timelapse.this,s,Toast.LENGTH_LONG).show();
    }


    public void sendPosition(){
        int xSteps = 0, ySteps = 0, xJoy = 0, yJoy = 0, xSpeed = 3000, ySpeed = 3000;
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
        Log.e("Output string", msgXY);
        BluetoothSendString(msgXY);
    }


    private void BluetoothSendString(String s){
        if (btSocket.isConnected())
        {
            try
            {
                btSocket.getOutputStream().write(s.getBytes());
                Log.e("bluetooth string", s);
                textBtOutput.setText("Out:" + s);
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    public void moveToPoint(Point p, int time,String msg) throws InterruptedException {
        int delta_x, delta_y, y_ratio = 111, xSteps, ySteps, xOutPutSteps, yOutPutSteps; //34 and 111 steps per degree
        String xDir, yDir;
        float x_ratio = (float) 33.77;
        int xSpeed, ySpeed;
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

        }

        yOutPutSteps = ySteps;
        xOutPutSteps = xSteps;
        String msgXY =  String.format("%s%05d%04d%s%05d%04d%s",xDir,xOutPutSteps,xSpeed,yDir,yOutPutSteps,ySpeed,msg);
        BluetoothSendString(msgXY);

    }

    //get Bluetooth input
    void beginListenForData(){
        final Handler handlerInput = new Handler(Looper.getMainLooper());
        final byte delimiter = 78; //This is the ASCII code for "N"
        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];

        //create own thread
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)//while running
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);

                            //decode input stream
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    //convert to string
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
                                                //return data to time lapse thread
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
        if (automaticStep > 0){ //automatic mode is active
            try {
                handleAutomatic();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        else{ //Error check if single motion has ended
            if (allPointsSet || ABSet) {
                moveToPoint(destination, 0, "0");
            }
        }
    }

    //time lapse Automatic mode
    public void handleAutomatic() throws InterruptedException, IOException {
        int ab_time, bc_time; //time in seconds from a to b and from b to c

        //calculate moving time
        if (ABCSet){
            //move from a to b and c
            ab_time = Math.round((speedRatio * movingTime)/100);
            bc_time = movingTime - ab_time;
        }else{
            //move from a to b
            ab_time = movingTime;
            bc_time = 0;
        }

        switch(automaticStep){
            case 0: {
                //move to A
                myProgressBar.setVisibility(View.VISIBLE);
                moveToPoint(pointA, 0, "1");
                automaticStep = 1;
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
                myProgressCountdown.start();
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
                myProgressBar.setVisibility(View.GONE);
                myProgressCountdown.cancel();
                timerProgress = 0;
                msg("Recording stopped");

                //reset handleAutomatic
                automaticStep = 0;

                btnA.setEnabled(TRUE);
                btnB.setEnabled(TRUE);
                btnC.setEnabled(TRUE);
                btnMotion.setEnabled(TRUE);
                switchCapture.setEnabled(TRUE);
                joystickPan.setEnabled(TRUE);
                sBMovingTime.setEnabled(TRUE);
                sBSpeedRatio.setEnabled(TRUE);

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
}