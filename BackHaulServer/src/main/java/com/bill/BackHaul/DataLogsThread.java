package com.bill.BackHaul;

import DataClass.DataClass;

public class DataLogsThread implements Runnable {

    private SQLDriver SqlCon;
    private Server server;

    /** Thread that will receive the data objects from edge and using JDBC object will put them in database*/
    public DataLogsThread() {
        SqlCon = new SQLDriver();
        try {
            server = new Server(6090, 2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**Method receiveLogs of tcp socket does the interaction with the SQL and edge*/
    public void run() {
        server.receiveLogs(SqlCon);
    }
}
