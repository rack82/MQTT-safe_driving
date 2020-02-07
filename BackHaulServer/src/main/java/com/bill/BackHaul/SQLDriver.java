package com.bill.BackHaul;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

class SQLDriver{

    private Connection connection;
    private Statement statement;

    public SQLDriver() {

        System.out.println("-------- MySQL JDBC Connection Testing ------------");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Where is your MySQL JDBC Driver?");
            e.printStackTrace();
            return;
        }

        System.out.println("MySQL JDBC Driver Registered!");
        connect();
    }
    public void connect(){
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/AndroidDB", "root", "8246");

        } catch (SQLException e) {
            System.out.println("Connection Failed! Check output console");
            e.printStackTrace();
            return;
        }

        if (connection != null) {
            System.out.println("You made it, take control your database now!\n");
        } else {
            System.out.println("Failed to make connection!");
        }
    }

    /** SQL object's method for executing a query*/
    public void execute(final String squery){
        try {

            statement = connection.createStatement();
            statement.executeUpdate(squery);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}