package com.example.bill.mqttclient;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;



/*MQttObject class is the connection object for mqtt with edge broker*/

public class MqttObjectClass {
    private static final String Tag = TransmitData.class.getSimpleName();
    private MqttAndroidClient client;
    private MqttConnectOptions options;


    /*Connection object constructor*/
    public MqttObjectClass(final Context context){

        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        final Ringtone r = RingtoneManager.getRingtone(context, notification);


        final String clientId = MqttClient.generateClientId();
        this.client = new MqttAndroidClient(context, "tcp://VirtualMachine:1883", clientId);//use edge hostname to construct and then connect
        //this.client = new MqttAndroidClient(context, "tcp://192.168.2.8:1883", clientId);//use edge ip to construct and then connect
        this.client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.d(Tag, "connection lost from virtual machine");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d(Tag, message.toString());
                r.play();
                toast("wake up Dudeeee!!!!!!!!!!!!", 0, context);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
        Log.d(Tag, "creating MQttcon object");
        if (this.client != null)
            Connect();
        else
            Log.d(Tag, "mqtt connection object is null");
    }

    /** Method for displaying the message when the phone receives a command*/
    private void toast(final String text, final int duration, final Context context) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, text, duration).show();
            }
        });
    }

    /*Method responsible for connecting to edge server. */
    public void Connect(){
        this.options = new MqttConnectOptions();
        this.options.setCleanSession(false);
            try {
                IMqttToken token = this.client.connect(this.options, null);
                Log.d(Tag, "token is " + token);
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        // We are connected
                        Log.d(Tag, "before subscribe ");
                        subscribe("android/receive");//call object method subscribe
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        // Something went wrong e.g. connection timeout or firewall problems
                        Log.d(Tag, "not connected, retrying");
                        try {
                            client.connect();
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        Log.d(Tag, "data memeber client inside Connect isconnected = " + this.client.isConnected());
    }

    public boolean isConnected(){
        return this.client.isConnected();
    }
    /*Method for publishing*/
    public void publish(String name, String topic, byte[] encodedPayload, JSONObject obj){
        try {
            encodedPayload = obj.toString().getBytes("UTF-8");//convert message to be sent into byte array
            MqttMessage message = new MqttMessage(encodedPayload);//construct message
            Log.d(Tag, "before publish isconnected = " + this.client.isConnected());
            if (this.client.isConnected())
                this.client.publish(topic, message);// publish to topic "topic"
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

    /*Method for subscribing */
    public void subscribe(final String Topic){
        if (this.client.isConnected()) {
            try {
                IMqttToken subToken = this.client.subscribe(Topic, 1);// subscribe to topic "Topic"
                subToken.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d(Tag, "subscribed to " + Topic);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken,
                                          Throwable exception) {
                        // The subscription could not be performed, maybe the user was not
                        // authorized to subscribe on the specified topic e.g. using wildcards
                        Log.d(Tag, "subscribe failed ");
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }
}