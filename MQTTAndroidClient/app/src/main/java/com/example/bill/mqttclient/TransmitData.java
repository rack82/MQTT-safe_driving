package com.example.bill.mqttclient;

import android.Manifest;
import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/*For constantly sending data to edge server we use Intent service which has its own Thread*/
public class TransmitData extends IntentService implements SensorEventListener, LocationListener {
    private final IBinder bindObj = new LocalBinder();
    private static final String Tag = TransmitData.class.getSimpleName();
    private MqttObjectClass conObject;
    private MqttConnectOptions options;
    private Sensor Ameter;
    private SensorManager SM;
    private LocationManager LM;
    private Location location;
    private static final int MIN_DISTANCE_CHANGE_FOR_UPDATES = 0;
    private static final int MIN_TIME_BW_UPDATES = 0;
    private float x, y, z;
    private double Latitude, Longitude;
    private List<String[]> strArray = null;
    private JSONObject jsonObj = null;
    private String[] filesList = new String[0];
    private int rand;
    private int freq = 1100;
    private boolean isDisposed = false; //will check if the service should continue working
    private Handler mHandler;



    public TransmitData() {
        super("dataTransmitService");
    }

    /*onHandIntent gets called when MainActivity starts the service*/
    @Override
    protected void onHandleIntent(Intent intent) {

        /*configure and register sensors for obtaining x,y,z data*/
        SM = (SensorManager) getSystemService(SENSOR_SERVICE);
        Ameter = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        SM.registerListener(this, Ameter, SensorManager.SENSOR_DELAY_NORMAL);

        try {
            this.filesList = getAssets().list("");//read all assets csv fies into a list
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*configure location manager for obtaining gps coordinates and check if there is permission to use GPS sensor*/
        LM = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        /*create mqtt connection object*/
        conObject = new MqttObjectClass(getApplicationContext());
        while (conObject == null)
            continue;
        conObject.Connect();//call object method connect
        try {
            Thread.sleep(3000);//allow the connection to establish before subscribe is called other wise we get nullObject exception
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String name = Settings.Secure.getString(getContentResolver(), "bluetooth_name");// get name of the device
        String topic = "android/topic";
        byte[] encodedPayload = new byte[0];// byte array to be sent via mqtt connection
        jsonObj = new JSONObject();// data will be sent using json object
        try {
            Thread.sleep(2000);
            while(1==1) {
                /*isDisposed can be changed from MainActivity using bind and allows us to stop the service*/
                if (isDisposed)
                    break;

                /*get location coordinates*/
                location = LM.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                LM.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                if (location != null){
                    this.Latitude = location.getLatitude();
                    this.Longitude = location.getLongitude();
                }
                rand = new Random().nextInt(filesList.length);// random number for selecting a csv file from assets
                strArray = getStrArray(filesList, rand);// call methos to read the csv data
                if (strArray != null) {
                    /*register the data into our json object */
                    jsonObj.put("x", x);
                    jsonObj.put("z", z);
                    jsonObj.put("y", y);
                    jsonObj.put("longitude", this.Longitude);
                    jsonObj.put("latitude", this.Latitude);
                    jsonObj.put("name", name);
                    jsonObj.put("buffer", new JSONArray(strArray));// entire csv data is saved as JSONArray
                    jsonObj.put("csv fileName", filesList[rand]);
                    while (conObject.isConnected() == false) {
                        conObject.Connect();
                        try {
                            Thread.sleep(3000);//allow the connection to establish before subscribe is called other wise we get nullObject exception
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    conObject.publish(name, topic, encodedPayload, jsonObj); // call object method publish. The conversion from json to byte array happens within the method
                    /*instruct the thread to wait before sending the next data*/
                    Thread.sleep(freq);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /*read csv data from assets folder using streams and return them*/
    public List<String[]> getStrArray(String[] filesList, int rand) {
        InputStream is = null;
        InputStreamReader ir = null;
        String line = "";
        try {
            is = getAssets().open(filesList[rand]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(is != null)
            ir = new InputStreamReader(is);
        else
            return null;
        BufferedReader bu = new BufferedReader(ir);
        List<String[]> TempstrArray = new ArrayList<String[]>();
        try {
            line = bu.readLine();
            while ((line = bu.readLine()) != null){
                String[] row = line.split(",");
                TempstrArray.add(row);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        finally{
            try{
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return TempstrArray;
    }


    /*set frequency method can be called from MainActivity using bind. The default send data freq is 800ms*/
    public void setFreq(int frequency){
        if (frequency < 1100)
            this.freq =1100;
        else
            this.freq = frequency;
    }

    /*Set Disposed can also be called from MainActivity using bind*/
    public void setDisposed(boolean isDisposed){
        this.isDisposed = isDisposed;
    }

    /*sensor changed updates x,y,z every time they change*/
    @Override
    public void onSensorChanged(SensorEvent event) {
        this.x = event.values[0];
        this.y = event.values[1];
        this.z = event.values[2];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onLocationChanged(Location location) {
        if (location != null){
            this.Longitude = location.getLongitude();
            this.Latitude = location.getLatitude();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    /*On bind and Local Binder are used to bind the service with MainActivity*/
    @Override
    public IBinder onBind(Intent intent) {
        return bindObj;
    }
    public class LocalBinder extends Binder {
        TransmitData getService(){
            return TransmitData.this;
        }
    }
}

