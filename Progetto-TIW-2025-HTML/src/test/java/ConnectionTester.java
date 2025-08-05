import java.sql.*;
public class ConnectionTester {
    public static void main(String[] args) {
        final String DATABASE = "Progetto_Web";
        final String USER = "progetto_web";
        final String PASSWORD = "progetto_web";
        Connection connection = null;
        // Load the JDBC driver
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("Driver loaded");
        } catch (ClassNotFoundException e) {
            System.out.println("Driver not found");
            e.printStackTrace();
        }
        try {
            connection = DriverManager.getConnection
                    ("jdbc:mysql://localhost:3306/" + DATABASE, USER, PASSWORD);
            System.out.println("Database connected");

            // Perform a simple query to test the connection
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM Utente");
            if (rs.next()) {
                System.out.println("Connessione funzionante: " + rs.getString("username"));
            }
            connection.close();
        } catch (Exception e) {
            System.out.println("Connection failed");
            e.printStackTrace();
        }
    }
}