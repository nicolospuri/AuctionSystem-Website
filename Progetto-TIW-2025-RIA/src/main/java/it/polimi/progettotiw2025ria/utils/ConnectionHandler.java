package it.polimi.progettotiw2025ria.utils;

import jakarta.servlet.UnavailableException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionHandler {
    public static Connection getConnection() throws UnavailableException {
        final String DATABASE = "Progetto_Web";
        final String USER = "progetto_web";
        final String PASSWORD = "progetto_web";
        final String URL = "jdbc:mysql://localhost:3306/" + DATABASE + "?serverTimezone=UTC";;
        Connection connection = null;
        // Load the JDBC driver
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("Driver loaded");
        } catch (ClassNotFoundException e) {
            throw new UnavailableException("Can't load database driver");
        }
        try {
            connection = DriverManager.getConnection
                    (URL, USER, PASSWORD);
            System.out.println("Database connected");
        } catch (Exception e) {
            throw new UnavailableException("Couldn't get db connection");
        }
        return connection;
    }

    public static void closeConnection(Connection connection) throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}
