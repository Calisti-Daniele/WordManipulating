package org.example.database;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import javax.swing.*;
import java.io.File;
import java.sql.*;
import java.util.Properties;

public class DatabaseConnection {

    private Connection connection;
    private Session session;
    private String sshHost;
    private String sshUser;
    private String sshPrivateKey; // Path to your PEM file
    private int sshPort;

    private String dbHost; // this will be localhost after SSH tunnel
    private int dbPort;
    private String dbUser;
    private String dbPassword;
    private String dbName;

    private int localPort = 3307; // local port that will be forwarded to the remote db server

    private void initialize(String dbname) throws JSchException {
        sshHost = "";
        String sshUser = "";
        sshPrivateKey = ""; // Path to your PEM file
        sshPort = 22;
        dbHost = "localhost";
        dbPort = 3306;
        dbUser = "";
        dbPassword = "";
        dbName = dbname;
        localPort = 3307;

        JSch jsch = new JSch();
        jsch.addIdentity(sshPrivateKey,"Euservice_new");
        session = jsch.getSession(sshUser, sshHost, sshPort);
    }

    public DatabaseConnection(String dbName) throws JSchException {
        initialize(dbName);
    }

    public Connection getConnection(){
        try {
            // Check if the private key file exists
            File privateKeyFile = new File(sshPrivateKey);
            if (!privateKeyFile.exists()) {
                throw new RuntimeException("Private key file does not exist: " + sshPrivateKey);
            }

            // Configure the session to use only publickey authentication
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "publickey");
            session.setConfig(config);

            session.connect();
            System.out.println("SSH Connected");

            // Set up port forwarding
            int assignedPort = session.setPortForwardingL(localPort, dbHost, dbPort);
            System.out.println("localhost:" + assignedPort + " -> " + dbHost + ":" + dbPort);

            // Set up database connection
            String jdbcUrl = "jdbc:mysql://localhost:" + localPort + "/" + dbName;
            connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);

            return connection;

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,e.getMessage());
            System.exit(0);
        }

        return null;
    }

    public void close() throws JSchException, SQLException {
        connection.close();
        session.disconnect();
        System.out.println("Disconnected");
    }
}
