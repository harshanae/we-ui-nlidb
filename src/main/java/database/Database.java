package database;

import java.sql.*;

public class Database {
    Connection conn;
    String name;

    public Database(String host, int port, String user, String password, String dbName) {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName;
        this.name = dbName;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.conn = DriverManager.getConnection(url, user, password);
            System.out.println("Database connected!");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
