package com.example.bt_firsttry;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set splash layout as default
        setContentView(R.layout.splash);
        //creates new thread
        Thread timerThread = new Thread(){
            //executes runnable in thread... what should happen in thread
            public void run(){
                try{
                    //wait 700millis
                    sleep(1500);
                //catch error from sleep thread
                }catch(InterruptedException e){
                    e.printStackTrace();

                }finally{
                    Intent intent = new Intent(SplashScreen.this,DeviceList.class);
                    startActivity(intent);
                }
            }
        };
        timerThread.start();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //if on Pause is called, go to onDestroy because activity will not be used again
        finish();
    }
}