package com.example.bill.mqttclient;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.example.bill.mqttclient.TransmitData.LocalBinder;


public class MainActivity extends AppCompatActivity{

    private static final String Tag = MainActivity.class.getSimpleName();
    private Button Submit, Exit;
    private int freq;
    private Intent i;
    private TransmitData TransmitService;
    private boolean isBind = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);// request permissions to allow gps sensor use

        /*register buttons for later use*/
        Submit = findViewById(R.id.submit);
        Exit = findViewById(R.id.exit);
        Exit = findViewById(R.id.exit);

        /*create intent in order to both start and bind to Intent Service "TransmitData"*/
        i = new Intent(this, TransmitData.class);
        if (!isBind)
            bindService(i, serviceCon, Context.BIND_AUTO_CREATE);
        startService(i);

        Submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*by clicking submit we call the SetFreq method in order to change the service's data sending frequency*/
                EditText input = (EditText)findViewById(R.id.freq);
                freq = Integer.parseInt(input.getText().toString());
                TransmitService.setFreq(freq);
                input.setText("");// clears the edit text
            }
        });

        /*when exit button is clicked, it asks the user for exit permission */
        Exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.app_name);
                builder.setIcon(R.mipmap.ic_launcher);
                builder.setMessage("Do you want to exit?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                            /*if the user clicks yes, service's setDisposed is called and allows the service to end the OnHabdleMethod, that way it exits*/
                            public void onClick(DialogInterface dialog, int id) {
                                TransmitService.setDisposed(true);
                                finish();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });
    }
    /*this method handles the gps sensor permission request and response*/
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    // permission denied. Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    /*OnBackPressed is the same as button Exit*/
    @Override
    public void onBackPressed() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.app_name);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setMessage("Do you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        TransmitService.setDisposed(true);
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        unbindService(serviceCon);
    }

    /*Connection object responsible for binding MainActivity and Service*/
    private ServiceConnection serviceCon = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocalBinder binder = (LocalBinder) service;
            TransmitService = binder.getService();
            isBind = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBind = false;
        }
    };
}