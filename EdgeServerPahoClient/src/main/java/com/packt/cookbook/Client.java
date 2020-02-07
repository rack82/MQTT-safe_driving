package com.packt.cookbook;

import DataClass.DataClass;
import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;


class Client implements MqttCallback{

    protected MqttClient client;
    private String topic;
    private TrainingSetData TrainSetObj;
    private int closedOccur, openedOccur;
    private SyncBuffer buffer;
    private Thread mqttThread;
    private Thread backThread;
    private Runnable runAnroidThread;
    private final Object pauseLock;
    private float classifierSuccessCounter = 0;
    private float classifierExecutionCounter = 0;


    public Client(String topic, SyncBuffer buffer) {
        this.pauseLock = new Object(); //Object for syncronized methods on the threads
        this.buffer = buffer; //sync buffer holds the objects and the commands to be sent to android and backhaul
        this.closedOccur = 0; // counters for the classifier
        this.openedOccur = 0;
        this.topic = topic;

        runAnroidThread = new ThreadAndroid(buffer, "android/receive", pauseLock); //android thread runnable object
        mqttThread = new Thread(runAnroidThread); //initialize android thread
        backThread = new Thread(new ThreadBackhaul(buffer, "bill-virtual-machine", 6090));//initialize backhaul thread

        try {
            client = new MqttClient("tcp://localhost:1883", "1");//construct connection object with hostname
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**MQTT body connects and subscribes to the topic and sets callback, it also starts the two threads*/
    public void execute() {
        try {
            backThread.start();
            Thread.sleep(2000);
            String homeDir = System.getProperty("user.home");
            TrainSetObj = new TrainingSetData(homeDir + "/Training_Set.csv");
            mqttThread.start();
            client.connect();
            client.setCallback(this);
            client.subscribe(this.topic);
        } catch (MqttException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**Discconect is called when we receive message = finish. Just for testing purposes*/
    public void Disconnet() throws MqttException {
        client.disconnect();
        client.close();
    }

    public void ParString(String str) {
        DataClass ObjData = null;
        /**It constructs a Json object using the string provided*/
        JSONObject json = new JSONObject(str);
        /**Json methods allow for easy access to the data of the string payload*/
        float x = json.getFloat("x");
        float y = json.getFloat("y");
        float z = json.getFloat("z");
        String name = new String(json.getString("name"));
        String filename = new String(json.getString("csv fileName"));
        System.out.println("csv file is " + filename);
        double longtitude = json.getDouble("longitude");
        double latitude = json.getDouble("latitude");
        JSONArray jsonStr = json.getJSONArray("buffer");

        /**Construct an object using the data parsed from json
         * pskc_schema.xsd is causing errors when we receive it*/
        if (!filename.equals("pskc_schema.xsd")) {
            ObjData = new DataClass(name, filename, x, y, z, longtitude, latitude, jsonStr);
            startScheduler(ObjData, filename);
        }
    }
    /** Method for calling the classifier, filling the buffer with commands and calculate the success of the classifier*/
    public void startScheduler(DataClass data, String file){
        boolean result;
        if (!(result = classify(data))) {
            classifierExecutionCounter++;
            if (file.contains("closed")) {
                classifierSuccessCounter++;
                System.out.println("execution: " + (int) classifierExecutionCounter + " classifier successfully stated CLOSED\n");
            }
            else
                System.out.println("execution: " + (int) classifierExecutionCounter + " classifier wrongly stated CLOSED\n");
            if (classifierExecutionCounter >= 20){
                float classResult = (classifierSuccessCounter / classifierExecutionCounter) * 100;
                System.out.println("classifier was successfull " + classResult + "%\n");
                classifierSuccessCounter = 0;
                classifierExecutionCounter = 0;
            }
            /** above 3 closed occurances, we fill the buffer with a command*/
            if((++this.closedOccur) >= 3){
                data.setCommand("Execute Eyes Closed Single Danger 1");
                buffer.add(data);
            }
        }
        else {
            classifierExecutionCounter++;
            if (file.contains("opened")) {
                classifierSuccessCounter++;
                System.out.println("execution: " + (int) classifierExecutionCounter + " classifier successfully stated OPENED\n");
            }
            else
                System.out.println("execution: " + (int) classifierExecutionCounter + " classifier wrongly stated OPENED\n");
            if (classifierExecutionCounter >= 20){
                float classResult = (classifierSuccessCounter / classifierExecutionCounter) * 100;
                System.out.println("classifier was successfull " + classResult + "%\n");
                classifierSuccessCounter = 0;
                classifierExecutionCounter = 0;
            }
            /**If the classification is Eyes opened, we delete all the commands with proper scheduling.
             * pausedLock object is also being used by Android Thread*/
            synchronized (pauseLock) {
                buffer.delete();
                this.closedOccur = 0;
            }
        }
    }
    /** KNN Algorithm as the classifier*/
    public boolean classify(DataClass dataObj){

        double additiveDistance = 0.0;
        int EyesOpened = 0;
        int EyesClosed = 0;
        int eyes;
        double weightMeterOpened = 0.0;
        double weightMeterClosed = 0.0;
        ArrayList<EuclDistanceStruct> distances = new ArrayList<EuclDistanceStruct>();
        ArrayList<EuclDistanceStruct> I = new ArrayList<EuclDistanceStruct>();

        for (String[] row : TrainSetObj.dataframe) {
            if (row.length <= 1)
                continue;
            if (row[0].contains("EyesOpened"))
                eyes = 1;
            else
                eyes = 0;

            additiveDistance = 0.0;
            for (int i = 0; i < (row.length - 1); i++)
                additiveDistance = additiveDistance + Math.pow((dataObj.featureVectorDouble[i] - Double.parseDouble(row[i + 1])),2);

            /*add the objects that hold the eucl distance and the class*/
            double finaldistance = Math.sqrt(additiveDistance);
            EuclDistanceStruct distObj = new EuclDistanceStruct();
            distObj.dist = finaldistance;
            distObj.experimentClass = eyes;
            distances.add(distObj);
        }
        for (int k = 0; k < 7; k++){
            EuclDistanceStruct min = distances.get(0);
            for (int i = 0; i < distances.size(); i++){
                if(distances.get(i).dist < min.dist)
                    min = distances.get(i);
            }
            I.add(min);
            if(I.get(I.size() - 1).experimentClass == 1) {
                EyesOpened++;
                weightMeterOpened = weightMeterOpened + 1/(I.get(I.size() - 1).dist);
            }
            else {
                EyesClosed++;
                weightMeterClosed = weightMeterClosed + 1/(I.get(I.size() - 1).dist);
            }
            distances.remove(min);
        }
        /**Returns true if eyes are opened*/
        return((weightMeterOpened * EyesOpened) > (weightMeterClosed * EyesClosed));
    }

    @Override
    public void connectionLost(Throwable throwable) {
        System.out.println("connection to server ended");
    }


    /** MessageArrived is called when a message arrives*/
    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws MqttException {
        String end = "finish";
        if (mqttMessage.toString().equals(end)) {
            Disconnet();
        }
        String str = null;
        try {
            /**Retreive the message from android mqtt to a string*/
            str = new String(mqttMessage.getPayload(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //System.out.println("string is " + str);
        ParString(str);//call the method that stores the android data into Dataclass object
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {System.out.println("delivery complete");}
}

/**Small class (like struct) used in classifier*/
class EuclDistanceStruct{
    public Double dist;
    public int experimentClass;
}

