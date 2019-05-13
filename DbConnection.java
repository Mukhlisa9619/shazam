/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sas;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import java.io.IOException; 
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;

/**
 *
 * @author Muxlisa
 */
public class DbConnection {
    private Connection connection = DriverManager.getConnection("jdbc:mysql://localhost/musicdb","root", "");
    public static Connection externalConnection;
    private Statement statement;
    private ResultSet resultSet;
    private String fields = "";
 
    public DbConnection() throws SQLException {
        statement = connection.createStatement();
        externalConnection = connection;
//        statement.close();
        /**
         * this constructor is for default database
         * */
    }
 
    public void execute(String query) throws SQLException {
        statement = connection.createStatement();
        try {
            statement.execute(query);
        } finally {
            closeConnection();
        }
    }
 
 
    public ArrayList get_data(String query, String col1, String col2) throws SQLException {
//            System.out.println(query);
            statement = connection.createStatement();
            ArrayList<String> ids = new ArrayList<String>();
//            System.out.println(query);
        try {
            ResultSet rs = statement.executeQuery(query);
            while (rs.next()) {
                String firstCol = rs.getString(col1);
                String d = rs.getString(col2);
                ids.add(firstCol);
                //System.out.println(col1 + firstCol + ": " + col2 + ": " + d + "\n");
            }
        } finally {
            closeConnection();
        }
        return ids;
        
    }
 
    public ArrayList get_music(String query) throws SQLException {
           // System.out.println(query);
            statement = connection.createStatement();
            ArrayList<String> ids = new ArrayList<String>();
//            System.out.println(query);
        try {
            ResultSet rs = statement.executeQuery(query);
            while (rs.next()) {
                String firstCol = rs.getString("location");
//                String d = rs.getString(col2);
                ids.add(firstCol);
                //System.out.println(col1 + firstCol + ": " + col2 + ": " + d + "\n");
            }
        } finally {
            closeConnection();
        }
        return ids;
        
    }
 
    @Override
    public String toString() {
        return "The database connection is successful";
    }
 
    public boolean closeConnection() throws SQLException {
        this.statement.close();
        return this.statement.isClosed();
    }
 
    public PreparedStatement preparedStatement(String preparedQuery) throws SQLException {
        return connection.prepareStatement(preparedQuery);
    }
}