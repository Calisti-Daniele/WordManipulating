package org.example;

import com.jcraft.jsch.JSchException;
import org.example.database.DatabaseConnection;
import org.example.gui.GUI;

import javax.swing.*;
import java.sql.Connection;

public class Main {
    public static void main(String[] args) throws JSchException {
        DatabaseConnection gestionale = new DatabaseConnection("gestionale");
        Connection conn = gestionale.getConnection();


        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new GUI(conn).setVisible(true);
            }
        });
    }
}
