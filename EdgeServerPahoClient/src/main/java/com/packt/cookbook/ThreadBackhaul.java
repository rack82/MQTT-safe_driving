package com.packt.cookbook;

import DataClass.DataClass;

public class ThreadBackhaul implements Runnable{
    private SyncBuffer buffer;
    private SocketClient tcpSocket;

    public ThreadBackhaul(SyncBuffer buffer, String Host, int port){
        this.buffer = buffer;
        try {
            tcpSocket = new SocketClient(Host, port); // establish socket connection
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**Body of the thread that sends the data class objects to backhaul server*/
    @Override
    public void run() {
        DataClass dataObj;
        try {
            Thread.sleep(1100);
            while(true) {
                if ((dataObj = buffer.read()) != null) {
                    tcpSocket.sendLogs(dataObj);
                }
                Thread.sleep(1100);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
