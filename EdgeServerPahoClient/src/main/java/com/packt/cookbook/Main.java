package com.packt.cookbook;

import java.io.IOException;

public class Main {

    public static void main(String[] args){
        SyncBuffer buffer = new SyncBuffer(); //list with data objects containing phone info and commands
        TrainingSetData dataObj = null;
        SocketClient myclient = null;

        try {
            myclient = new SocketClient("bill-virtual-machine", 6090);//connect with backhaul
            System.out.println("connection to tcp socket success");
            myclient.receiveSet();//method for receiving Training_Set.csv
            System.out.println("csv Set received\n");
            myclient = null;
            Thread.sleep(2000);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        /** Mqtt client will now run on UI thread*/
        Client mqttClient = new Client("android/topic", buffer);
        mqttClient.execute();
    }
}