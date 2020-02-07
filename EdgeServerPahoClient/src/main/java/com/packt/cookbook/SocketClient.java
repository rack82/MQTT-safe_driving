package com.packt.cookbook;

import DataClass.DataClass;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class SocketClient {

    private Socket socket;
    private Scanner scanner;
    private ObjectOutputStream os = null;

    /**construct tcp connection object*/
    public SocketClient(String DNS, int serverPort) throws Exception {
        this.socket = new Socket(DNS, serverPort);
        this.scanner = new Scanner(System.in);
        os = new ObjectOutputStream(socket.getOutputStream());
    }

    /**method receives the Training_Set.csv. Tcp socket doesn't run on separate thread. It might in next project phase*/
    public void receiveSet() throws IOException {
        try {
            String homeDir = System.getProperty("user.home");
            String fileName = homeDir + "/Training_Set.csv";
            InputStream is = socket.getInputStream();
            int bufferSize = socket.getReceiveBufferSize();
            System.out.println("Buffer size: " + bufferSize);
            FileOutputStream fos = new FileOutputStream(fileName);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            byte[] bytes = new byte[bufferSize];
            int count;
            while ((count = is.read(bytes)) >= 0) {
                bos.write(bytes, 0, bufferSize);
            }
            bos.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        socket.close();
    }
    /** Method used by backhaul thread in order to send the Data Objects into backhaul server*/
    public void sendLogs(DataClass data){
        System.out.println("Sending log data to backhaul \n");
        try {
            if (socket.isConnected()) {
                os.writeObject(data);
                os.flush();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

