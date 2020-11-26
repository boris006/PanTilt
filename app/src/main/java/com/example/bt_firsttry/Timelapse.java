package com.example.bt_firsttry;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Timelapse extends AppCompatActivity {
    Camera camera;
    FrameLayout frameLayout; //for camera preview
    SurfaceView mSurfaceView; //for preview while recording timelapse
    SurfaceHolder mHolder; //holder for surfaceView
    ShowCamera showCamera; //Class for camera preview on framelayout
    MediaRecorder recorder;
    float[] orientations = new float[3]; //orientation 1 tilt sensor
    int i = 0;
    Boolean recording = Boolean.FALSE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timelapse_layout);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        useSensor();
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);

        mHolder = mSurfaceView.getHolder();
        frameLayout = (FrameLayout) findViewById(R.id.framelayout);
        //mHolder.addCallback((SurfaceHolder.Callback) this);
        //mHolder.addCallback(this);
        //mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //open camera
        /*camera = Camera.open();
        showCamera = new ShowCamera(this, camera);
        frameLayout.addView(showCamera);
        frameLayout.setVisibility(View.VISIBLE);
        //showCamera.startShowing();
        Log.e("state","screen started");
        //MediaRecorder recorder = new MediaRecorder();*/


    }
    @Override
    protected void onResume(){
        super.onResume();
        if(camera == null) {
            camera = Camera.open();
            showCamera = new ShowCamera(this, camera);
            frameLayout.addView(showCamera);
            frameLayout.setVisibility(View.VISIBLE);
            //showCamera.startShowing();
            Log.e("state", "screen started");
        }
    }
    @Override
    protected void onPause(){
        super.onPause();
        if(camera != null){
            camera.release();
            camera = null;
        }
    }

//    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
//        @Override
//        public void onPictureTaken(byte[] data, Camera camera) {
//            File picture_File = getOutputMediafile();
//
//            try {
//                FileOutputStream fos = new FileOutputStream(picture_File);
//                fos.write(data);
//                fos.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            camera.startPreview();
//           /* if(picture_File == null){
//                return;
//            }else{
//                try{
//                    FileOutputStream fos = new FileOutputStream(picture_File);
//                    fos.write(data);
//                    fos.close();
//                    camera.startPreview();
//                }catch(IOException e){
//                    e.printStackTrace();
//                }
//
//            }*/
//        }
//    };

    private File getOutputMediafile() {
        String state = Environment.getExternalStorageState();
        //File folder_gui = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "temp");
        //folder_gui.mkdirs();
        String formatted_date = new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss").format(new Date(System.currentTimeMillis()));
        Log.i("state", formatted_date);
        File outputFile = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "temp", "IMG" + formatted_date + ".jpeg");
        return outputFile;
      /*if(!state.equals(Environment.MEDIA_MOUNTED)){
          return null;
      }else{
          File folder_gui = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "temp");

          if(!folder_gui.exists()){
              folder_gui.mkdir();
          }

          File outputFile = new File(folder_gui,"temp.jpg");
          return outputFile;
      }*/
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

//    public void captureImage(View v){
//        if(camera!= null){
//            camera.takePicture(null,null,mPictureCallback);
//        }
//    }

    public void buttonHandler() {
        Button timelapse = (Button) findViewById(R.id.timelapse);
        if (recording == Boolean.FALSE) {
            timelapse.setText("Timelapse Starten");
        } else {
            timelapse.setText("Timelapse Stoppen");
        }


    }

    public void startTimelapse(View v) throws IOException {
        if (recording == Boolean.FALSE) { //not recording yet but starting
            //MediaRecorder recorder = new MediaRecorder();
            showCamera.stopShowing();   //Turn Off Camera Preview
            frameLayout.setVisibility(View.INVISIBLE); //Set Framelayout invisble, to show recording of timelapse
            Log.e("state","trying started1");
            Camera.Parameters params = camera.getParameters();
            String fileName = getOutputVideoFilePath(); //Video file path with name
            if (recorder == null)
                recorder = new MediaRecorder();

            if (mSurfaceView.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) { //set orientation of preview and recorded video
                params.set("orientation", "landscape");
                camera.setDisplayOrientation(0);
                params.setRotation(0);
            } else {
                params.set("orientation", "portrait");
                camera.setDisplayOrientation(90);
                params.setRotation(270);
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
            recorder.setCaptureRate(1);
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

        TextView textX = (TextView) findViewById(R.id.textViewX);
        TextView textY = (TextView) findViewById(R.id.textViewY);
        TextView textZ = (TextView) findViewById(R.id.textViewZ);
        SensorEventListener rotListener = new SensorEventListener(){
            @Override
            public void onSensorChanged(SensorEvent event) {
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
                    textY.setBackgroundColor(Color.WHITE);
                }
                textZ.setText("Z: " + Math.round(orientations[2]));
            }


            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        sensorManager.registerListener(rotListener,rotSensor,SensorManager.SENSOR_DELAY_UI);
    }

}