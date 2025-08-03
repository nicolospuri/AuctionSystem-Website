import java.sql.*;
public class ConnectionTester {
    public static void main(String[] args) throws SQLException,
            ClassNotFoundException {
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
                    ("jdbc:mysql://localhost:3306/" + DATABASE + "?serverTimezone=UTC", USER, PASSWORD);
            System.out.println("Database connected");
            connection.close();
        } catch (Exception e) {
            System.out.println("Connection failed");
            e.printStackTrace();
        }
    }
}