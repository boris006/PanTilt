package com.example.bt_firsttry;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ColorMatrix;
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
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bt_firsttry.views.CustomView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import io.github.controlwear.virtual.joystick.android.JoystickView;

//TODO camera focus
//TODO preview mit buttons [xdir][###][ydir][###]
public class Timelapse extends AppCompatActivity {
    Camera camera;
    FrameLayout frameLayout; //for camera preview
    SurfaceView mSurfaceView; //for preview while recording timelapse
    SurfaceHolder mHolder; //holder for surfaceView
    ShowCamera showCamera; //Class for camera preview on framelayout
    MediaRecorder recorder;
    CustomView crossView;

    String address = null;
    JoystickView joystickTime;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    float[] orientations = new float[3]; //orientation 1 tilt sensor
    int i = 0;
    Boolean recording = Boolean.FALSE;
    //set UUID
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    //Automatic Mode Point Initialisation
    Boolean allPointsSet = false, continueMoving = false;
    Button setPoint, moveToA;
    Point pointA, pointB;

    class Point{
        int x_angle;
        int y_angle;
        boolean isSet;

        public Point(){
            x_angle = 0;
            y_angle = 0;
            isSet = false;
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

        //create Joystick
        crossView = (CustomView) findViewById(R.id.CustomView);
        joystickTime = (JoystickView) findViewById(R.id.joystickCamera);
        Log.e("state","on create before joystick");
        joystickTime.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {

                crossView.adjustCross(joystickTime.getNormalizedX(),joystickTime.getNormalizedY());
                sendPosition();

            }
        },100); //TODO send interval in ms


        //Automatic Mode Point Initialisation
        pointA = new Point();
        pointB = new Point();
        //set Points A and B
        setPoint = (Button)findViewById(R.id.setPoint);
        setPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                //has a been initialised?
                if (pointA.isSet == false){
                    pointA.setFromOrientation();
                    setPoint.setText("Punkt B setzen");
                }else{
                    if (pointB.isSet == false){
                        pointB.setFromOrientation();
                        setPoint.setText("A & B gesetzt");
                        allPointsSet = true;
                    }else{
                        pointA.reset();
                        pointB.reset();
                        setPoint.setText("Punkt A setzen");
                        allPointsSet = false;
                    }

                }

            }
        });
        //Move To Point A to Initialize
        moveToA = (Button)findViewById(R.id.moveToA);
        moveToA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (allPointsSet){
                    //TODO Hier abfragen ob Smartphone in die Halterung gesetzt wurde
                    try {
                        moveToPoint(pointA,100);
                        //msg("Move To Point wird ausgeführt");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }else{
                    msg("Fehler: Punkte A & B müssen gesetzt werden!");
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
        Button timelapse = (Button) findViewById(R.id.timelapse);
        if (recording == Boolean.FALSE) {
            timelapse.setText("Timelapse Starten");
        } else {
            timelapse.setText("Timelapse Stoppen");
        }


    }

    public void startTimelapse(View v) throws IOException {
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
            recorder.setCaptureRate(24);
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

    private void useSensor(){
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor rotSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        float[] rotationMatrix = new float[16];
        float[] remappedRotationMatrix = new float[16];

        TextView textX = (TextView) findViewById(R.id.textViewX);
        TextView textY = (TextView) findViewById(R.id.textViewY);
        //TextView textZ = (TextView) findViewById(R.id.textViewZ);
        SensorEventListener rotListener = new SensorEventListener(){
            @Override
            public void onSensorChanged(SensorEvent event) {
                //TODO not executed if movetopoint is running
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
                    textY.setBackgroundColor(0xFF1D171F);
                }
                if (continueMoving){
                    try {
                        msg("continued moving");
                        continueMoving = false;
                        //TODO steppers not stopping if this is set
                        Thread.sleep(1500);
                        moveToPoint(pointA,100);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //textZ.setText("Z: " + Math.round(orientations[2]));
            }


            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        sensorManager.registerListener(rotListener,rotSensor,SensorManager.SENSOR_DELAY_UI);
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
    private void msg(String s)
    {
        Toast.makeText(Timelapse.this,s,Toast.LENGTH_LONG).show();
    }

    public void sendPosition(){
        //TextView textZ = (TextView) findViewById(R.id.textViewZ);
        int xSteps = 0, ySteps = 0, xJoy = 0, yJoy = 0;
        String xDir = "0", yDir = "0";
        xJoy = joystickTime.getNormalizedX();
        yJoy = joystickTime.getNormalizedY();
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

        String msgXY =  String.format("%s%03d%s%03d",xDir,xSteps,yDir,ySteps);
        //msg(msgXY);
        Log.e("Output string", msgXY);
        //textZ.setText(msgXY);
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
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    public void moveToPoint(Point p, int speed) throws InterruptedException {

        int delta_x = 0, delta_y = 0, x_ratio = 34, y_ratio = 111, xSteps = 0, ySteps = 0; //34 and 111 steps per degree
        String xDir = "0", yDir = "0";
        int current_x_angle = Math.round(orientations[0]); //current orientations
        int current_y_angle = Math.round(orientations[1]);

        //get delta of orientations
        delta_x = p.x_angle - current_x_angle;
        delta_y = p.y_angle - current_y_angle;

        //convert from angle to steps
        xSteps = Math.abs(delta_x * x_ratio);
        ySteps = Math.abs(delta_y * y_ratio);

        //get directions
        if (delta_x > 0){
            xDir = "0";
        }else{
            xDir = "1";
        }
        if (delta_y > 0){
            yDir = "1";
        }else{
            yDir = "0";
        }
        String msgXY =  String.format("%s%03d%s%03d",xDir,xSteps,yDir,ySteps);
        //msg(msgXY);
        //to fit our protocol (max 999 steps)
        if ((xSteps > 999)||(ySteps > 999)){ //out of protocol limits
           if (xSteps > 999){xSteps = 999;}
           if (ySteps > 999){ySteps = 999;}
            msgXY =  String.format("%s%03d%s%03d",xDir,xSteps,yDir,ySteps);
            //textZ.setText(msgXY);
            msg("Try to Send Move from MoveToPoint > 999");
            BluetoothSendString(msgXY);
            continueMoving = true;
            //Thread.sleep(500);

            //useSensor();
            //moveToPoint(pointA,100);
        }else{
            msg("Try to Send Move from MoveToPoint < 999");

            continueMoving = false;
            BluetoothSendString(msgXY);
        }



        //send Over Bluetooth
    }
}