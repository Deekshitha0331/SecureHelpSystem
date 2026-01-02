package application;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// declared fields for User class.
public class User {
	private int id;
	private String username;
	private char[] passwordHash;  // char[] password for preventing cyberattacks.
	private boolean isOneTimePassword;
	private LocalDateTime otpExpiration;
	private boolean isSetupComplete;
	private String email;
	private String firstName;
	private String otp;
	private String middleName;
	private String lastName;
	private String preferredName;
	private Set<String> roles;
	private String expertiseLevel;
	private String currentRole;	
	
	// Constructor for creating a user object from database records
	public User(int id, String username, char[] passwordHash, boolean isOneTimePassword,
			LocalDateTime otpExpiration, boolean isSetupComplete, String email, String firstName,
			String middleName, String lastName, String preferredName, String expertiseLevel, Set<String> roles) {
		this.id = id;
		this.username = username;
		this.passwordHash = passwordHash;
		this.isOneTimePassword = isOneTimePassword;
		this.otpExpiration = otpExpiration;
		this.isSetupComplete = isSetupComplete;
		this.email = email;
		this.firstName = firstName;
		this.middleName = middleName;
		this.lastName = lastName;
		this.preferredName = preferredName;
		this.expertiseLevel = expertiseLevel;
		this.roles = roles != null ? roles : new HashSet<>();	
	}
	
	
	// Getters and Setters for all fields.

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public char[] getPasswordHash() {
		return  passwordHash;
	}

	
	public void setPasswordHash(char[] passwordHash) {
		this.passwordHash = passwordHash;
	}

	public boolean isOneTimePassword() {
		return isOneTimePassword;
	}

	public void setOneTimePassword(boolean isOneTimePassword) {
		this.isOneTimePassword = isOneTimePassword;
	}

	public String getOtp() {
		return otp;
	}

	public void setOtp(String otp) {
		this.otp = otp;
	}

	public LocalDateTime getOtpExpiration() {
		return otpExpiration;
	}

	public void setOtpExpiration(LocalDateTime otpExpiration) {
		this.otpExpiration = otpExpiration;
	}

	public boolean isSetupComplete() {
		return isSetupComplete;
	}

	public void setSetupComplete(boolean isSetupComplete) {
		this.isSetupComplete = isSetupComplete;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFristName(String firstName) {
		this.firstName = firstName;
	}

	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getPreferredName() {
		return preferredName;
	}

	public void setPrefferedname(String preferredName) {
		this.preferredName = preferredName;
	}

	public String getExpertiseLevel() {
		return expertiseLevel;
	}

	public void setExpertiseLevel(String expertiseLevel) {
		this.expertiseLevel = expertiseLevel;
	}

	public String getCurrentRole() {
		return currentRole;
	}

	public void setCurrentRole(String currentRole) {
		this.currentRole = currentRole;
	}

	public Set<String> getRoles() {
		return roles;
	}

	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}
	
	// Helper method to add a role.
	public void addRole(String role) {
		if (role != null && !role.trim().isEmpty()) {
			roles.add(role);
		}
	}
	
	// Helper method to check if user has a role.
	public boolean hasRole(String role) {
		return roles.contains(role);
	}

	// Get display name, using preferred name if available.
	public String getDisplayName() {
		return (preferredName != null && !preferredName.isEmpty()) ? preferredName : firstName;
	}

	// Override toString() for debugging.
	public String toString() {
		return "User{" +
				"id=" + id +
				", username='" + username + '\'' +
				", email='" + email + '\'' +
				", firstName='" + firstName + '\'' +
				", middleName='" + middleName + '\'' +
				", lastName='" + lastName + '\'' +
				", preferredName='" + preferredName + '\'' +
				", expertiseLevel='" + expertiseLevel + '\'' +
				", roles='" + roles + '\'' +
				", currentRole='" + currentRole + '\'' +
				'}';
	}

	// Override equals() and hashCode() for comparing User objects.
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		
		User user = (User) o;
		
		if (id != user.id) return false;
		return username != null ? username.equals(user.username) : user.username == null;
	}

	public int hashCode() {
		return id;
	}
}

