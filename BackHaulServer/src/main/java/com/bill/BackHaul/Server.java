package com.bill.BackHaul;


import DataClass.DataClass;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;

class Server {

    private ServerSocket server = null;
    private Socket client = null;
    private ObjectInputStream inStream = null;

    public Server(int port, int select) throws Exception {
        server = new ServerSocket(port);
        if (select == 1)
            listen();
        else{
            client = server.accept();
            inStream = new ObjectInputStream(client.getInputStream());
        }

    }

    /** Method for sending training_Set.csv*/
    public void listen() throws Exception {
        File myFile = new File(System.getProperty("user.home") + "/Training_set.csv");//file to be sent
        String data = null;
        client = this.server.accept(); // blocks until the client connects
        byte[] mybytearray = new byte[(int) myFile.length()];//prepare byte array for stream
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(myFile));
        bis.read(mybytearray, 0, mybytearray.length);// put the byte array into a buffered stream
        OutputStream os = client.getOutputStream();
        os.write(mybytearray, 0, mybytearray.length);//send the stream into the client
        os.close();
        bis.close();
        String clientAddress = client.getInetAddress().getHostAddress();
        System.out.println("\r\nTraining Set sent to" + clientAddress);
        server.close();
        client.close();
    }

    public boolean isClosed(){
        return server.isClosed();
    }

    /**Method for receiving dataObjects and putting them into SQL server*/
    public void receiveLogs(SQLDriver driver){
        DataClass object = null;
        try {
            while(true) {
                if (inStream != null) {
                    object = (DataClass) inStream.readObject();
                    String deviceName = object.getDeviceName();
                    int criticalityLevel = object.getCriticality();
                    String command = object.getCommand();
                    double Longtitude = object.getLongtitude();
                    double Latidude = object.getLatidude();
                    String timeStamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime());
                    //System.out.println("critical = " + criticalityLevel + "command = " + command + "Longtitude = " + Longtitude + "timestamp = " + timeStamp + "\n");
                    String query = "insert into Details (PhoneName, TimeStamp, GPSCoord, CriticalityLevel) values (" + "'" + deviceName + "'" + "," + " " + "'" + timeStamp + "'" + "," + " " + "'" + Latidude + "," + Longtitude + "'" + "," + " " + criticalityLevel + ");";
                    System.out.println(query);
                    driver.execute(query);
                    System.out.println("values inserted in Database \n");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (NullPointerException n) {
            n.printStackTrace();
        }
    }

    public InetAddress getSocketAddress() {
        return this.server.getInetAddress();
    }
    public int getPort(){
        return this.server.getLocalPort();
    }
}