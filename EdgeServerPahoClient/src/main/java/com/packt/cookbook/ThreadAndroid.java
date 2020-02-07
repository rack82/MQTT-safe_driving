package com.packt.cookbook;

import DataClass.DataClass;
import org.eclipse.paho.client.mqttv3.*;
import java.io.UnsupportedEncodingException;

public class ThreadAndroid implements Runnable{

    private SyncBuffer buffer;
    private String topic;
    protected MqttClient client;
    private volatile boolean running = true;
    private volatile boolean paused = false;
    private final Object pauseLock;

    /**Thread that transmits commands to android phone*/
    public ThreadAndroid(SyncBuffer buffer, String topic, Object pauseLock){
        this.pauseLock = pauseLock;
        this.buffer = buffer;
        this.topic = topic;
        try {
            client = new MqttClient("tcp://localhost:1883", "2");//construct connection object with hostname
            client.connect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        String strCommand;
        DataClass temp;
        while(running) {
            /**Pause lock region ensures that the contents are not being deleted while the thread reads them*/
            synchronized (pauseLock) {
                /**Read method is syncronized, therefore one thread can access at a time*/
                if ((temp = buffer.read()) != null) {
                    if ((strCommand = temp.getCommand()) != null) {
                        byte[] payload = new byte[0];
                        MqttMessage message = null;
                        try {
                            payload = strCommand.getBytes("UTF-8");
                            message = new MqttMessage(payload);
                            client.publish(topic, message);
                        } catch (MqttException e) {
                            e.printStackTrace();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            try {
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**methods for creating inner thread scheduling, not used in this example*/
    public void stop(){
        running = false;
    }

    public void pause(){
        paused = true;
    }

    public void resume(){
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
    }
}
