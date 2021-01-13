package com.example.bt_firsttry;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class DeviceList extends AppCompatActivity {

    //widgets
    Button btnPaired;
    Button startTimelapse;
    ListView deviceList;

    //Bluetooth
    //myBluetooth represents bluetooth module of device
    private BluetoothAdapter myBluetooth = null;
    //creates a Set/collection of class Bluetooth device representing paired devices
    private Set<BluetoothDevice> pairedDevices;
    //address of selected device
    public static String EXTRA_ADDRESS = "device_address";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list_v2);

        //calling widgets
        btnPaired =(Button)findViewById(R.id.btnDevices);
        startTimelapse = (Button)findViewById(R.id.startTimelapse);
        deviceList = (ListView)findViewById(R.id.listDevices);

        //get device bluetooth adapter
        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        //look if it is has one and if it isnt available try to enable it
        checkBluetoothEnabled();

        //pairedDevicesList();

        //OnClickListener for btnPaired
        btnPaired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                //if clicked call function
                checkBluetoothEnabled();
                //pairedDevicesList();
            }
        });
        startTimelapse.setOnClickListener(new View.OnClickListener() {
            //@RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                //start Timelapse
                Intent i = new Intent(getApplicationContext(), Control.class);
                startActivity(i);
                overridePendingTransition(R.anim.zoom_in,R.anim.static_animation);

            }
        });

        int MyVersion = Build.VERSION.SDK_INT;
        if (MyVersion > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (!checkIfAlreadyHavePermission()) {
                requestForSpecificPermission();
            }
        }
    }


    //function to check if a Bluetooth module is enabled, otherwise try to turn it on and show paired devices List
    private void checkBluetoothEnabled(){
        if(myBluetooth == null) {

            //show message that the device has no bluetooth adapter
            Toast.makeText(getApplicationContext(), "No Bluetooth Adapter available",
                    Toast.LENGTH_LONG).show();

            //finish apk
            finish();
        }
        //if adapter is disabled ask user to turn it on
        else if(!myBluetooth.isEnabled()){
            //create intent to enable Bluetooth
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon,1);
        }
        if(myBluetooth.isEnabled()){
            pairedDevicesList();
        }
    }


    //function pairedDevicesList shows paired Devices on screen
    private void pairedDevicesList()
    {
        //store paired Devices into Set
        pairedDevices = myBluetooth.getBondedDevices();
        //create list which stores devices
        ArrayList list = new ArrayList();

        if(pairedDevices.size() > 0)
        {   //for each bluetoothDevice in paired Devices do
            for(BluetoothDevice bt : pairedDevices)
            {
                list.add(bt.getName() + "\n" + bt.getAddress());
            }

        }
        else
        {
            //toast message
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.",
                    Toast.LENGTH_LONG).show();
        }

        //
        final ArrayAdapter adapter = new ArrayAdapter(this,
                R.layout.device_list_row, list);
        deviceList.setAdapter(adapter);
        deviceList.setOnItemClickListener(myListClickListener);

    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener()
    {

        @Override
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            //get the devices MAC address, meaning the last 17chars in the view
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            //Make an intent to start next activity
            Intent i = new Intent(DeviceList.this, Timelapse.class);

            //add address to intent
            i.putExtra(EXTRA_ADDRESS, address);
            //Change activity
            startActivity(i);
            overridePendingTransition(R.anim.zoom_in,R.anim.static_animation);



        }
    };

    //check if permission are already granted
    private boolean checkIfAlreadyHavePermission() {
        int checkResultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int checkResultStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int checkResultAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (checkResultCamera == PackageManager.PERMISSION_GRANTED) {
            if (checkResultStorage == PackageManager.PERMISSION_GRANTED) {
                if (checkResultAudio == PackageManager.PERMISSION_GRANTED) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    //Request the permissions
    private void requestForSpecificPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA ,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
    }

    //get the result of the request
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 101:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //granted
                } else {
                    //not granted
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}