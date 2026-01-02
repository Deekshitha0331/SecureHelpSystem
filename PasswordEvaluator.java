package application;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PasswordEvaluator {
	
	// Evaluates if the password meets basic strength requirements
	public static String evaluatePassword(String password) {
		if (password.length() < 8) {
			return "Password too short";
		}
		if (!password.matches(".*[A-Z].*")) {
			return "Password must contain at least one uppercase letter";
			
		}
		if (!password.matches(".*[a-z].*")) { 
			return "Password must contain at least one lowercase letter";
		}
		if (!password.matches(".*\\d.*")) {
			return "Password muct contain at least one number";
			
		}
		if (!password.matches(".*[!@#$%^&*].*")) {
			return "Password must contain at least one special character (!@#$%^&*)";
			
		}
		return ""; //password is valid
}
	
	//check if the password already exists in the database (if unique password policy is required)
	public static boolean isPasswordUnique(Connection connection, String password) {
		String query = "SELECT COUNT(*) FROM users WHERE password = ?";
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			stmt.setString(1, password);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1) == 0; //Returns true if password is unique 
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	
	
	//Method to validate password and check for uniqueness in the database
	public static String validatePasswordAndCheckUniqueness(Connection connection, String password) {
		String validationMessage = evaluatePassword(password);
		if (!validationMessage.isEmpty()) {
			return validationMessage; //return validatiion error if password dosen't meet the required criteria
		}
		if (!isPasswordUnique(connection, password)) {
			return "Password already in use, please choose a different one.";
			
		}
		return ""; // return empty if the password is valid and uniques
	}
	
}
