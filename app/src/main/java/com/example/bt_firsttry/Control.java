package com.example.bt_firsttry;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class Control extends AppCompatActivity {

    //initialisation
    Button On, Off, Discnt;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private JoystickView joystickControl;
    private boolean isBtConnected = false;
    //set UUID
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private TextView mTextViewAngleLeft;
    private TextView mTextViewStrengthLeft;
    private TextView mTextViewAngleSelf;
    private TextView mTextViewStrengthSelf;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set View
        setContentView(R.layout.activity_control);


        Intent newInt = getIntent();
        address = newInt.getStringExtra(DeviceList.EXTRA_ADDRESS);

        //call the widgets
        On = (Button)findViewById(R.id.btnOn);
        Off = (Button)findViewById(R.id.btnOff);
        Discnt = (Button)findViewById(R.id.btnDscn);

        //call function to connect bluetooth
        new ConnectBT().execute();

        //clicklistener for buttons to send data to HC module
        On.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //call function to turn LED on
                turnOnLed();
            }
        });

        Off.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //call function to turn LED off
                turnOffLed();
            }
        });

        Discnt.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Disconnect(); //close connection
            }
        });

        mTextViewAngleLeft = (TextView) findViewById(R.id.textView_angle_left);
        mTextViewStrengthLeft = (TextView) findViewById(R.id.textView_strength_left);

        mTextViewAngleSelf = (TextView) findViewById(R.id.textView_angle_self);
        mTextViewStrengthSelf = (TextView) findViewById(R.id.textView_strength_self);

        /*JoystickView joystickLeft = (JoystickView) findViewById(R.id.joystickView_left);
        joystickLeft.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                mTextViewAngleLeft.setText(angle + "°");
                mTextViewStrengthLeft.setText(strength + "%");
            }
        });*/
        joystickControl = (JoystickView) findViewById(R.id.joystickSelf);
        joystickControl.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                mTextViewAngleSelf.setText(angle + "°");
                mTextViewStrengthSelf.setText(strength + "%");
            }
        });

    }

    // fast way to call Toast
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    private void Disconnect()
    {
        //if btSocket is busy, close connection
        if(btSocket != null)
        {
            try
            {
                btSocket.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                msg("Error");
            }
        }
        //delete application return to device list layout
        finish();
    }
    //send a 0 over bluetooth to turn off LED
    private void turnOffLed()
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("0".toString().getBytes());
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }
    //send a 1 over bluetooth to turn on LED
    private void turnOnLed()
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("1".toString().getBytes());
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    //UI thread
    private class ConnectBT extends AsyncTask<Void, Void, Void>
    {
        //high probability connection was successful
        private boolean ConnectSuccess = true;

        @Override
        protected void onPreExecute()
        {
            //show progressDialog on screen
            progress = ProgressDialog.show(Control.this, "Connecting...",
                    "Please wait!");
        }
        @Override
        protected Void doInBackground(Void... devices)
        {
            try
            {
                //connect if socket is not used or connection flag is not set
                if(/*btSocket == null ||*/ !isBtConnected)
                {
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
                finish();
            }
            else
            {
                msg("Connected");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }
}