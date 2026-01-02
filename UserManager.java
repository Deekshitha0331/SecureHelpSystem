package application;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class UserManager {

	// Add a new user to the database (including OTP and roles)
	public void addUser(User user) {
	    String insertUserSQL = "INSERT INTO users (username, password_hash, email, first_name, middle_name, last_name, preferred_name, expertise_level, is_one_time_password, otp, otp_expiration) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	    String getRoleIdSQL = "SELECT id FROM roles WHERE role_name = ?";
	    String insertUserRoleSQL = "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)";

	    try (Connection conn = DatabaseConnection.getConnection();
	         PreparedStatement insertUserStmt = conn.prepareStatement(insertUserSQL, Statement.RETURN_GENERATED_KEYS);
	         PreparedStatement getRoleIdStmt = conn.prepareStatement(getRoleIdSQL);
	         PreparedStatement insertUserRoleStmt = conn.prepareStatement(insertUserRoleSQL)) {

	        // Insert user details including OTP
	        insertUserStmt.setString(1, user.getUsername());
	        insertUserStmt.setString(2, new String(user.getPasswordHash())); // Password handling
	        insertUserStmt.setString(3, user.getEmail());
	        insertUserStmt.setString(4, user.getFirstName());
	        insertUserStmt.setString(5, user.getMiddleName() != null ? user.getMiddleName() : null);
	        insertUserStmt.setString(6, user.getLastName());
	        insertUserStmt.setString(7, user.getPreferredName() != null ? user.getPreferredName() : null);
	        insertUserStmt.setString(8, user.getExpertiseLevel());
	        insertUserStmt.setBoolean(9, user.isOneTimePassword());  // Is this a one-time password?
	        insertUserStmt.setString(10, user.getOtp());  // Save OTP in the database
	        insertUserStmt.setTimestamp(11, user.getOtpExpiration() != null ? Timestamp.valueOf(user.getOtpExpiration()) : null);  // Save OTP expiration

	        insertUserStmt.executeUpdate();

	        // Get the generated user ID (auto-incremented)
	        ResultSet generatedKeys = insertUserStmt.getGeneratedKeys();
	        if (generatedKeys.next()) {
	            long userId = generatedKeys.getLong(1);

	            // Assign roles to the user
	            for (String role : user.getRoles()) {
	                getRoleIdStmt.setString(1, role);
	                ResultSet roleRs = getRoleIdStmt.executeQuery();
	                if (roleRs.next()) {
	                    long roleId = roleRs.getLong("id");
	                    insertUserRoleStmt.setLong(1, userId);  // Set the user ID
	                    insertUserRoleStmt.setLong(2, roleId);  // Set the role ID
	                    insertUserRoleStmt.executeUpdate();     // Insert into the user_roles table
	                }
	            }
	        }

	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}


	// Generate an OTP for password reset and set expiration time
	public void generatePasswordResetOtp(User user) {
	    String otp = generateOtp();  // Reuse the OTP generation function
	    user.setOtp(otp);  // Set the generated OTP
	    user.setOtpExpiration(LocalDateTime.now().plusHours(1));  // OTP valid for 1 hour

	    try (Connection conn = DatabaseConnection.getConnection()) {
	        // Update the user in the database with the OTP and expiration
	        String updateUserSQL = "UPDATE users SET otp = ?, otp_expiration = ?, is_one_time_password = TRUE WHERE username = ?";
	        try (PreparedStatement ps = conn.prepareStatement(updateUserSQL)) {
	            ps.setString(1, user.getOtp());
	            ps.setTimestamp(2, Timestamp.valueOf(user.getOtpExpiration()));
	            ps.setString(3, user.getUsername());
	            ps.executeUpdate();
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }

	    // Optionally show the OTP to the admin via a pop-up dialog (reuse the existing OTP pop-up logic)
	    showOtpPopup(otp);
	}

	// This method resets the user's password after they successfully log in with an OTP
	public void resetPasswordWithOtp(User user, String newPassword) {
	    String resetPasswordSQL = "UPDATE users SET password_hash = ?, is_one_time_password = FALSE, otp = NULL, otp_expiration = NULL WHERE username = ?";

	    try (Connection conn = DatabaseConnection.getConnection();
	         PreparedStatement updateStmt = conn.prepareStatement(resetPasswordSQL)) {

	        // Update the password hash, clear the OTP, and remove the expiration
	        updateStmt.setString(1, new String(newPassword.toCharArray()));  // Set the new password
	        updateStmt.setString(2, user.getUsername());  // Find the user by username
	        updateStmt.executeUpdate();

	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}

	public void updateUserRoles(User user) {
	    // Delete current roles for the user
	    String deleteRolesSQL = "DELETE FROM user_roles WHERE user_id = ?";
	    String insertUserRoleSQL = "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)";

	    try (Connection conn = DatabaseConnection.getConnection();
	         PreparedStatement deleteStmt = conn.prepareStatement(deleteRolesSQL);
	         PreparedStatement insertStmt = conn.prepareStatement(insertUserRoleSQL)) {

	        // Delete existing roles
	        deleteStmt.setInt(1, user.getId());
	        deleteStmt.executeUpdate();

	        // Insert new roles
	        for (String role : user.getRoles()) {
	            String getRoleIdSQL = "SELECT id FROM roles WHERE role_name = ?";
	            try (PreparedStatement roleIdStmt = conn.prepareStatement(getRoleIdSQL)) {
	                roleIdStmt.setString(1, role);
	                ResultSet rs = roleIdStmt.executeQuery();
	                if (rs.next()) {
	                    int roleId = rs.getInt("id");
	                    insertStmt.setInt(1, user.getId());
	                    insertStmt.setInt(2, roleId);
	                    insertStmt.executeUpdate();
	                }
	            }
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}


	// Method to reset user password after OTP verification
	// Reset user password (setting a one-time password)
	// Reset user password by generating a one-time password and setting an expiration
	public void resetPassword(String username, String generatedOtp, LocalDateTime otpExpiration) {
	    // SQL to update the user's password with the new OTP and expiration time
	    String resetPasswordSQL = "UPDATE users SET otp = ?, is_one_time_password = TRUE, otp_expiration = ? WHERE username = ?";

	    try (Connection conn = DatabaseConnection.getConnection();
	         PreparedStatement resetStmt = conn.prepareStatement(resetPasswordSQL)) {

	        resetStmt.setString(1, generatedOtp);  // Set the generated OTP
	        resetStmt.setTimestamp(2, Timestamp.valueOf(otpExpiration));  // Set the expiration time
	        resetStmt.setString(3, username);  // Identify the user by username
	        resetStmt.executeUpdate();  // Execute the update to store the OTP and expiration

	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}



    
 // Generates a 6-digit OTP for the user
    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);  // Generates a 6-digit number
        return String.valueOf(otp);
    }
    
    
    // Method to display the OTP in a pop-up dialog
    public void showOtpPopup(String otp) {
        // Create an alert dialog to display the OTP
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("One-Time Password (OTP)");
        alert.setHeaderText("User OTP");
        alert.setContentText("The OTP for this user is: " + otp + "\nPlease note it down.");

        // Display the alert and wait for the admin to close it
        alert.showAndWait();
    }
    
 // Get user by OTP (Invitation Code)
    public User getUserByInvitationCode(String otp) {
        String selectUserSQL = "SELECT * FROM users WHERE otp = ?";
        User user = null;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement selectUserStmt = conn.prepareStatement(selectUserSQL)) {
            selectUserStmt.setString(1, otp);
            ResultSet rs = selectUserStmt.executeQuery();

            if (rs.next()) {
                user = extractUserFromResultSet(rs);  // Extract user details
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return user;
    }
    public void inviteUser(User user) {
        String otp = generateOtp();  // Generates a 6-digit random OTP
        user.setOtp(otp);  // Set the generated OTP
        user.setOtpExpiration(LocalDateTime.now().plusHours(1));  // OTP valid for 1 hour

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Insert the new user details into the users table
            String insertUserSQL = "INSERT INTO users (username, password_hash, email, otp, otp_expiration, expertise_level) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertUserSQL, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.getUsername());
                ps.setString(2, new String(user.getPasswordHash()));  // Hash the password
                ps.setString(3, user.getEmail());
                ps.setString(4, user.getOtp());  // Save OTP
                ps.setTimestamp(5, Timestamp.valueOf(user.getOtpExpiration()));  // Save OTP expiration
                ps.setString(6, user.getExpertiseLevel());  // Expertise level (optional)

                ps.executeUpdate();

                // Get the generated user ID for role assignment
                ResultSet generatedKeys = ps.getGeneratedKeys();
                if (generatedKeys.next()) {
                    long userId = generatedKeys.getLong(1);

                    // Assign roles to the user
                    String assignRoleSQL = "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)";
                    try (PreparedStatement roleStmt = conn.prepareStatement(assignRoleSQL)) {
                        for (String role : user.getRoles()) {
                            // Fetch role ID by role name
                            String getRoleIdSQL = "SELECT id FROM roles WHERE role_name = ?";
                            try (PreparedStatement roleIdStmt = conn.prepareStatement(getRoleIdSQL)) {
                                roleIdStmt.setString(1, role);
                                ResultSet rs = roleIdStmt.executeQuery();
                                if (rs.next()) {
                                    long roleId = rs.getLong("id");
                                    roleStmt.setLong(1, userId);
                                    roleStmt.setLong(2, roleId);
                                    roleStmt.executeUpdate();  // Assign role
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Show the OTP to the admin via a pop-up dialog
        showOtpPopup(otp);
    }


    // Retrieve a user by username
    public User getUser(String username) {
        String selectUserSQL = "SELECT u.*, GROUP_CONCAT(r.role_name) AS roles " +
                               "FROM users u " +
                               "LEFT JOIN user_roles ur ON u.id = ur.user_id " +
                               "LEFT JOIN roles r ON ur.role_id = r.id " +
                               "WHERE u.username = ? GROUP BY u.id";
        User user = null;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement selectUserStmt = conn.prepareStatement(selectUserSQL)) {

            selectUserStmt.setString(1, username);
            ResultSet rs = selectUserStmt.executeQuery();

            if (rs.next()) {
                String roles = rs.getString("roles");
                Set<String> roleSet = new HashSet<>();
                if (roles != null && !roles.isEmpty()) {
                    String[] rolesArray = roles.split(",");
                    for (String role : rolesArray) {
                        roleSet.add(role.trim());
                    }
                }

                user = new User(rs.getInt("id"), rs.getString("username"),
                                rs.getString("password_hash").toCharArray(), rs.getBoolean("is_one_time_password"),
                                rs.getTimestamp("otp_expiration") != null ? rs.getTimestamp("otp_expiration").toLocalDateTime() : null,
                                rs.getBoolean("is_setup_complete"), rs.getString("email"),
                                rs.getString("first_name"), rs.getString("middle_name"),
                                rs.getString("last_name"), rs.getString("preferred_name"),
                                rs.getString("expertise_level"), roleSet);
                
                user.setOtp(rs.getString("otp"));  // Make sure to retrieve the OTP
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return user;
    }


	
    public void generatePasswordResetOtp(String username) {
        String generatedOtp = generateOtp();  // Generate a new OTP
        LocalDateTime otpExpiration = LocalDateTime.now().plusHours(1);  // Set OTP to expire in 1 hour

        // Store the OTP and expiration in the database
        String resetPasswordSQL = "UPDATE users SET otp = ?, is_one_time_password = TRUE, otp_expiration = ? WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement resetStmt = conn.prepareStatement(resetPasswordSQL)) {

            resetStmt.setString(1, generatedOtp);  // Set the generated OTP
            resetStmt.setTimestamp(2, Timestamp.valueOf(otpExpiration));  // Set the expiration time
            resetStmt.setString(3, username);  // Identify the user by username
            resetStmt.executeUpdate();  // Execute the update to store the OTP and expiration

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Show the OTP to the admin (could be in a pop-up or console for now)
        showOtpPopup(generatedOtp);  // Display OTP to the admin for password reset
    }

    

    public void generateOtpForPasswordReset(String username) {
        String generatedOtp = generateOtp();  // Generate a new OTP
        LocalDateTime otpExpiration = LocalDateTime.now().plusHours(1);  // Set OTP to expire in 1 hour

        // Store the OTP and expiration in the database
        String resetPasswordSQL = "UPDATE users SET otp = ?, is_one_time_password = TRUE, otp_expiration = ? WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement resetStmt = conn.prepareStatement(resetPasswordSQL)) {

            resetStmt.setString(1, generatedOtp);  // Set the generated OTP
            resetStmt.setTimestamp(2, Timestamp.valueOf(otpExpiration));  // Set the expiration time
            resetStmt.setString(3, username);  // Identify the user by username
            resetStmt.executeUpdate();  // Execute the update to store the OTP and expiration

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Show the OTP to the admin (could be in a pop-up or console for now)
        showOtpPopup(generatedOtp);  // Display OTP to the admin for password reset
    }

    public boolean verifyPasswordResetOtp(User user, String enteredOtp) {
        // Compare the entered OTP with the stored OTP
        return enteredOtp.equals(user.getOtp());
    }



    // Update user details (for account setup completion)
    public void updateUserDetails(User user, String email, String firstName, String middleName, String lastName, String preferredName) throws SQLException {
        String updateUserSQL = "UPDATE users SET email = ?, first_name = ?, middle_name = ?, last_name = ?, preferred_name = ?, is_setup_complete = TRUE WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement updateUserStmt = conn.prepareStatement(updateUserSQL)) {

            updateUserStmt.setString(1, email);
            updateUserStmt.setString(2, firstName);
            updateUserStmt.setString(3, middleName.isEmpty() ? null : middleName);  // Allow null for optional middle name
            updateUserStmt.setString(4, lastName);
            updateUserStmt.setString(5, preferredName.isEmpty() ? null : preferredName);  // Allow null for optional preferred name
            updateUserStmt.setString(6, user.getUsername());

            updateUserStmt.executeUpdate();  // Update the user's details in the database
        }
    }

    
 // Update user credentials (after OTP login and account creation)
    public void updateUserCredentials(User user) {
        String updateUserSQL = "UPDATE users SET username = ?, password_hash = ?, is_one_time_password = FALSE, otp = NULL, otp_expiration = NULL WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement updateStmt = conn.prepareStatement(updateUserSQL)) {
            updateStmt.setString(1, user.getUsername());
            updateStmt.setString(2, new String(user.getPasswordHash()));  // Update password
            updateStmt.setInt(3, user.getId());
            updateStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
 // Helper method to extract User from ResultSet
    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String uname = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        boolean isOTP = rs.getBoolean("is_one_time_password");
        Timestamp otpExp = rs.getTimestamp("otp_expiration");
        boolean isSetup = rs.getBoolean("is_setup_complete");
        String email = rs.getString("email");
        String firstName = rs.getString("first_name");
        String middleName = rs.getString("middle_name");
        String lastName = rs.getString("last_name");
        String preferredName = rs.getString("preferred_name");
        String expertiseLevel = rs.getString("expertise_level");

        return new User(id, uname, passwordHash.toCharArray(), isOTP,
                otpExp != null ? otpExp.toLocalDateTime() : null,
                isSetup, email, firstName, middleName, lastName, preferredName,
                expertiseLevel, new HashSet<>());
    }

    

    // Remove a user from the database
    public void removeUser(User user) {
        String deleteFromSpecialAccessSQL = "DELETE FROM special_access_group_users WHERE user_id = ?";
        String deleteFromFirstAdminSQL = "DELETE FROM first_admin WHERE user_id = ?";
        String deleteUserSQL = "DELETE FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement deleteSpecialAccessStmt = conn.prepareStatement(deleteFromSpecialAccessSQL);
             PreparedStatement deleteFirstAdminStmt = conn.prepareStatement(deleteFromFirstAdminSQL);
             PreparedStatement deleteUserStmt = conn.prepareStatement(deleteUserSQL)) {

            // Delete from special_access_group_users table
            deleteSpecialAccessStmt.setLong(1, user.getId());
            deleteSpecialAccessStmt.executeUpdate();

            // Delete from first_admin table
            deleteFirstAdminStmt.setLong(1, user.getId());
            deleteFirstAdminStmt.executeUpdate();

            // Finally, delete from users table
            deleteUserStmt.setString(1, user.getUsername());
            deleteUserStmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //getting infroamtion of students by fetching from MySQl
	public List<User> getAllStudents() {
        String getAllStudentsSQL = """
            SELECT u.*, GROUP_CONCAT(r.role_name) AS roles
            FROM users u
            JOIN user_roles ur ON u.id = ur.user_id
            JOIN roles r ON ur.role_id = r.id
            WHERE r.role_name = 'Student'
            GROUP BY u.id
        """;

        List<User> students = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(getAllStudentsSQL)) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String roles = rs.getString("roles");
                Set<String> roleSet = new HashSet<>();
                if (roles != null && !roles.isEmpty()) {
                    String[] rolesArray = roles.split(",");
                    for (String role : rolesArray) {
                        roleSet.add(role.trim());
                    }
                }

                User student = new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("password_hash").toCharArray(),
                    rs.getBoolean("is_one_time_password"),
                    rs.getTimestamp("otp_expiration") != null ? rs.getTimestamp("otp_expiration").toLocalDateTime() : null,
                    rs.getBoolean("is_setup_complete"),
                    rs.getString("email"),
                    rs.getString("first_name"),
                    rs.getString("middle_name"),
                    rs.getString("last_name"),
                    rs.getString("preferred_name"),
                    rs.getString("expertise_level"),
                    roleSet
                );

                students.add(student);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return students;
    }


	//listing all users 
	public List<User> listUsers() {
		String listUsersSQL = "SELECT u.*, GROUP_CONCAT(r.role_name) AS roles " + "FROM users u " + 
			"JOIN user_roles ur ON u.id = ur.user_id " + "JOIN roles r ON ur.role_id = r.id " +
			"GROUP BY u.id";
		List<User> users = new ArrayList<>();  

		try (Connection conn = DatabaseConnection.getConnection();
			PreparedStatement listUsersStmt = conn.prepareStatement(listUsersSQL)) {
				ResultSet rs = listUsersStmt.executeQuery();
				while (rs.next()){
					int id  = rs.getInt("id");
					String uname = rs.getString("username");
					String passwordHash = rs.getString("password_hash");
					boolean isOTP = rs.getBoolean("is_setup_complete");
					Timestamp otpExp = rs.getTimestamp("otp_epiration");
					boolean isSetup = rs.getBoolean("is_setup_complete");
					String email = rs.getString("email");
					String firstName = rs.getString("first_name");
					String middleName = rs.getString("middle_name");
					String lastName = rs.getString("last_name");
					String preferredName = rs.getString("preferred_name");
					String expertiseLevel = rs.getString("expertise_level");
					String roles = rs.getString("roles");

					Set<String> roleSet = new HashSet<>();
					if (roles != null && !roles.isEmpty()) {
						String[] rolesArray = roles.split(",");
						for (String role : rolesArray) {
							rolesSet.add(role.trim());
						}
					}

					User user = new User(id, uname, passwordHash.toCharArray(), isOTP, 
					otpExp != null ? otpExp.toLocalDateTime() : null, isSetup, email,
					firstName, middleName, lastName, preferredName, expertiseLevel, roleSet);

					users.add(user);
				}
			}    catch (SQLException e) {
				e.printStackTrace();
			}

			return users;

	}

	public boolean isUserInSpecialAccessGroup(int userId, String groupName){
		String sql = "SELECT * FROM special_access_group_users WHERE user_id = ? AND group_name = ?";
		try (Connection connection = DatabaseConnection.getConnection();
			PreparedStatement stmt = connection.prepareStatement(sql)) {
				stmt.setInt(1, userId);
				stmt.setString(2, groupName);
				ResultSet rs = stmt.executeQuery();
				return rs.next();
			} catch (SQLException e) {
				e.printStackTrace();
			} 
			return false;   
			}

	//checking if the user has speciall access admin  rught sor nit 
	public boolean hasSpecialAccessAdminRights(int userId, String groupName) {
		String sql = "SELECT role FROM special_access_group_users WHERE user_id = ? AND role = 'admin'";
		try (Connection connection = DatabaseConnection.getConnection();
			PreparedStatement stmt = connection.prepareStatement(sql)) {
				stmt.setInt(1, userId);
				stmt.setString(2, groupName);
				ResultSet rs = stmt.executeQuery();
				return rs.next();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return false;
	}

	// checking if the user has a admin role 
	public boolean hasSpecialAccessAdminRole(int userId) {
		return hasSpecaialAccessRole(userId, "admin");
	}

	//checkign if the user has insteutor role or no t
	public boolean hasSpecialAccessInstructorRole(int userId) {
		return hasSpecialAccessRole(userId, "instructor");
		
	}

	//check if the user is student or not 
	public boolean hasSpecialAccessStudentRole(int userId) {
		return hasSpecialAccessRole(userId, "student");
	}


	//very important cause we store the firsta dmin in a sql table so that we can make sure that atleast one person has all the powers
	public static boolean isFirstAdmin(long userId) {
		String sql = "SELECT COUNT(*) FROM first_admin WHERE user_id = ?";
		
		
		try (Connection connection = DatabaseConnection.getConnection();
				PreparedStatement stmt = connection.prepareStatement(sql)) {
			
			stmt.setLong(1, userId);
			ResultSet rs = stmt.executeQuery();
			
			if (rs.next()) {
				return rs.getInt(1) > 0;
			}
		} catck (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}


	// adding him into a  sl table 
	public boolean addFirstAdmin(User user) {
		String sql = "INSERT INTO first_admin (user_id) VALUES (?)";
		
		try (Connection connection = DatabaseConnection.getConnection();
			PreparedStatement stmt = connection.prepareStatement(sql)) {
			
			stmt.setLong(1, user.getId());
			stmt.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}


	// to check if the user has Special access role and we do this by using selct command
	public static boolean hasSpecialAccessRole(long userId, String role) {
		String sql = "SELECT COUNT(*) FROM special_access_group_users WHERE user_id = ? AND role = ?";
		
		try (Connection connection = DatabaseConnection.getConnection();
			PreparedStatement stmt = connection.prepareStatement(sql)) {
			
			stmt.setLong(1, userId);
			stmt.setString(2, role);
			ResultSet rs = stmt.exexuteQuery();
			
			if (rs.next()) {
				return rs.getInt(1) > 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
		
	}


	//We use this method to add the user details from article general table to special access group table and make sure that we have everything which is eeded
	public boolean addUserToSpecialAccessGroup(
			long userId,
			String userType,
			String specialRole,
			String username,
			String email,
			String firstName,
			String middleName,
			String lastName,
			String preferredName,
			String expertiseLevel,
			boolean isSetupComplete) {
		
		String sql = """
				INSERT INTO special_access_group_users (
					user_id, user_type, role, username, email, first_name, middle_name,
					last_name, preferred_name, expertise_level, is_setup_complete
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""";
		
		try (Connection connection = DatabaseConnection.getConnection();
			PreparedStatement stmt = connection.prepareStatement(sql)) {
			
			stmt.setLong(1, userId);
			stmt.setString(2, userType);
			stmt.setString(3, specialRole);
			stmt.setString(4, username);
			stmt.setString(5, email);
			stmt.setString(6, firstName);
			stmt.setString(7, middleName);
			stmt.setString(8, lastName);
			stmt.setString(9, preferredName);
			stmt.setString(10, expertiseLevel);
			stmt.setBoolean(11, isSetupComplete);
			
			int rowsAffected = stmt.executeUpdate();
			return rowsAffected > 0;
			
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
			
		}
	}

	public boolean isInSpecialAccessGroup(User user) {
		String sql = "SELECT COUNT(*) FROM special_access_group_users WHERE user_id = ?";
		
		
		try (Connection connection = DatabaseConnection.getConnection();
			PreparedStatement stmt = connection.prepareStatement(sql)) {
			
			stmt.setLong(1, user.getId());
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1) > 0; 
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
		
	}

	//this fucntions helps us remove the person from special access group if we want 
	public void removeUserFromSpecialAccessGroups(long userId) {
		String sql = "DELETE FROM special_access_group_users WHERE user_id = ?";
		try (Connection connection = DatabaseConnection.getConnection();
			PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setLong(1,userId);
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}

	//If we want to ever delete this first admin later we can s=do this by using ths methiod 
	public void removeUserFromFirstAdmin(long userId) {
		String sql = "DELETE FROM first_admin WHERE user_id = ?";
		try (Connection connection = DatabaseConnection.getConnection();
			PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setLong(1, userId);
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();		
		}
	}


	}
