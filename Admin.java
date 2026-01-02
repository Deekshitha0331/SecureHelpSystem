package application;

import java.time.LocalDateTime;

public class Admin extends User { 
	
	public Admin(String username, char[] passwordHash) {
		super(0, username, passwordHash, false, null, false, null, null, null, null, null, "Intermediate", null);
		addRole("Admin");
	}
	
	//Admin can invite a new user by generating a one-time password
	 public void inviteUser(UserManager userManager, String newUsername, char[] passwordHash, String role) {

	        User newUser = new User(0, newUsername, passwordHash, true, LocalDateTime.now().plusDays(1),
	                                false, null, null, null, null, null, "Intermediate", null);
	        newUser.addRole(role);
	        userManager.addUser(newUser);
	        System.out.println("User invited: " + newUsername + " with role: " + role);
	    }
	
// Admin can reset the password of a user using OTP
	public void resetPassword(UserManager userManager, String username) {
		User user = userManager.getUser(username);
		if (user != null) {
			userManager.generatePasswordResetOtp(user); // Generate a new OTP for password reset
			System.out.println("Password reset OTP generated for user: " + username);
		} else {
			System.out.println("User not found:" + username);
		}
	}
	
	//Admin can delete a user
	public void deleteUser(UserManager userManager, String username) {
		User user = userManager.getUser(username);
		if (user != null) {
			userManager.removeUser(user);
			System.out.println("User deleted: " + username);
		} else {
			System.out.println("User not found: " + username);
		}
	}
	
	// Admin can list all users and their roles

    public void listUsers(UserManager userManager) {
        for (User user : userManager.listUsers()) {
            System.out.println("Username: " + user.getUsername() +
                               ", Name: " + user.getDisplayName() +
                               ", Roles: " + String.join(", ", user.getRoles()));
        }
    }
	
	//Admin can update a user's details (assuming update functionality exists in UserManager)
	public void updateUserDetails(UserManager userManager, User user, String email, String firstName,
			String middleName, String lastName, String preferredName) {
		try {
            userManager.updateUserDetails(user, email, firstName, middleName, lastName, preferredName);
			System.out.println("User details updated for: " + user.getUsername());
		} catch (Exception e) {
			System.out.println("Error updating user details: " + e.getMessage());
			
		}
		}
	}