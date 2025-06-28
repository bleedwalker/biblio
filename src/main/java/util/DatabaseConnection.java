package util;

import java.sql.*;
import java.util.concurrent.*;

public class DatabaseConnection {
    private static final BlockingQueue<Connection> pool = new LinkedBlockingQueue<>(10);
    private static final String URL = "jdbc:mysql://localhost:3306/biblioteka";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            initializePool();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Database driver not found", e);
        }
    }

    private static void initializePool() {
        for (int i = 0; i < 10; i++) {
            pool.add(createConnection());
        }
    }

    private static Connection createConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create DB connection", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = pool.poll();
        return (conn == null || conn.isClosed()) ? createConnection() : conn;
    }

    public static void releaseConnection(Connection conn) {
        if (conn != null) pool.offer(conn);
    }

    public static void closeAll() {
        pool.forEach(c -> {
            try {
                if (c != null && !c.isClosed()) {
                    c.close();
                }
            } catch (SQLException ignored) {}
        });
        pool.clear();
        System.out.println("All database connections closed");
    }
}