package com.example.bt_firsttry;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.List;

public class ShowCamera extends SurfaceView implements SurfaceHolder.Callback {
    Camera cameraSur;
    SurfaceHolder holder;


    public ShowCamera(Context context, Camera cameraSur) {
        super(context);
        this.cameraSur = cameraSur;
        holder = getHolder();
        holder.addCallback(this);

    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        //setParameters();
        //startShowing();
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        Log.e("state", " surface created");
        setParameters();
        startShowing();

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        this.cameraSur.stopPreview();
        cameraSur.release();
        Log.e("state", " surface destroyed");
    }

    public void startShowing(){
        try{
            cameraSur.setPreviewDisplay(holder);
            cameraSur.startPreview();
            //camera.unlock();
        }catch(IOException e ){
            e.printStackTrace();
        }
    }
    public void stopShowing(){
        cameraSur.stopPreview();
    }
    public void setParameters(){
        Camera.Parameters params = cameraSur.getParameters();
        List<Camera.Size> sizes = params.getSupportedPictureSizes();
        Camera.Size mSize = null;
        int maxwidth = 0;
        int maxheight = 0;
        for(Camera.Size size : sizes){

            Log.i("state","width: " + size.width + " height: " + size.height);

            if ((size.width > maxwidth) & (size.height > maxheight)){
                maxwidth = size.width;
                maxheight = size.height;
                mSize = size;
            }



        }
        Log.i("state","m width: " + mSize.width + " m height: " + mSize.height);
        //change orientation

        if(this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE){
            params.set("orientation","portrait");
            cameraSur.setDisplayOrientation(90);
            params.setRotation(90);
        }else{
            params.set("orientation","landscape");
            cameraSur.setDisplayOrientation(0);
            params.setRotation(180);
        }

        params.setPictureSize(mSize.width,mSize.height);
        if (params.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        cameraSur.setParameters(params);
//        try{
//            camera.setPreviewDisplay(holder);
//            camera.startPreview();
//            //camera.unlock();
//        }catch(IOException e ){
//            e.printStackTrace();
//        }

    }
}
