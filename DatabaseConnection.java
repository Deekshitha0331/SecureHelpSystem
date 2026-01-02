package application;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
	private static final String URL = "jdbc:mysql://localhost:3306/CSE360GROUP13"; //  db name
	private static final String USER = "root"; //  db username
	private static final String PASSWORD = "#CSsoftware2024"; //  db password
	
	public static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(URL, USER, PASSWORD);

	}
}