package application;

import java.io.File;
import java.nio.charset.StandardCharsets;
//importing necessary modules
import java.sql.Connection;
import java.sql.PreparedStatement;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javafx.scene.layout.HBox;

/*import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import application.SpecialAccessManager; */


public class LoginSystem extends Application{
	//Declaring variables to be used 
	private TextField usernameField;
	private PasswordField passwordField;
	private TextField invitationCodeField;
	private Button loginButton;
	private Button invitationCodeButton;
	private Label loginMessage;
	private UserManager userManager;
	private ArticleManager articleManager;
	private Stage mainStage;
	
	
	//whenever user will run the program this function will help them to show the appropriate menu.
	public void start(Stage primaryStage) {
		primaryStage.setTitle("Login");
		//initialize UserManager to interact with userdata.
		userManager = new UserManager();
		articleManager = new ArticleManager();
		mainStage = primaryStage;
		loginMessage = new Label();
		
		if (userManager.listUsers().isEmpty()) {
			openFirstUserSignUpPage();
			return;
		}
		
		usernameField = new TextField();
		usernameField.setPromptText("Username");
		
		passwordField = new PasswordField();
		passwordField.setPromptText("Password");
		
		invitationCodeField = new TextField();
		invitationCodeField.setPromptText("Enter Invitation Code");
		
		loginButton = new Button();
		loginButton.setOnAction(e -> login());
		
		invitationCodeButton =  new Button("Login with Invitation code");
		invitationCodeButton.setOnAction(e -> processInvitationCode());
		
		Button signUpButton = new Button("Sign Up");
		signUpButton.setOnAction(e -> openSignUpPage());
		
		// add elements to the layout.
		VBox layout = new VBox(10, usernameField,passwordField,invitationCodeField,loginButton,invitationCodeButton,signUpButton,loginMessage);
		layout.setAlignment(Pos.CENTER);
		Scene scene = new Scene(layout, 300, 250);
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
	//This is to create a home page 	
	private void openCreateAccountPage(User invitedUser) {
		//creating fields to make their own username
		Stage accountCreationStage=new Stage();
		accountCreationStage.setTitle("Create Your Account");
		
		TextField usernameField =new TextField();
		usernameField.setPromptText("Choose a Username");
		
		//password for them
		PasswordField passwordField= new PasswordField();
		passwordField.setPromptText("Choose a Password");
		
		PasswordField confirmPasswordField = new PasswordField();
		confirmPasswordField.setPromptText("Confirm Password");
		
		Label creationMessage =new Label();
		
		Button createAccountButton = new Button("Create Account");
		createAccountButton.setOnAction(e -> {
			String username= usernameField.getText().trim();
			String password =passwordField.getText();
			String confirmPassword = confirmPasswordField.getText();
			
			// username can't be empty
			if (username.isEmpty() || password.isEmpty()) {
				creationMessage.setText("Username and Password cannot be empty");
				return;
			}
			
			if (!password.equals(confirmPassword)) {
				creationMessage.setText("Passwords do not match");
				return;
			}
			
			invitedUser.setUsername(username);
			//for security we convert password to char
			invitedUser.setPasswordHash(password.toCharArray());
			invitedUser.setOneTimePassword(false);
			//updating credentials of the user
			userManager.updateUserCredentials(invitedUser);
			accountCreationStage.close();
			loginMessage.setText("Account created successfully! Please log in.");
		});
		
		VBox layout =new VBox(10,usernameField, passwordField,confirmPasswordField, createAccountButton,creationMessage);
		layout.setAlignment(Pos.CENTER);
		
		Scene scene =new Scene(layout, 300, 250);
		accountCreationStage.setScene(scene);
		accountCreationStage.show();
	}
	
	//method to process the invitation code during user onboarding
	private void processInvitationCode() {
		String invitationCode = invitationCodeField.getText().trim();
		if(invitationCode.isEmpty()) {
			loginMessage.setText("Invitation Code cannot be empty");
			return;
		}
		
		//fetch the user using the invitation code
		User invitedUser =userManager.getUserByInvitationCode(invitationCode);
		
		//check if the user is valid and still in OTP based
		if (invitedUser != null && invitedUser.isOneTimePassword()) {
			//checking if otp has not expired
			if (invitedUser.getOtpExpiration() != null && invitedUser.getOtpExpiration().isAfter(LocalDateTime.now())) {
				openCreateAccountPage(invitedUser);
			}
			else {
				loginMessage.setText("Invitation code has expired. Please contact Admin");
				
			}
		}
		else {
			loginMessage.setText("Invalid or expired invitation code");
		}
	}
	
	// Homepage method is there to make sure that each role percieves and sees the desired method
	//firstAdmin is also taken care of 
	private void openHomePage(User user) {
	    Stage homeStage = new Stage();
	    User currentUser = userManager.getUser(user.getUsername());

	    if (currentUser.getRoles() != null && !currentUser.getRoles().isEmpty()) {
	        if (currentUser.getCurrentRole() == null || currentUser.getCurrentRole().isEmpty()) {
	        	currentUser.setCurrentRole(currentUser.getRoles().iterator().next());
	        }
	    }

	    String roleToDisplay = currentUser.getCurrentRole() != null ? currentUser.getCurrentRole() : "Unknown Role";
	    homeStage.setTitle(roleToDisplay + " Home");

	    String displayName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Unknown Name";
	    Label welcomeMessage = new Label("Welcome, " + displayName + " (" + roleToDisplay + ")");
	    Label expertiseLabel = new Label("Expertise Level: " + currentUser.getExpertiseLevel());

	    Button logoutButton = new Button("Log Out");
	    logoutButton.setOnAction(e -> {
	        homeStage.close();
	        mainStage.show();
	    });

	    VBox homeLayout = new VBox(10, welcomeMessage, expertiseLabel, logoutButton);
	    homeLayout.setAlignment(Pos.CENTER);
	    
	    boolean hasSpecialAccess = userManager.isInSpecialAccessGroup(currentUser);

	    // Display first admin specific
	    if (currentUser.hasRole("Admin") & currentUser.isFirstAdmin()) {
	        Button inviteUserButton = new Button("Invite User");
	        inviteUserButton.setOnAction(e -> inviteUser());

	        Button resetPasswordButton = new Button("Reset User Password");
	        resetPasswordButton.setOnAction(e -> resetUserPassword());

	        Button deleteUserButton = new Button("Delete User");
	        deleteUserButton.setOnAction(e -> deleteUser());

	        Button listUsersButton = new Button("List Users");
	        listUsersButton.setOnAction(e -> listUsers());

	        Button manageRolesButton = new Button("Manage User Roles");
	        manageRolesButton.setOnAction(e -> openManageRolesPage());

	        // New "Articles" button to open the article management window
	        Button articlesButton = new Button("Articles");
	        articlesButton.setOnAction(e -> openArticlesWindow());
	        
	        homeLayout.getChildren().addAll(inviteUserButton, resetPasswordButton, deleteUserButton, listUsersButton, manageRolesButton, articlesButton);
	    
	        if (hasSpecialAccess) {
	        	Button specialAccessGroupsButton = new Button("Special Access Groups");
	        	specialAccessGroupsButton.setOnAction(e -> openSpecialAccessGroupsWindow(currentUser));
	        	homeLayout.getChildren().add(specialAccessGroupsButton);
	        }
	    } 
	    
		//if he is just a admin
	    if (currentUser.hasRole("Admin") & !currentUser.isFirstAdmin()) {
	        Button inviteUserButton = new Button("Invite User");
	        inviteUserButton.setOnAction(e -> inviteUser());

	        Button resetPasswordButton = new Button("Reset User Password");
	        resetPasswordButton.setOnAction(e -> resetUserPassword());

	        Button deleteUserButton = new Button("Delete User");
	        deleteUserButton.setOnAction(e -> deleteUser());

	        Button listUsersButton = new Button("List Users");
	        listUsersButton.setOnAction(e -> listUsers());

	        Button manageRolesButton = new Button("Manage User Roles");
	        manageRolesButton.setOnAction(e -> openManageRolesPage());

	        // New "Articles" button to open the article management window
	        Button articlesButton = new Button("Articles");
	        articlesButton.setOnAction(e -> openArticlesWindowForAdmins());
	        
	        homeLayout.getChildren().addAll(inviteUserButton, resetPasswordButton, deleteUserButton, listUsersButton, manageRolesButton, articlesButton);
	    
	        if (hasSpecialAccess) {
	        	Button specialAccessGroupsButton = new Button("Special Access Groups");
	        	specialAccessGroupsButton.setOnAction(e -> openSpecialAccessGroupsWindow(currentUser));
	        	homeLayout.getChildren().add(specialAccessGroupsButton);
	        }
	    }
	    //for instructor 
	    if (currentUser.hasRole("Instructor")) {
	        Button articlesButton = new Button("Articles");
	        articlesButton.setOnAction(e -> openArticlesWindow());
	        homeLayout.getChildren().addAll(articlesButton);
	        
	        Button searchAndViewButton = new Button("Search and View");
	        searchAndViewButton.setOnAction(e -> openSearchAndViewArticles());
	        homeLayout.getChildren().add(searchAndViewButton);
	        
	        Button viewAllMessagesButton = new Button("View All Student Messages");
	        viewAllMessagesButton.setOnAction(e -> openAllStudentMessagesWindow());
	        
	        homeLayout.getChildren().add(viewAllMessagesButton);
	        
	        Button manageStudentButton = new Button("Manage Student(View and Delete)");
	        manageStudentButton.setOnAction(e -> openManageStudentsForInstructor());
	        
	        homeLayout.getChildren().add(manageStudentButton);
	        
	        if (hasSpecialAccess) {
	        	Button specialAccessGroupsButton = new Button("Special Access Groups");
	        	specialAccessGroupsButton.setOnAction(e -> openSpecialAccessGroupsWindow(currentUser));
	        	homeLayout.getChildren().add(specialAccessGroupsButton);
	        }
	        
	    } 
	    //for students
	    if (currentUser.hasRole("Student")) {
	        
	    	Button helpMesasageButton =  new Button("Send Help Message");
	    	helpMesasageButton.setOnAction(e -> openHelpMessageWindow(currentUser));
	    	
	    	Button viewMesasageButton =  new Button("View Sent Messages");
	    	viewMesasageButton.setOnAction(e -> openStudentMessageWindow(currentUser));
	    	
	    	Button listArticlesButton =  new Button("List Articles");
	    	listArticlesButton.setOnAction(e -> openListArticlesPage());
	    	
	    	Button searchAndViewButton =  new Button("Search And View");
	    	searchAndViewButton.setOnAction(e -> openSearchAndViewArticles());
	    	
	    	homeLayout.getChildren().addAll(helpMesasageButton,viewMesasageButton,listArticlesButton,searchAndViewButton);
	    	
	    	if (hasSpecialAccess) {
	        	Button specialAccessGroupsButton = new Button("Special Access Groups");
	        	specialAccessGroupsButton.setOnAction(e -> openSpecialAccessGroupsWindow(currentUser));
	        	homeLayout.getChildren().add(specialAccessGroupsButton);
	        }
	    }

	    Scene homeScene = new Scene(homeLayout, 400, 400);
	    homeStage.setScene(homeScene);
	    homeStage.show();
	}
	
	// to display all student messages retrieved for the database. Can view details like student ID, message type, and timeStamp.
	private void openAllStudentMessagesWindow() {
		Stage messagesStage = new Stage();
		messagesStage.setTitle("All Student Messages");
		
		TextArea messagesArea = new TextArea();
		messagesArea.setEditable(false);
		
		List<Map<String, Object>> messages = articleManager.getAllStudentMessages();
		
		if (messages.isEmpty()) {
			messagesArea.setText("No messages sent by students.");
		} else {
			StringBuilder content = new StringBuilder();
			for (Map<String, Object> message : messages) {
				content.append("Student ID: ").append(message.get("student_id")).append("\n")
					   .append("Message ID: ").append(message.get("message_id")).append("\n")
					   .append("Type: ").append(message.get("message_type")).append("\n")
					   .append("Message: ").append(message.get("message_text")).append("\n")
					   .append("Date: ").append(message.get("created_at")).append("\n")
					   .append("---------------------------------------------\n");
			}
			messagesArea.setText(content.toString());
		}
		VBox layout = new VBox(10, messagesArea);
		layout.setAlignment(Pos.CENTER);
		Scene scene = new Scene(layout, 500, 500);
		messagesStage.setScene(scene);
		messagesStage.show();			
	}
	
	// search and view is used to see the article primarly built for students and instrcutors 
	private void openSearchAndViewArticles() {
		Stage searchStage = new Stage();
		searchStage.setTitle("Search and View Articles");
		
		TextField searchField = new TextField();
		searchField.setPromptText("Search by Title r Abstract (e.g., 'Java Basics')");
		
		TextField descriptionField = new TextField();
		descriptionField.setPromptText("Search by Description (optional)");
		
		TextField groupField = new TextField();
		groupField.setPromptText("Filter by Group (e.g., 'Assignments', 'Help')");
		
		TextArea resultArea = new TextArea();
		resultArea.setEditable(false);
		
		TextField viewByIdField = new TextField();
		viewByIdField.setPromptText("Enter Article ID to View Details");
		
		Button searchButton = new Button("Search");
		searchButton.setOnAction(e -> {
			String searchQuery = searchField.getText().trim();
			String descriptionQuery = descriptionField.getText().trim();
			String group = groupField.getText().trim();
			
			if (searchQuery.isEmpty() && descriptionQuery.isEmpty() && group.isEmpty()) {
				resultArea.setText("Please provide a search query, description, or group filter.");
				return;
			}
			
			List<Article> articles = new ArrayList<>();
			
			if (!searchQuery.isEmpty()) {
				articles.addAll(articleManager.searchArticlesByTitle(searchQuery));
			}
			
			if (!descriptionQuery.isEmpty()) {
				articles.addAll(articleManager.searchArticlesByDescription(descriptionQuery));
			}
			
			if (!group.isEmpty()) {
				articles.addAll(articleManager.searchArticlesByGroup(group));
			}
			
			resultArea.clear();
			if (!group.isEmpty()) {
				resultArea.appendText("Currently Active Group: " + group + "\n");
			}
			
			if (articles.isEmpty()) {
				resultArea.appendText("No articles found matching the criteria.\n");
			} else {
				Map<String, Long> levelCount = articles.stream()
						.collect(Collectors.groupingBy(Article::getLevel, Collectors.counting()));
				levelCount.forEach((level, count) -> resultArea.appendText("Level " + level + ": " + count + " articles\n"));
				
				resultArea.appendText("\nMatching Articles:\n");
				for (Article article : articles) {
					resultArea.appendText("ID: " + article.getId() + ", Title: " + article.getTitle() + ", Abstract: " + article.getDescription() + "\n");
				}
			}
		});
		
		Button viewByIdButton = new Button("View Article Details by ID");
		viewByIdButton.setOnAction(e -> {
			try {
				String input = viewByIdField.getText().trim();
				long articleId = Long.parseLong(input);
				
				Article article = articleManager.getArticleById(articleId);
				if (article == null) {
					resultArea.setText("No articles found with the given ID.");
					return;
				}
				
				displayArticleDetail(article);
			} catch (NumberFormatException ex){
				resultArea.setText("Enter a valid article ID.");
			}
		});
		
		VBox layout = new VBox(10, searchField, descriptionField, groupField, searchButton, resultArea, viewByIdField, viewByIdButton);
		layout.setAlignment(Pos.CENTER);
		Scene scene = new Scene(layout, 600, 600);
		searchStage.setScene(scene);
		searchStage.show();		
	}
	
	// Opens a window to search and filter articles by title, description, or group and view their details. 
	private void displayArticleDetail(Article article) {
		Stage detailStage = new Stage();
		detailStage.setTitle("Article Details");
		
		TextArea detailArea = new TextArea();
		detailArea.setEditable(false);
		detailArea.setWrapText(true);
		
		StringBuilder details = new StringBuilder();
		details.append("Title: ").append(article.getTitle()).append("\n")
			   .append("Level: ").append(article.getLevel()).append("\n")
			   .append("Description: ").append(article.getDescription()).append("\n\n")
			   .append("Content:\n ").append(article.getContent()).append("\n\n");
		
		if (article.getGroupIdentifiers() != null && article.getGroupIdentifiers().isEmpty()) {
			details.append("Groups: ").append(String.join(",", article.getGroupIdentifiers())).append("\n");
		}
		
		if (article.getKeywords() != null && article.getKeywords().isEmpty()) {
			details.append("Keywords: ").append(String.join(",", article.getKeywords())).append("\n");
		}
		
		if (article.getLinks() != null && article.getLinks().isEmpty()) {
			details.append("Links: ").append(String.join(",", article.getLinks())).append("\n");
		}
		
		detailArea.setText(details.toString());
		
		VBox layout = new VBox(10, detailArea);
		layout.setAlignment(Pos.CENTER);
		Scene scene = new Scene(layout, 600, 400);
		detailStage.setScene(scene);
		detailStage.show();	
	}
	
	// displays details like message ID, type, content and date of messages sent by the current student.
	private void openStudentMessagesWindow(User currentUser) {
		Stage messageStage = new Stage();
		messageStage.setTitle("Sent Messages");
		
		TextArea messageArea = new TextArea();
		messageArea.setWrapText(true);
		messageArea.setEditable(false);
		
		List<Map<String, Object>> messages = articleManager.getHelpMessagesByStudentId(currentUser.getId());
		
		StringBuilder messageContent = new StringBuilder();
		for (Map<String, Object> message : messages) {
			messageContent.append("Message ID: ").append(message.get("message_id")).append("\n")
			   			  .append("Type: ").append(message.get("message_type")).append("\n")
			   			  .append("Message: ").append(message.get("message_text")).append("\n")
			   			  .append("Date: ").append(message.get("created_at")).append("\n")
			   			  .append("--------------------------------------------------\n\n");
		}
		
		messageArea.setText(messageContent.toString());
		
		VBox layout = new VBox(messageArea);
		layout.setAlignment(Pos.CENTER);
		Scene scene = new Scene(layout, 400, 400);
		messageStage.setScene(scene);
		messageStage.show();		
	}
	
	// opens a window where students can compose and send help messages.
	private void openHelpMessageWindow(User currentUser) {
		Stage helpStage = new Stage();
		helpStage.setTitle("Send Help Messages");
		
		Label instructionLabel = new Label("Select Message Type and Enter Message:");
		
		RadioButton genericMessageRadio = new RadioButton("Generic");
		RadioButton specificMessageRadio = new RadioButton("Specific");
		ToggleGroup messageTypeGroup = new ToggleGroup();
		genericMessageRadio.setToggleGroup(messageTypeGroup);
		specificMessageRadio.setToggleGroup(messageTypeGroup);
		
		TextArea messageTextArea = new TextArea();
		messageTextArea.setPromptText("Enter your message here...");
		
		Button sendButton = new Button("Send Message");
		Label statusLabel = new Label();
		
		sendButton.setOnAction(e -> {
			String messageText = messageTextArea.getText().trim();
			if (messageText.isEmpty()) {
				statusLabel.setText("Message cannot be empty.");
				return;
			}
			
			String messageType = genericMessageRadio.isSelected() ? "generic" : "specific";
			boolean success = articleManager.addStudentHelpMessage(currentUser.getId(), messageType, messageText);
			
			if (success) {
				statusLabel.setText("Message sent successfully.");
			} else {
				statusLabel.setText("Failed to send message.");
			}
		});
		
		VBox layout = new VBox(10, instructionLabel, genericMessageRadio, specificMessageRadio ,messageTextArea, sendButton, statusLabel);
		layout.setAlignment(Pos.CENTER);
		Scene scene = new Scene(layout, 400, 300);
		helpStage.setScene(scene);
		helpStage.show();
		
	}
	
	// opens a windows for managing special access groups, allwing admins to add users if they have admin rights add, update, delete and backup articles. It also allows for a view-only option.
	private void openSpecialAccessGroupsWindow(User user) {
		Stage specialAccessStage = new Stage();
		specialAccessStage.setTitle("Special Access Groups");
		
		VBox layout = new VBox(10);
		layout.setAlignment(Pos.CENTER);
		
		if (user.hasSpecialAccessRole("admin") || user.isFirstAdmin()) {
			Button addUserButton = new Button("Add User to Special Access Group");
			addUserButton.setOnAction(e -> openAddUserToSpecialAccessGroup());
			
			Button addArticleButton = new Button("Add Article to Special Access Group");
			addArticleButton.setOnAction(e -> openAddArticleToSpecialAccessGroup());
			
			Button listSpecialAccessArticlesButton = new Button("List Special Access Article");
			listSpecialAccessArticlesButton.setOnAction(e -> openViewSpecialAccessArticles());
			
			Button updateSpecialAccessButton = new Button("Update Special Access Article");
			updateSpecialAccessButton.setOnAction(e -> openUpdateSpecialAccessArticlesPage());
			
			Button deleteSpecialAccessButton = new Button("Delete Special Access Article");
			deleteSpecialAccessButton.setOnAction(e -> openDeleteSpecialAccessArticlesPage());
			
			Button deleteByGroupButton = new Button("Delete Articles by Group");
			deleteByGroupButton.setOnAction(e -> openDeleteSpecialAccessArticlesByGroupPage());
			
			Button backupSpecialAllButton = new Button("Backup All Special Access Articles");
			backupSpecialAllButton.setOnAction(e -> {
				TextInputDialog dialog = new TextInputDialog("special_access_backup.csv");
				dialog.setTitle("Backup All Special Access Articles");
				dialog.setHeaderText("Enter the file name for the backup:");
				dialog.setContentText("File Name:");
				
				dialog.showAndWait().ifPresent(fileName -> {
					articleManager.backupAllSpecialAccessArticles(fileName);
					showAlert("Backup Complete", "All special access articles have been backed up to: " + filename);
				});
			});
			
			Button backupSpecialByGroupButton = new Button("Backup Special Articles by Group");
			backupSpecialByGroupButton.setOnAction(e -> {
				TextInputDialog groupDialog = new TextInputDialog();
				groupDialog.setTitle("Backup Special Articles by Group");
				groupDialog.setHeaderText("Enter the Group Name for Backup:");
				groupDialog.setContentText("Group Name:");
				
				groupDialog.showAndWait().ifPresent(groupName -> {
					TextInputDialog fileDialog = new TextInputDialog(groupName + "_special_backup.csv");
					fileDialog.setTitle("Backup by Group");
					fileDialog.setHeaderText("Enter the file name for the group backup:");
					fileDialog.setContentText("File Name:");
					
					fileDialog.showAndWait().ifPresent(filename -> {
						articleManager.backupSpecialAccessArticlesByGroup(groupName, fileName);
						showAlert("Backup COmplete", "Group '" + groupName + "' backed up to: " + fileName);
					});
				});
			});
			
			Button restoreSpecialButton = new Button("Restore Special Access Articles");
			restoreSpecialButton.setOnAction(e -> {
				TextInputDialog dialog = new TextInputDialog();
				dialog.setTitle("Restore Special Access Articles");
				dialog.setHeaderText("Enter the name of the backup file to restore:");
				dialog.setContentText("File Name:");
				
				dialog.showAndWait().ifPresent(fileName -> {
					boolean replaceExisting = showConfirmationDialog(
							"Restore Mode",
							"Do you eant to replace existing special access articles with the restored data?"
							);
					articleManager.restoreSpecialAccessArticlesFromBackup(fileName, replaceExisting);
					showAlert("Restore Complete", "Special access articles restored from: " + filename);
				});
			});
			
			layout.getChildren().addAll(addUserButton, addArticleButton, listSpecialAccessArticlesButton, updateSpecialAccessButton, deleteSpecialAccessButton, deleteByGroupButton, backupSpecialAllButton, backupSpecialByGroupButton, restoreSpecialButton);
		}
		
		if (user.hasSpecialAccessRole("view") || user.hasRole("Instructor") || user.isFirstAdmin()) {
			Button viewArticleByIdButton = new Button("View Special Access Article by ID");
			viewArticleByIdButton.setOnAction(e -> openViewSpecialAccessAricleById());
			layout.getChildren().add(viewArticleByIdButton);
		}
		
		Scene scene = new Scene(layout, 500, 400);
		specialAccessStage.setScene(scene);
		specialAccessStage.show();
	}
	
	// opens a windows for deleting special access articles by their ID.
	private void openDeleteSpecialAccessArticlesPage() {
		Stage deleteStage = new Stage();
		deleteStage.setTitle("Delete Special Access Articles");
		
		TextField specialIdField = new TextField();
		specialIdField.setPromptText("Enter Special Group Article ID (required)");
		
		Label messageLabel = new Label();
		
		Button deleteButton = new Button("Delete");
		deleteButton.setOnAction(e -> {
			String specialIdText = specialIdField.getText().trim();
			
			if (specialIdText.isEmpty()) {
				messageLabel.setText("Please provide a Special Group Article ID.");
				return;
			}
			
			try {
				long specialGroupArticleId = Long.parseLong(specialIdText);
				boolean success = articleManager.deleteSpecialAccessArticleBySpecialId(specialGroupArticleId);
				
				if (success) {
					messageLabel.setText("Special access article deleted successfully.");
				} else {
					messageLabel.setText("Failed to delete. Ensure the ID exists.");
				}
			} catch (NumberFormatException ex) {
				messageLabel.setText("Invalid ID. Please enter a numeric value.");
			}
		});
		
		VBox layout = new VBox(10, specialIdField, deleteButton, messageLabel);
		layout.setAlignment(Pos.CENTER);
		Scene scene = new Scene(layout, 400, 200);
		deleteStage.setScene(scene);
		deleteStage.show();
	}
	
	// opens a window to update the details of a special access article by its ID. Allows for editing the fields.
	private void openUpdateSpecialAccessArticlesPage() {
		Stage updateStage = new Stage();
		updateStage.setTitle("Update Special Access Article");
		
		TextField specialArticleIdField = new TextField();
		specialArticleIdField.setPromptText("Enter Special Group Article ID");
		
		Button loadArticleButton = new Button("Load Article");
		
		TextField titleField = new TextField();
		titleField.setPromptText("Enter Title");
		
		ComboBox<String> levelComboBox = new ComboBox<>();
		levelComboBox.getItems().addAll("Beginner", "Intermediate", "Advanced", "Expert");
		
		TextArea descriptionArea = new TextArea();
		descriptionArea.setPromptText("Enter Description");
		
		TextArea contentArea = new TextArea();
		contentArea.setPromptText("Enter Content");
		
		TextField groupNameField = new TextField();
		groupNameField.setPromptText("Enter Group Name");
		
		TextField keywordsField = new TextField();
		keywordsField.setPromptText("Enter Keywords (comma-separated)");
		
		TextField linksField = new TextField();
		linksField.setPromptText("Enter Links (comma-separated)");
		
		Button updateButton = new Button("Update Article");
		
		Label messageLabel = new Label();
		
		loadArticleButton.setOnAction(event -> {
			String articleIdText = specialArticleIdField.getText().trim();
			if (!articleIdText.isEmpty()) {
				try {
					long specialArticleId = Long.parseLong(articleIdText);
					SpecialAccessArticle article = articleManager.getSpecialAccessArticleById(specialArticleId);
					
					if (article != null) {
						titleField.setText(article.getTitle());
						levelComboBox.setValue(article.getLevel());
						descriptionArea.setText(article.getDescription());
						contentArea.setText(article.getContent());
						groupNameField.setText(article.getGroupName());
						keywordsField.setText(String.join(",", article.getKeywords()));
						linksField.setText(String.join(",", article.getLinks()));
					} else {
						messageLabel.setText("Special Access Article not found.");
					}
				} catch (NumberFormatException ex) {
					messageLabel.setText("Invalid Special Group Article ID.");
				}
			}
		});
		
		updateButton.setOnAction(e -> {
			try {
				long specialArticleId = Long.parseLong(specialArticleIdField.getText().trim());
				String title = titleField.getText().trim();
				String level = levelComboBox.getValue();
				String description = descriptionArea.getText().trim();
				String content = contentArea.getText().trim();
				String groupName = groupNameField.getText().trim();
				List<String> keywords = parseCommaSeparatedInput(keywordsField.getText().trim());
				List<String> links = parseCommaSeparatedInput(linksField.getText().trim());
				
				boolean success = articleManager.updateSpecialAccessArticleById(specialArticleId, title, level, description, content, groupName, keywords, links);
				
				if (success) {
					messageLabel.setText("Special Access Article updated successfully.");
				} else {
					messageLabel.setText("Duplicate Special Access Article detected. Update cancelled.");
				}
			} catch (NumberFormatException ex ){
				messageLabel.setText("Invalid Special Group Article ID.");
			} catch (Exception ex) {
				messageLabel.setText("An unexpected error occurred.");
				ex.printStackTrace();
			}
		});
		VBox layout = new VBox(10, specialArticleIdField, loadArticleButton, titleField, levelComboBox, descriptionArea, contentArea, groupNameField, keywordsField, linksField, updateButton, messageLabel);
		layout.setAlignment(Pos.CENTER);
		Scene scene = new Scene(layout, 400, 600);
		updateStage.setScene(scene);
		updateStage.show();		
	}
	
	// opens a windows to add a user to a special access group. Allows to select user,type and role.
	private void openAddUserToSpcialAccessGroup() {
		Stage addUserStage = new Stage();
		addUserStage.setTitle("Add User to Special Access");
		
		ComboBox<User> userDropdown = new ComboBox<>();
		userDropdown.getItems().addAll(userManager.listUsers());
		userDropdown.setPromptText("Select User");
		
		ComboBox<String> userTypeDropdown = new ComboBox<>();
		userTypeDropdown.getItems().addAll("student", "instructor", "admin");
		userTypeDropdown.setPromptText("Select User Type");
		
		ComboBox<String> roleDropdown = new ComboBox<>();
		roleDropdown.getItems().addAll("view", "admin");
		roleDropdown.setPromptText("Select Special Role");
		
		Button addUserButton = new Button("Add User");
		Label messageLabel = new Label();
		
		addUserButton.setOnAction(e -> {
			User selectedUser = userDropdown.getValue();
			String selectedUserType = userTypeDropdown.getValue();
			String selectedRole = roleDropdown.getValue();
			
			if (selectedUser != null && selectedUserType != null && selectedRole != null) {
				boolean success = userManager.addUserToSpecialAccessGroup(
						selectedUser.getId(),
						selectedUserType,
						selectedUser.getUsername(),
						selectedUser.getEmail(),
						selectedUser.getFirstName(),
						selectedUser.getMiddleName(),
						selectedUser.getLastName(),
						selectedUser.getPreferredName(),
						selectedUser.getExpertiseLevel(),
						selectedUser.isSetupComplete()
				);
				messageLabel.setText(success ? "User added successfully." : "Failed to add user.");				
			} else {
				messageLabel.setText("Please fill in all required fields.");
			}
		});
		
		VBox layout = new VBox(10, userDropdown, userTypeDropdown, roleDropdown, addUserButton, messageLabel);
		layout.setAlignment(Pos.CENTER);
		Scene scene = new Scene(layout, 400, 300);
		addUserStage.setScene(scene);
		addUserStage.show();
	}
	// opens a window to add an article to a special access group. 
	private void openAddArticleToSpecialAccessGroup() {
		Stage addArticleStage = new Stage();
		addArticleStage.setTitle("Add Article to Special Access Group");
		
		RadioButton byArticleIdRadio = new RadioButton("Add by Article ID");
		RadioButton byGroupNameRadio = new RadioButton("Add by Group Name");
		ToggleGroup toggleGroup = new ToggleGroup();
		byArticleIdRadio.setToggleGroup(toggleGroup);
		byGroupNameRadio.setToggleGroup(toggleGroup);
		
		TextField articleIdField = new TextField();
		articleIdField.setPromptText("Enter Article ID");
		
		TextField groupNameField = new TextField();
		groupNameField.setPromptText("Enter Group Name(s), e.g., mac, windows or mac&windows");
		
		articleIdField.setDisable(true);
		groupNameField.setDisable(true);
		
		byArticleIdRadio.setOnAction(e -> {
			articleIdField.setDisable(false);
			groupNameField.setDisable(true);
		});
		
		byGroupNameRadio.setOnAction(e -> {
			articleIdField.setDisable(true);
			groupNameField.setDisable(false);
		});
		
		Button addArticleButton = new Button("Add Article");
		Label messageLabel = new Label();
		
		addArticleButton.setOnAction(e -> {
			boolean success = false;
			
			Long articleId = null;
			if (!articleIdField.getText().isEmpty()) {
				articleId = Long.parseLong(articleIdField.getText());
			}
			String groupNameInput = groupNameField.getText();
			
			if (byArticleIdRadio.isSelected() && articleId != null) {
				success = articleManager.addArticleToSpecialAccessGroupByCriteria(null, articleId);
			} else if (byGroupNameRadio.isSelected() && groupNameInput != null && !groupNameInput.isEmpty()) {
				List<List<String>> groupFilters = Arrays.stream(groupNameInput.split(",|&"))
						.map(String::trim)
						.map(Collections::singletonList)
						.collect(Collectors.toList());
				success = srticleManager.addArticleToSpecialAccessGroupByCriteria(groupFilters, null);
			} else {
				messageLabel.setText("Please select an option and enter the necessary information.");
				return;
			}
			
			messageLabel.setText(success ? "Article added successfully." : "Failed to add article.");
		});
		
		VBox layout = new VBox(10, byArticleIdRadio, articleIdField, byGroupNameRadio, groupNameField, addArticleButton, messageLabel);
		layout.setAlignment(Pos.CENTER);
		Scene scene = new Scene(layout, 400, 300);
		addArticleStage.setScene(scene);
		addArticleStage.show();
	}
	
	// opens a window to delete a special access article by ID.
	private void openDeleteSpecialAccessArticleByIdPage() {
		Stage deleteStage = new Stage();
		deleteStage.setTitle("Delete Special Access Article by ID");
		
		TextField idField = new TextField();
		idField.setPromptText("Enter Article ID");
		
		Label messageLabel = new Label();
		
		Button deleteButton = new Button("Delete");
		deleteButton.setOnAction(e -> {
			try {
				long articleId = Long.parseLong(idField.getText().trim());
				if (articleManager.deleteSpecialAccessArticleBySpecialId(articleId)) {
					messageLabel.setText("Special access article deleted successfully.");
				} else {
					messageLabel.setText("Failed to delete article. Article may not exist.");
				}
			} catch (NumberFormatException ex) {
				messageLabel.setText("Invalid ID. Please enter a numeric value.");
			}
		});
		
		VBox layout = new VBox(10, idField, deleteButton, messageLabel);
		layout.setAlignment(Pos.CENTER);
		Scene scene = new Scene(layout, 300, 200);
		deleteStage.setScene(scene);
		deleteStage.show();
	}
	
	// opens a window to delete special access articles by group name.
	private void openDeleteSpecialAccessArticleByGroupPage() {
		Stage deleteStage = new Stage();
		deleteStage.setTitle("Delete Special Access Article by Group");
		
		TextField groupField = new TextField();
		groupField.setPromptText("Enter Group Name");
		
		Label messageLabel = new Label();
		
		Button deleteButton = new Button("Delete");
		deleteButton.setOnAction(e -> {
			String groupName = groupField.getText().trim();
				
			if (groupName.isEmpty()) {
					messageLabel.setText("Please provide a group name.");
					return;
			} 
			boolean success = articleManager.deleteSpecialAccessArticlesByGroup(groupName);
			
			if (success) {
				messageLabel.setText("Special access articles in group '" + groupName + "' deleted successfully.");
			} else {
				messageLabel.setText("Failed to delete. Ensure the group exists or contians articles.");
			}
		});
		
		VBox layout = new VBox(10, groupField, deleteButton, messageLabel);
		layout.setAlignment(Pos.CENTER);
		Scene scene = new Scene(layout, 400, 200);
		deleteStage.setScene(scene);
		deleteStage.show();
	}
	
	// encrypts a string using caesar cipher.
	private String encryptContent(String content) {
		int shift = 3;
		StringBuilder encrypted = new StringBuilder();
		
		for (char c : content.toCharArray()) {
			encrypted.append((char) (c + shift));
		}
		return encrypted.toString();
	}
	
	// decrypts a string reversing caesar cipher.
	private String decryptContent(String encryptedcontent) {
		int shift = 3;
		StringBuilder decrypted = new StringBuilder();
		
		for (char c : encryptedcontent.toCharArray()) {
			decrypted.append((char) (c - shift));
		}
		return decrypted.toString();
	}
	
	// displays details of a specific special access article you can choose either list all article or you can also view by ID
	private void openViewSpecialAccessArticles() {
		Stage viewStage = new Stage();
		viewStage.setTitle("List All Special Access Articles");
		
		TextField idField = new TextField();
		idField.setPromptText("Enter Special Group Articles ID");
		
		TextArea articleDetails = new TextArea();
		articleDetails.setWrapText(true);
		articleDetails.setWrapText(false);
		
		Label messageLabel = new Label();
		
		Button viewButton = new Button("View Article");
		viewButton.setOnAction(e -> {
			String idText = idField.getText().trim();
			
			if (idText.isEmpty()) {
				messageLabel.setText("Please provide a Special Group Aricle ID.");
				return;
			}
			
			try {
				long specialGroupArticleId = Long.parseLong(idText);
				SpecialAccessArticle article = articleManager.getSpecialAccessArticleBySpecialId(specialGroupArticleId);
				
				if (article == null) {
					messageLabel.setText("Article not found.");
					articleDetails.clear();
					return;
				}
				
				StringBuilder details = new StringBuilder();
				details.append("Special Group Article ID: ").append(article.getSpecialGroupArticleId()).append("\n")
					   .append("Title: ").append(article.getTitle()).append("\n")
					   .append("Level: ").append(article.getLevel()).append("\n")
					   .append("Description: ").append(article.getDescription()).append("\n")
					   .append("Content: ").append(article.getContent()).append("\n")
					   .append("Private Note: ").append(article.getPrivateNote() != null ? article.getPrivateNote() : "N/A").append("\n")
					   .append("Group Name: ").append(article.getGroupName()).append("\n")
					   .append("Keywords: ").append(String.join(", ", article.getKeywords())).append("\n")
					   .append("Links: ").append(String.join(", ", article.getLinks())).append("\n\n");
				
				articleDetails.setText(details.toString());
				messageLabel.setText("Article loaded successfully.");
			} catch (NumberFormatException ex) {
				messageLabel.setText("Invalid ID. Please enter a numeric value.");
			}
		});
		
		VBox layout = new VBox(10, idField, viewButton, articleDetails, messageLabel);
		layout.setAlignment(Pos.CENTER);
		Scene scene = new Scene(layout, 400, 400);
		viewStage.setScene(scene);
		viewStage.show();
	}
	
	// allows an admin to view details of a specific article by its ID.
	private void openViewArticlePageForAdmin() {
		Stage viewArticleStage = new Stage();
		viewArticleStage.setTitle("View Article");
		
		TextField idField = new TextField();
		idField.setPromptText("Enter Article ID");
		
		TextArea articleContentArea = new TextArea();
		articleContentArea.setEditable(false);
		
		Label messageLabel = new Label();
		
		Button viewButton = new Button("View");
		viewButton.setOnAction(e -> {
			try {
				long articleId = Long.parseLong(idField.getText().trim());
				Article article = articleManager.getArticleById(articleId);
				
				if (article == null) {
					messageLabel.setText("Article not found.");
					articleContentArea.clear();
					return;
				}
				
				StringBuilder content = new StringBuilder();
				content.append("ID: ").append(article.getId()).append("\n");
				content.append("Title: ").append(article.getTitle()).append("\n");
				content.append("Level: ").append(article.getLevel()).append("\n");
				content.append("Description: ").append(article.getDescription()).append("\n");
				content.append("Content: ").append("RESTRICTED\n");
				content.append("Private Note: ").append("RESTRICTED\n");
				content.append("Groups: ").append(String.join(", ", article.getGroupIdetifiers())).append("\n");
				content.append("Keywords: ").append(String.join(", ", article.getKeywords())).append("\n");
				content.append("Links: ").append(String.join(", ", article.getLinks())).append("\n");
				
				articleContentArea.setText(content.toString());
				messageLabel.setText("Article loaded successfully.");
			} catch (NumberFormatException ex) {
				messageLabel.setText("Invalid ID. Please enter a valid number.");
			}
		});
		
		VBox layout = new VBox(10, idField, viewButton, articleContentArea, messageLabel);
		layout.setAlignment(Pos.CENTER);
		Scene scene = new Scene(layout, 400, 400);
		viewArticleStage.setScene(scene);
		viewArticleStage.show();
	}
	
	
	// opens the admins article management window providing options to add, view, delelete, list, backup, and restore articles.
	//main thing here is admin sees view differently he will see restricted for body
	private void openArticleWindowForAdmins() {
		Stage articlesStage = new Stage();
		articlesStage.setTitle("Articles");
		
		Button addArticleButton = new Button("Add Article");
		addArticleButton.setOnAction(e -> openAddArticleForm());
		
		Button listArticlesButton = new Button("List Articles");
		listArticlesButton.setOnAction(e -> openListArticlesPage());
		
		Button deleteArticlesButton = new Button("Delete Articles");
		deleteArticlesButton.setOnAction(e -> openDeleteArticlesPage());
		
		Button viewArticleButton = new Button("View Article");
		viewArticleButton.setOnAction(e -> openViewArticlePageForAdmin());
		
		Button backupAllButton = new Button("Backup All Articles");
		backupAllButton.setOnAction(e -> {
			TextInputDialog dialog = new TextInputDialog("all_articles_backup.csv");
			dialog.setTitle("Backup All Articles");
			dialog.setHeaderText("Enter the file name for the backup:");
			dialog.setContentText("File Name:");
			
			dialog.showAndWait().ifPresent(fileName -> {
				articleManager.backupAllArticles(fileName);
				showAlert("Backup Complete", "All articles have been backed up to: " + fileName);
			});
		});
		
		Button backupByGroupButton = new Button("Backup by Group");
		backupByGroupButton.setOnAction(e -> {
			TextInputDialog groupDialog = new TextInputDialog();
			groupDialog.setTitle("Backup by Group");
			groupDialog.setHeaderText("Enter the Group Name ofr Backup:");
			groupDialog.setContentText("Group Name:");
			
			groupDialog.showAndWait().ifPresent(groupName -> {
				TextInputDialog fileDialog = new TextInputDialog(groupName + "_backup.csv");
				fileDialog.setTitle("Backup by Group");
				fileDialog.setHeaderText("Enter the file name for the group backup:");
				fileDialog.setContentText("File Name:");
				
				fileDialog.showAndWait().ifPresent(fileName -> {
					articleManager.backupGroupArticles(groupName, fileName);
					showAlert("Backup Complete", "Group '" + groupName + "' backed up to: " + fileName);
				});
			});
		});
		
		Button restoreButton = new Button("Restore Articles");
		restoreButton.setOnAction(e -> {
			TextInputDialog dialog = new TextInputDialog();
			dialog.setTitle("Restore Articles");
			dialog.setHeaderText("Enter the name of the backup file to restore:");
			dialog.setContentText("File Name:");
			
			dialog.showAndWait().ifPresent(fileName -> {
				boolean replaceExisting = showCOnfirmationDialog(
						"Restore Mode",
						"Do you want to replace existing articles with the restored data?"
						);
				
				articleManager.restoreArticlesFromBackup(fileName, replaceExisting);
				showAlert("Restore Complete", "Articles restored from: " + fileName);
			});
		});
		VBox layout = new VBox(10, addArticleButton, listArticlesButton, deleteArticlesButton, viewArticleButton, backupAllButton, backupByGroupButton, restoreButton);
		layout.setAlignment(Pos.CENTER);
		Scene scene = new Scene(layout, 400, 400);
		articlesStage.setScene(scene);
		articlesStage.show();		
	}
	

	
	// 	opens the main articles management window with options to add, view, delete, update, backup, restore, and manage special access articles.
	private void openArticlesWindow() {
	    Stage articlesStage = new Stage();
	    articlesStage.setTitle("Articles");

	    // Button to add a new article
	    Button addArticleButton = new Button("Add Article");
	    addArticleButton.setOnAction(e -> openAddArticleForm());

	    // Button to list all articles or filter them
	    Button listArticlesButton = new Button("List Articles");
	    listArticlesButton.setOnAction(e -> openListArticlesPage());

	    // Button to delete articles
	    Button deleteArticlesButton = new Button("Delete Articles");
	    deleteArticlesButton.setOnAction(e -> openDeleteArticlesPage());

	    // Button to update articles
	    Button updateArticlesButton = new Button("Update Articles");
	    updateArticlesButton.setOnAction(e -> openUpdateArticlesPage());
	    
	    // button for special access groups
	    Button specialAccessGroupsButton = new Button("Special Access Groups");
	    specialAccessGroupsButton.setOnAction(e -> openSpecialAccessGroupsPage());
	    
	    // View a specific article by ID
	    Button viewArticleButton = new Button("View Article");
	    viewArticleButton.setOnAction(e -> openViewArticlePage());

	    // Backup all articles with user-input file name
	    Button backupAllButton = new Button("Backup All Articles");
	    backupAllButton.setOnAction(e -> {
	        TextInputDialog dialog = new TextInputDialog("all_articles_backup.csv");
	        dialog.setTitle("Backup All Articles");
	        dialog.setHeaderText("Enter the file name for the backup:");
	        dialog.setContentText("File Name:");

	        dialog.showAndWait().ifPresent(fileName -> {
	            articleManager.backupAllArticles(fileName);
	            showAlert("Backup Complete", "All articles have been backed up to: " + fileName);
	        });
	    });

	    // Backup articles by group with user-input file name
	    Button backupByGroupButton = new Button("Backup by Group");
	    backupByGroupButton.setOnAction(e -> {
	        TextInputDialog groupDialog = new TextInputDialog();
	        groupDialog.setTitle("Backup by Group");
	        groupDialog.setHeaderText("Enter the Group Name for Backup:");
	        groupDialog.setContentText("Group Name:");

	        groupDialog.showAndWait().ifPresent(groupName -> {
	            TextInputDialog fileDialog = new TextInputDialog(groupName + "_backup.csv");
	            fileDialog.setTitle("Backup by Group");
	            fileDialog.setHeaderText("Enter the file name for the group backup:");
	            fileDialog.setContentText("File Name:");

	            fileDialog.showAndWait().ifPresent(fileName -> {
	                articleManager.backupGroupArticles(groupName, fileName);
	                showAlert("Backup Complete", "Group '" + groupName + "' backed up to: " + fileName);
	            });
	        });
	    });

	    // Restore articles from a backup with user-input file name
	    Button restoreButton = new Button("Restore Articles");
	    restoreButton.setOnAction(e -> {
	        TextInputDialog dialog = new TextInputDialog();
	        dialog.setTitle("Restore Articles");
	        dialog.setHeaderText("Enter the name of the backup file to restore:");
	        dialog.setContentText("File Name:");

	        dialog.showAndWait().ifPresent(fileName -> {
	            boolean replaceExisting = showConfirmationDialog(
	                "Restore Mode", 
	                "Do you want to replace existing articles with the restored data?"
	            );

	            articleManager.restoreArticlesFromBackup(fileName, replaceExisting);
	            showAlert("Restore Complete", "Articles restored from: " + fileName);
	        });
	    });

	    // Layout with all buttons
	    VBox layout = new VBox(10, 
	        addArticleButton, listArticlesButton, deleteArticlesButton,
	        updateArticlesButton, specialAccessGroupsButton,
	        viewArticleButton, backupAllButton, backupByGroupButton, restoreButton
	    );
	    layout.setAlignment(Pos.CENTER);

	    Scene scene = new Scene(layout, 400, 500);
	    articlesStage.setScene(scene);
	    articlesStage.show();
	}


	// Helper method to show an alert
	private void showAlert(String title, String message) {
	    Alert alert = new Alert(Alert.AlertType.INFORMATION);
	    alert.setTitle(title);
	    alert.setContentText(message);
	    alert.showAndWait();
	}

	// Helper method to show a confirmation dialog
	private boolean showConfirmationDialog(String title, String message) {
	    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
	    alert.setTitle(title);
	    alert.showAndWait();
	    return alert.getResult() == ButtonType.YES;
	}
	
	// opens a window to update article details by loading an article using its ID and allowing a modifications to its attributes.
	private void openUpdateArticlesPage() {
	    Stage updateStage = new Stage();
	    updateStage.setTitle("Update Article");

	    // Input field to enter the Article ID
	    TextField articleIdField = new TextField();
	    articleIdField.setPromptText("Enter Article ID");

	    Button loadArticleButton = new Button("Load Article");

	    // Labels and input fields for the article data
	    TextField titleField = new TextField();
	    titleField.setPromptText("Enter Title");

	    ComboBox<String> levelComboBox = new ComboBox<>();
	    levelComboBox.getItems().addAll("Beginner", "Intermediate", "Advanced", "Expert");

	    TextArea descriptionArea = new TextArea();
	    descriptionArea.setPromptText("Enter Description");

	    TextArea contentArea = new TextArea();
	    contentArea.setPromptText("Enter Content");

	    TextArea privateNoteArea = new TextArea();
	    privateNoteArea.setPromptText("Enter Private Note");

	    TextField groupsField = new TextField();
	    groupsField.setPromptText("Enter Group Identifiers (comma-separated)");

	    TextField keywordsField = new TextField();
	    keywordsField.setPromptText("Enter Keywords (comma-separated)");

	    TextField linksField = new TextField();
	    linksField.setPromptText("Enter Links (comma-separated)");

	    Button updateButton = new Button("Update Article");

	    Label messageLabel = new Label();

	    loadArticleButton.setOnAction(event -> {
	        String articleIdText = articleIdField.getText().trim();
	        if (!articleIdText.isEmpty()) {
	            try {
	                long articleId = Long.parseLong(articleIdText);
	                Article article = articleManager.getArticleById(articleId);

	                if (article != null) {
	                    // Populate the fields with the article's data
	                    titleField.setText(article.getTitle());
	                    levelComboBox.setValue(article.getLevel());
	                    descriptionArea.setText(article.getDescription());
	                    contentArea.setText(article.getContent());
	                    privateNoteArea.setText(article.getPrivateNote());
	                    groupsField.setText(String.join(",", article.getGroupIdentifiers()));
	                    keywordsField.setText(String.join(",", article.getKeywords()));
	                    linksField.setText(String.join(",", article.getLinks()));
	                } else {
	                    messageLabel.setText("Article not found.");
	                }
	            } catch (NumberFormatException ex) {
	                messageLabel.setText("Invalid article ID.");
	            }
	        }
	    });

	    updateButton.setOnAction(e -> {
	        try {
	            long articleId = Long.parseLong(articleIdField.getText().trim());

	            // Gather the updated data from the fields
	            String title = titleField.getText().trim();
	            String level = levelComboBox.getValue();
	            String description = descriptionArea.getText().trim();
	            String content = contentArea.getText().trim();
	            String privateNote = privateNoteArea.getText().trim();
	            List<String> groups = parseCommaSeparatedInput(groupsField.getText().trim());
	            List<String> keywords = parseCommaSeparatedInput(keywordsField.getText().trim());
	            List<String> links = parseCommaSeparatedInput(linksField.getText().trim());

	            // Perform the update
	            boolean success = articleManager.updateArticleById(
	                    articleId, title, level, description, content, privateNote, groups, keywords, links);

	            if (success) {
	                messageLabel.setText("Article updated successfully.");
	            } else {
	                messageLabel.setText("Duplicate article detected. Update cancelled.");
	            }
	        } catch (NumberFormatException ex) {
	            messageLabel.setText("Invalid article ID.");
	        } catch (Exception ex) {
	            messageLabel.setText("An unexpected error occurred.");
	            ex.printStackTrace();
	        }
	    });


	    // Layout the UI elements
	    VBox layout = new VBox(10,
	            articleIdField, loadArticleButton, titleField, levelComboBox,
	            descriptionArea, contentArea, privateNoteArea, groupsField,
	            keywordsField, linksField, updateButton, messageLabel
	    );
	    layout.setAlignment(Pos.CENTER);
	    Scene scene = new Scene(layout, 400, 600);
	    updateStage.setScene(scene);
	    updateStage.show();
	}
	
	// to handle exceptions.
	public class DuplicateArticleException extends Exception {
	    public DuplicateArticleException(String message) {
	        super(message);
	    }
	}

	// opens a from for adding a new article with fields for title, level, description, and other attributes.
	private void openAddArticleForm() {
	    Stage addArticleStage = new Stage();
	    addArticleStage.setTitle("Add Article");

	    TextField titleField = new TextField();
	    titleField.setPromptText("Title");

	    ComboBox<String> levelComboBox = new ComboBox<>();
	    levelComboBox.getItems().addAll("Beginner", "Intermediate", "Advanced", "Expert");
	    levelComboBox.setPromptText("Select Level");

	    TextField groupField = new TextField();
	    groupField.setPromptText("Grouping Identifiers");

	    TextField descriptionField = new TextField();
	    descriptionField.setPromptText("Short Description");

	    TextField keywordsField = new TextField();
	    keywordsField.setPromptText("Keywords");

	    TextArea contentArea = new TextArea();
	    contentArea.setPromptText("Article Body");

	    TextArea privateNoteArea = new TextArea();
	    privateNoteArea.setPromptText("Private Note");

	    TextField linksField = new TextField();
	    linksField.setPromptText("Related Links");

	    Label messageLabel = new Label();

	    Button submitButton = new Button("Add Article");
	    submitButton.setOnAction(e -> {
	        String title = titleField.getText().trim();
	        String level = levelComboBox.getValue();
	        String groupText = groupField.getText().trim();
	        String description = descriptionField.getText().trim();
	        String keywordsText = keywordsField.getText().trim();
	        String content = contentArea.getText().trim();
	        String privateNote = privateNoteArea.getText().trim();
	        String linksText = linksField.getText().trim();

	        if (title.isEmpty() || level == null || description.isEmpty() || content.isEmpty()) {
	            messageLabel.setText("Please fill out all required fields (Title, Level, Description, Body).");
	            return;
	        }

	        List<String> groups = groupText.isEmpty() ? new ArrayList<>() : Arrays.asList(groupText.split(","));
	        List<String> keywords = keywordsText.isEmpty() ? new ArrayList<>() : Arrays.asList(keywordsText.split(","));
	        List<String> links = linksText.isEmpty() ? new ArrayList<>() : Arrays.asList(linksText.split(","));

	        boolean success = articleManager.addArticle(title, level, description, content, privateNote, groups, keywords, links);

	        if (success) {
	            messageLabel.setText("Article added successfully!");
	        } else {
	            messageLabel.setText("Duplicate article detected. Article not added.");
	        }
	    });

	    VBox layout = new VBox(10, titleField, levelComboBox, groupField, descriptionField, keywordsField, contentArea, 
	                           privateNoteArea, linksField, submitButton, messageLabel);
	    layout.setAlignment(Pos.CENTER);

	    Scene scene = new Scene(layout, 400, 600);
	    addArticleStage.setScene(scene);
	    addArticleStage.show();
	}

	// displays a window offering options to either list all articles or filter them based on criteria. Also redirects to a appropriate view.
	private void openListArticlesPage() {
	    Stage initialStage = new Stage();
	    initialStage.setTitle("Choose Article Listing Option");

	    Button listAllButton = new Button("List All Articles");
	    listAllButton.setOnAction(e -> {
	        initialStage.close();
	        openListAllArticlesView();
	    });

	    Button filterArticlesButton = new Button("Filter Articles");
	    filterArticlesButton.setOnAction(e -> {
	        initialStage.close();
	        openFilterArticlesView();
	    });

	    VBox layout = new VBox(10, listAllButton, filterArticlesButton);
	    layout.setAlignment(Pos.CENTER);
	    Scene scene = new Scene(layout, 300, 200);
	    initialStage.setScene(scene);
	    initialStage.show();
	}


	// Opens the list of all articles without filters
	private void openListAllArticlesView() {
	    Stage listAllStage = new Stage();
	    listAllStage.setTitle("All Articles");

	    TextArea articlesArea = new TextArea();
	    articlesArea.setEditable(false);

	    // List all articles and display them
	    List<Article> articles = articleManager.listAllArticles();
	    displayArticles(articles, articlesArea);

	    VBox layout = new VBox(10, articlesArea);
	    layout.setAlignment(Pos.CENTER);
	    Scene scene = new Scene(layout, 400, 400);
	    listAllStage.setScene(scene);
	    listAllStage.show();
	}


	// Opens the filter articles view
	private void openFilterArticlesView() {
	    Stage filterStage = new Stage();
	    filterStage.setTitle("Filter Articles");

	    TextField titleField = new TextField();
	    titleField.setPromptText("Filter by Title (optional)");

	    ComboBox<String> levelComboBox = new ComboBox<>();
	    levelComboBox.getItems().addAll("Filter by Level (optional)", "Beginner", "Intermediate", "Advanced", "Expert");
	    levelComboBox.setValue("Filter by Level (optional)");  // Default selection
	    levelComboBox.setPromptText("Filter by Level (optional)");

	    TextField groupField = new TextField();
	    groupField.setPromptText("Filter by Groups (e.g., Java,Windows or Java&Windows)");

	    TextField keywordField = new TextField();
	    keywordField.setPromptText("Filter by Keywords (e.g., Java,Python)");

	    TextArea articlesArea = new TextArea();
	    articlesArea.setEditable(false);

	    Button applyFilterButton = new Button("Apply Filters");
	    applyFilterButton.setOnAction(e -> {
	        articlesArea.clear();  // Clear previous results

	        String title = titleField.getText().trim();
	        String level = levelComboBox.getValue();
	        String groupInput = groupField.getText().trim();
	        String keywordInput = keywordField.getText().trim();

	        List<Article> articles = new ArrayList<>();

	        // Apply filters based on user input
	        if (!title.isEmpty()) {
	            articles.addAll(articleManager.searchArticlesByTitle(title));
	        }
	        if (level != null && !level.equals("All Levels")) {
	            articles.addAll(articleManager.searchArticlesByLevel(level));
	        }
	        if (!groupInput.isEmpty()) {
	            List<List<String>> groupFilters = parseFilterInput(groupInput);
	            articles.addAll(articleManager.listArticlesByGroups(groupFilters));
	        }
	        if (!keywordInput.isEmpty()) {
	            List<List<String>> keywordFilters = parseFilterInput(keywordInput);
	            articles.addAll(articleManager.listArticlesByKeywords(keywordFilters));
	        }

	        // Display results
	        if (articles.isEmpty()) {
	            articlesArea.setText("No articles found with the provided filters.");
	        } else {
	            displayArticles(articles, articlesArea);
	        }
	    });

	    VBox layout = new VBox(10, titleField, levelComboBox, groupField, keywordField, applyFilterButton, articlesArea);
	    layout.setAlignment(Pos.CENTER);
	    Scene scene = new Scene(layout, 400, 600);
	    filterStage.setScene(scene);
	    filterStage.show();
	}


	// to parse the input.
	private List<List<String>> parseFilterInput(String input) {
	    List<List<String>> filters = new ArrayList<>();

	    // Split the input by comma (OR logic)
	    String[] orGroups = input.split("\\s*,\\s*");
	    for (String group : orGroups) {
	        // Handle AND logic if "&" is present
	        if (group.contains("&")) {
	            List<String> andGroup = Arrays.asList(group.split("\\s*&\\s*"));
	            filters.add(andGroup);  // Add as a separate AND group
	        } else {
	            filters.add(Collections.singletonList(group));  // Add as a single item list for OR
	        }
	    }

	    return filters;
	}


	// Helper method to display articles in the TextArea
	private void displayArticles(List<Article> articles, TextArea articlesArea) {
	    StringBuilder content = new StringBuilder();
	    for (Article article : articles) {
	        content.append("ID: ").append(article.getId()).append("\n");
	        content.append("Title: ").append(article.getTitle()).append("\n");
	        content.append("Level: ").append(article.getLevel()).append("\n");
	        content.append("Groups: ").append(String.join(", ", article.getGroupIdentifiers())).append("\n");
	        content.append("Keywords: ").append(String.join(", ", article.getKeywords())).append("\n");
	        content.append("--------------------------------------------------\n\n");
	    }
	    articlesArea.setText(content.toString());
	}


	// displays a window with options to delete articles either by their ID or by group.
	private void openDeleteArticlesPage() {
	    Stage deleteStage = new Stage();
	    deleteStage.setTitle("Delete Articles");

	    Button deleteByIdButton = new Button("Delete Article");
	    deleteByIdButton.setOnAction(e -> openDeleteByIdPage());

	    Button deleteByGroupButton = new Button("Delete Articles from Group");
	    deleteByGroupButton.setOnAction(e -> openDeleteByGroupPage());

	    VBox layout = new VBox(10, deleteByIdButton, deleteByGroupButton);
	    layout.setAlignment(Pos.CENTER);
	    Scene scene = new Scene(layout, 300, 200);
	    deleteStage.setScene(scene);
	    deleteStage.show();
	}

	// Delete by ID
	private void openDeleteByIdPage() {
	    Stage deleteByIdStage = new Stage();
	    deleteByIdStage.setTitle("Delete Article by ID");

	    TextField idField = new TextField();
	    idField.setPromptText("Enter Article ID");

	    Label messageLabel = new Label();

	    Button deleteButton = new Button("Delete");
	    deleteButton.setOnAction(e -> {
	        long articleId = Long.parseLong(idField.getText().trim());
	        if (articleManager.deleteArticle(articleId)) {  // Reusing the existing method
	            messageLabel.setText("Article deleted successfully.");
	        } else {
	            messageLabel.setText("Failed to delete article. Article may not exist.");
	        }
	    });

	    VBox layout = new VBox(10, idField, deleteButton, messageLabel);
	    layout.setAlignment(Pos.CENTER);
	    Scene scene = new Scene(layout, 300, 200);
	    deleteByIdStage.setScene(scene);
	    deleteByIdStage.show();
	}

	// Delete by Group
	private void openDeleteByGroupPage() {
	    Stage deleteByGroupStage = new Stage();
	    deleteByGroupStage.setTitle("Delete Articles by Group");

	    TextField groupField = new TextField();
	    groupField.setPromptText("Enter Group Name");

	    Label messageLabel = new Label();

	    Button deleteButton = new Button("Delete");
	    deleteButton.setOnAction(e -> {
	        String group = groupField.getText().trim();
	        if (articleManager.deleteArticlesByGroup(group)) {
	            messageLabel.setText("Articles deleted successfully from group: " + group);
	        } else {
	            messageLabel.setText("Failed to delete articles. Group may not exist.");
	        }
	    });

	    VBox layout = new VBox(10, groupField, deleteButton, messageLabel);
	    layout.setAlignment(Pos.CENTER);
	    Scene scene = new Scene(layout, 300, 200);
	    deleteByGroupStage.setScene(scene);
	    deleteByGroupStage.show();
	}
	
	// Utility method to parse comma-separated input into a list of strings
	private List<String> parseCommaSeparatedInput(String input) {
	    if (input == null || input.trim().isEmpty()) {
	        return new ArrayList<>();
	    }
	    // Split the input by commas and trim any leading/trailing spaces from each item
	    return Arrays.asList(input.split("\\s*,\\s*"));
	}
	
	// opens a window to view details of a specific article by entering its ID.
	private void openViewArticlePage() {
	    Stage viewArticleStage = new Stage();
	    viewArticleStage.setTitle("View Article");

	    TextField idField = new TextField();
	    idField.setPromptText("Enter Article ID");

	    TextArea articleContentArea = new TextArea();
	    articleContentArea.setEditable(false);

	    Label messageLabel = new Label();

	    Button viewButton = new Button("View");
	    viewButton.setOnAction(e -> {
	        try {
	            long articleId = Long.parseLong(idField.getText().trim());
	            Article article = articleManager.getArticleById(articleId);

	            if (article == null) {
	                messageLabel.setText("Article not found.");
	                articleContentArea.clear();
	                return;
	            }

	            // Display the article's details
	            StringBuilder content = new StringBuilder();
	            content.append("ID: ").append(article.getId()).append("\n");
	            content.append("Title: ").append(article.getTitle()).append("\n");
	            content.append("Level: ").append(article.getLevel()).append("\n");
	            content.append("Description: ").append(article.getDescription()).append("\n");
	            content.append("Content: ").append(article.getContent()).append("\n");
	            content.append("Private Note: ").append(article.getPrivateNote()).append("\n");
	            content.append("Groups: ").append(String.join(", ", article.getGroupIdentifiers())).append("\n");
	            content.append("Keywords: ").append(String.join(", ", article.getKeywords())).append("\n");
	            content.append("Links: ").append(String.join(", ", article.getLinks())).append("\n");

	            articleContentArea.setText(content.toString());
	            messageLabel.setText("Article loaded successfully.");
	        } catch (NumberFormatException ex) {
	            messageLabel.setText("Invalid ID. Please enter a valid number.");
	        }
	    });

	    VBox layout = new VBox(10, idField, viewButton, articleContentArea, messageLabel);
	    layout.setAlignment(Pos.CENTER);
	    Scene scene = new Scene(layout, 400, 400);
	    viewArticleStage.setScene(scene);
	    viewArticleStage.show();
	}
	
	// opens a window for students to view article details by entering an article ID.
	private void openViewArticlePageForStudent() {
		Stage viewArticleStage = new Stage();
		viewArticleStage.setTitle("View Article");
		
		TextField idField = new TextField();
		idField.setPromptText("Enter Article ID");
		
		TextArea articleContentArea = new TextArea();
		articleContentArea.setEditable(false);
		
		Label messageLabel = new Label();
		
		Button viewButton = new Button("View");
		viewButton.setOnAction(e -> {
			try {
				long articleId = Long.parseLong(idField.getText().trim());
				Article article = articleManager.getArticleById(articleId);
				
				if (article == null) {
					messageLabel.setText("Article not Found.");
					articleContentArea.clear();
					return;
				}
				
				StringBuilder content = new StringBuilder();
				content.append("ID: ").append(article.getId()).append("\n");
				content.append("Title: ").append(article.getTitle()).append("\n");
				content.append("Level: ").append(article.getLevel()).append("\n");
				content.append("Description: ").append(article.getDescription()).append("\n");
				content.append("Content: ").append(article.getContent()).append("\n");
				content.append("Groups: ").append(String.join(",", article.getGroupIdentifiers())).append("\n");
				content.append("Keywords: ").append(String.join(",", article.getKeywords())).append("\n");
				content.append("Links: ").append(String.join(",", article.getLinks())).append("\n");
				
				articleContentArea.setText(content.toString());
				messageLabel.setText("Article loaded successfully.");
			} catch (NumberFormatException ex){
				messageLabel.setText("Invalid ID. Please enter a valid number.");
			}
		});
		
		VBox layout = new VBox(10, idField, viewButton, articleContentArea, messageLabel);
	    layout.setAlignment(Pos.CENTER);
	    Scene scene = new Scene(layout, 400, 400);
	    viewArticleStage.setScene(scene);
	    viewArticleStage.show();
	}
	
	//method to reset password when otp is verified
	private void openPasswordResetPage(User user) {
		
		Stage passwordResetStage =new Stage();
		passwordResetStage.setTitle("Reset Your Pasword");
		
		PasswordField newPasswordField = new PasswordField();
		newPasswordField.setPromptText("Enter New Password");
		
		PasswordField confirmPasswordField = new PasswordField();
		confirmPasswordField.setPromptText("Confirm New Password");
		
		Label resetMessage = new Label();
		
		Button resetPasswordButton = new Button ("Reset Password");
		resetPasswordButton.setOnAction(e -> {
			String newPassword = newPasswordField.getText();
			String confirmPassword= confirmPasswordField.getText();
			
			if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
				resetMessage.setText("Password fields cannot be empty.");
				return;
			}
			
			if (!newPassword.equals(confirmPassword)) {
				resetMessage.setText("Passwords do not match");
				return;
			}
			
			userManager.resetPasswordWithOtp(user, newPassword);
			passwordResetStage.close();
			loginMessage.setText("Password reset successfully ! Please logn in");
			
		});

		VBox resetLayout =new VBox(10, newPasswordField,confirmPasswordField, resetPasswordButton, resetMessage);
		resetLayout.setAlignment(Pos.CENTER);
		Scene resetScene = new Scene(resetLayout, 300, 200);
		passwordResetStage.setScene(resetScene);
		passwordResetStage.show();
	}
	
	//sign up page for new users without invite code
	private void openSignUpPage() {
		Stage signUpStage =new Stage();
		signUpStage.setTitle("Sign Up");
		
		TextField usernameField= new TextField();
		usernameField.setPromptText("Enter Username");
		
		PasswordField passwordField = new PasswordField();
		passwordField.setPromptText("Enter Password");
		
		PasswordField confirmPasswordField = new PasswordField();
		confirmPasswordField.setPromptText("Confirm Password");
		
		CheckBox studentCheckBox =new CheckBox("Student");
		CheckBox instructorCheckBox= new CheckBox("Instructor");
		
		ComboBox<String> expertiseLevelComboBox = new ComboBox<>();
		expertiseLevelComboBox.getItems().addAll("Beginner", "Intermediate","Advanced", "Expert");
		//default expertise is intermediate
		expertiseLevelComboBox.setValue("Intermediate");
		
		Label signUpMessage = new Label();
		
		Button signUpButton = new Button("Sign Up");
		signUpButton.setOnAction(e -> {
			String username = usernameField.getText().trim();
			String password = passwordField.getText();
			String confirmPassword = confirmPasswordField.getText();
			String expertiseLevel = expertiseLevelComboBox.getValue();
			
			if (username.isEmpty() || password.isEmpty()) {
				signUpMessage.setText("Username and Password cannot be empty");
				return;
			}
			
			if (!password.equals(confirmPassword)) {
				signUpMessage.setText("Passwords do not match");
				return;
			}
			
			char[] passwordHash = password.toCharArray();
			User newUser = new User(0,username, passwordHash, false,null, false, null ,null,null,null,null,expertiseLevel,null);
			
			if (studentCheckBox.isSelected()) {
				newUser.addRole("Student");
			}
			
			if(instructorCheckBox.isSelected()) {
				newUser.addRole("Instructor");
			}
			
			if (newUser.getRoles().isEmpty()) {
				signUpMessage.setText("Please select at least one role");
				return;
			}
			
			userManager.addUser(newUser);
			signUpStage.close();
			loginMessage.setText("Account created succesfully ! Please log in");
		});
		
		VBox signUpLayout =new VBox(10,usernameField,passwordField,confirmPasswordField,studentCheckBox,instructorCheckBox,expertiseLevelComboBox,signUpButton,signUpMessage);
		signUpLayout.setAlignment(Pos.CENTER);
		Scene signUpScene = new Scene(signUpLayout, 300,350);
		signUpStage.setScene(signUpScene);
		signUpStage.show();
	}
	
	//This method is for the admin only to reset the password by using their username 
	private void resetUserPassword() {
		Stage resetStage = new Stage();
		resetStage.setTitle("Reset User Password");
		
		TextField resetUsernameField = new TextField();
		resetUsernameField.setPromptText("Enter Username to reset");
		
		Label resetMessage = new Label();
		
		Button resetButton = new Button("Reset Password");
		resetButton.setOnAction(e -> {
			String targetUsername= resetUsernameField.getText().trim();
			
			if (targetUsername.isEmpty()) {
				resetMessage.setText("Username cannot be empty.");
				return;
			}
			
			User targetUser= userManager.getUser(targetUsername);
			if (targetUser != null) {
				userManager.generateOtpForPasswordReset(targetUser.getUsername());
				
				resetMessage.setText("Password reset succesfully.OTP has been sent");
				
			}
			else {
				resetMessage.setText("User not found");
			}
		});
		
		VBox resetLayout = new VBox(10, resetUsernameField, resetButton,resetMessage);
		resetLayout.setAlignment(Pos.CENTER);
		Scene resetScene=new Scene(resetLayout, 300,250);
		resetStage.setScene(resetScene);
		resetStage.show();
		
	}
	
	//when there is no user this will be used , here admin is pre selected 
	private void openFirstUserSignUpPage() {
		Stage signUpStage = new Stage();
		signUpStage.setTitle("Admin Setup");
		
		TextField usernameField = new TextField();
		usernameField.setPromptText("Enter Admin Username");
		
		PasswordField passwordField =new PasswordField();
		passwordField.setPromptText("Enter Admin Password");
		
		PasswordField confirmPasswordField =new PasswordField();
		confirmPasswordField.setPromptText("Confirm Password");
		
		CheckBox adminCheckBox =new CheckBox("Admin");
		adminCheckBox.setSelected(true);
		adminCheckBox.setDisable(true);
		
		CheckBox studentCheckBox = new CheckBox("Student");
		CheckBox instructorCheckBox= new CheckBox("Instructor");
		
		ComboBox<String> expertiseLevelComboBox = new ComboBox<>();
		expertiseLevelComboBox.getItems().addAll("Beginner","Intermediate","Advanced","Expert");
		expertiseLevelComboBox.setValue("Intermediate");
		
		Label signUpMessage = new Label();
		
		Button signUpButton = new Button("Create Admin");
		signUpButton.setOnAction(e-> {
			String username =usernameField.getText().trim();
			String password=passwordField.getText();
			String confirmPassword= confirmPasswordField.getText();
			String expertiseLevel= expertiseLevelComboBox.getValue();
			
			if(username.isEmpty() || password.isEmpty()) {
				signUpMessage.setText("Username and password cannot be empty");
				return;
			}
			
			if(!password.equals(confirmPassword)) {
				signUpMessage.setText("Passowrds do not match");
				return;
			}
			
			char[] passwordHash = password.toCharArray();
			
			Admin admin =new Admin(username,passwordHash);
			if(studentCheckBox.isSelected()) admin.addRole("Student");
			if(instructorCheckBox.isSelected()) admin.addRole("Instructor");
			
			admin.setExpertiseLevel(expertiseLevel);
			
			userManager.addUser(admin);
			User savedAdmin = userManager.getUser(username);
			addFirstAdmin(savedAdmin.getId());
			addToSpecialAccessGroup(savedAdmin);
			signUpStage.close();
			loginMessage.setText("Admin account created.Please log in");
			
			start(mainStage);
		});
		
        VBox signUpLayout = new VBox(10, usernameField, passwordField, confirmPasswordField, adminCheckBox, studentCheckBox, instructorCheckBox, expertiseLevelComboBox, signUpButton, signUpMessage);
		signUpLayout.setAlignment(Pos.CENTER);
		Scene signUpScene = new Scene(signUpLayout, 400,400);
		signUpStage.setScene(signUpScene);
		signUpStage.show();
	}
	
	// Adds a user as the first admit by inserting their user ID into the first_admin table.
	private void addFirstAdmin(long userId) {
		String sql = "INSERT INTO first_admin (user_id) VALUES (?)";
		
		try (Connection connection = DatabaseConnection.getConnection();
				PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setLong(1, userId);
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("Error adding first admin to first_admin talbe.");
		}
	}
	
	// adds a auser to special_access_group_users table with admin type and role.
	private void addToSpecialAccessGroup(User user) {
		String sql = """
				INSERT INTO special_access_group_users (user_id, user_type, role, username, email)
				VALUES (?, 'admin', 'admin', ?, ?)
				""";
		try (Connection connection = DatabaseConnection.getConnection();
				PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setLong(1, user.getId());
			stmt.setString(2, user.getUsername());
			stmt.setString(3, user.getEmail());
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("Error adding user to special access group.");
		}
	}
	
	// allows instructions to manage students by viewing a list and deleting selected students.
	private void openManageStudentsForInstructor() {
		Stage manageStudentsStage = new Stage();
		manageStudentsStage.setTitle("Manage Students");
		
		TextField usernameField = new TextField();
		usernameField.setPromptText("Enter Username to Delete");
		
		Label messageLabel = new Label();
		
		TableView<User> studentTable = new TableView<>();
		studentTable.setPlaceholder(new Label("No students found."));
		
		TableColumn<User, String> usernameColumn = new TableColumn<>("Username");
		usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
		
		TableColumn<User, String> nameColumn = new TableColumn<>("Display Name");
		nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDisplayName()));
		
		studentTable.getColumns().addAll(usernameColumn, nameColumn);
		
		Button loadStudentsButton = new Button("Load Students");
		loadStudentsButton.setOnAction(e -> {
			List<User> students = userManager.getAllStudents();
			
			if (students.isEmpty()) {
				messageLabel.setText("No students found in the system.");
			} else {
				studentTable.getItems().clear();
				studentTable.getItems().addAll(students);
			}
		});
		
		Button deleteStudentButton = new Button("Delete Selected Student");
		deleteStudentButton.setOnAction(e -> {
			User selectedStudent = studentTable.getSelectionModel().getSelectedItem();
			
			if (selectedStudent == null) {
				messageLabel.setText("Please select a student to delete.");
				return;
			}
			
			Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
					"Are you sure you want to delete student '" + selectedStudent.getUsername() + "'?",
					ButtonType.YES, ButtonType.NO);
			
			confirmation.showAndWait().ifPresent(response -> {
				if (response == ButtonType.YES) {
					try {
						userManager.removeUser(selectedStudent);
						studentTable.getItems().remove(selectedStudent);
						messageLabel.setText("Student '" + selectedStudent.getUsername() + "' deleted successfully.");
					} catch (Exception ex) {
						messageLabel.setText("Failed to delete student '" + selectedStudent.getUsername() + "'.");
						ex.printStackTrace();
					}
				}
			});
		});
		
		VBox layout = new VBox(10, loadStudentsButton, studentTable, deleteStudentButton, messageLabel);
		layout.setAlignment(Pos.CENTER);
		Scene scene = new Scene(layout, 300,250);
		manageStudentsStage.setScene(scene);
		manageStudentsStage.show();
	}
		
	//method for login page 
	private void login() {
		String username=usernameField.getText().trim();
		String password=passwordField.getText();
		
		if(username.isEmpty() || password.isEmpty()) {
			loginMessage.setText("Username and password cannot be empty");
			return;
		}
		
		//fetch the user by username
		User user =userManager.getUser(username);
		//check if user exist
		if(user != null) {
			if(user.isOneTimePassword()) {
				//if user is in OTP LOGIN MODE 
				if (user.getOtpExpiration() != null && user.getOtpExpiration().isAfter(LocalDateTime.now())) {
					//otp is valid prompt the user to input their OTP
					openOtpInputPage(user);
				}else {
					//OTP has expired, display an message
					loginMessage.setText("One time password has expired.Contact admin");
				}
			
			
			}else {
				//regular password flow 
				
				if (String.valueOf(user.getPasswordHash()).equals(password)) {
					 user=userManager.getUser(user.getUsername());
					 
					 if(!user.isSetupComplete()) {
						 openAccountSetupPage(user);
					 }
					 else if (user.getRoles().size() >1) {
						 openRoleSelectionPage(user);
					 }
					 else {
						 user.setCurrentRole(user.getRoles().iterator().next());
						 openHomePage(user);
					 }
				
				}else {
				 	loginMessage.setText("Login failed. Pleae check your credentials");
				}
			} 
		}else {
			loginMessage.setText("User not found. Please check your credentials");
		}
	}
	//opening the otp input page
	private void openOtpInputPage(User user) {
		Stage otpStage = new Stage();
		otpStage.setTitle("Enter OTP");
		
		TextField otpField = new TextField();
		otpField.setPromptText("Enter your OPT");
		
		Label otpMessage = new Label();
		
		Button submitOtpButton = new Button("Submit OTP");
		submitOtpButton.setOnAction(e -> {
			String enteredOtp = otpField.getText().trim();
			
			if (enteredOtp.isEmpty()) {
				otpMessage.setText("OTP cannot be empty.");
				return;
			}
			
			// compare entered OTP with stored OTP.
			if (enteredOtp.equals(user.getOtp())) {
				otpStage.close();
				openPasswordResetPage(user); // Redirect to password reset page.
			} else {
				otpMessage.setText("Invalid OTP. Please try again.");
			}
		});
		
		// Layout for OTP input window.
		VBox otpLayout = new VBox(10, otpField, submitOtpButton, otpMessage);
		otpLayout.setAlignment(Pos.CENTER);
		Scene otpScene = new Scene(otpLayout, 300, 200);
		otpStage.setScene(otpScene);
		otpStage.show();
	}
	
	/* This method opens the account setup page where the users can fill in their details and
	   complete their profile */
	private void openAccountSetupPage(User initialUser) {
		Stage setupStage = new Stage();
		setupStage.setTitle("Finish Setting Up Your Account");
		
		final User[] user = {initialUser};
		
		// fields for user input.
		TextField emailField = new TextField();
		emailField.setPromptText("Enter your email");
		
		TextField firstNameField = new TextField();
		firstNameField.setPromptText("First name");
		
		TextField middleNameField = new TextField();
		middleNameField.setPromptText("Middle name (optional)");
		
		TextField lastNameField = new TextField();
		lastNameField.setPromptText("Last name");
		
		TextField preferredNameField = new TextField();
		preferredNameField.setPromptText("Preferred first name (optional)");
		
		Label setupMessage = new Label();
		
		// save button to finalize the setup.
		Button saveButton = new Button("Save");
		saveButton.setOnAction(e -> {
			String email = emailField.getText().trim();
			String firstName = firstNameField.getText().trim();
			String middleName = middleNameField.getText().trim();
			String lastName = lastNameField.getText().trim();
			String preferredName = preferredNameField.getText().trim();
			
			if (email.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
				setupMessage.setText("Email, First Name, and Last Name are required.");
				return;
			}
			
			try {
				// Update the user details in the database.
				userManager.updateUserDetails(user[0], email, firstName, middleName, lastName, preferredName);
				user[0].setSetupComplete(true);
				
				user[0] = userManager.getUser(user[0].getUsername());
				
				setupStage.close();
				
				// Redirect to the home page with updated user info.
				openHomePage(user[0]);
			} catch (SQLException ex) {
				ex.printStackTrace();
				setupMessage.setText("Error updating account. Please try again,");
			}
		});
		
		VBox setupLayout = new VBox(10, emailField, firstNameField, middleNameField, lastNameField, preferredNameField, saveButton, setupMessage);
		setupLayout.setAlignment(Pos.CENTER);
		Scene setupScene = new Scene(setupLayout, 400, 400);
		setupStage.setScene(setupScene);
		setupStage.show();
	}
	
	// This method opens a window where users can select their roles.
	private void openRoleSelectionPage(User user) {
		Stage roleStage = new Stage();
		roleStage.setTitle("Select Role");
		
		Label roleLabel = new Label ("Select your role for this session:");
		
		// dropdown to list user roles.
		ComboBox<String> roleDropdown = new ComboBox<>();
		roleDropdown.getItems().addAll(user.getRoles());
		roleDropdown.setValue(user.getRoles().iterator().next());
		
		// button to confirm role section.
		Button selectRoleButton = new Button("Select Role");
		selectRoleButton.setOnAction(e -> {
			String selectedRole = roleDropdown.getValue();
			user.setCurrentRole(selectedRole);
			roleStage.close();
			openHomePage(user);
		});
		
		VBox roleLayout = new VBox(10, roleLabel, roleDropdown, selectRoleButton);
		roleLayout.setAlignment(Pos.CENTER);
		Scene roleScene = new Scene(roleLayout, 300, 200);
		roleStage.setScene(roleScene);
		roleStage.show();
	}
	
	// This method opens a page where admin can manage user roles.
	private void openManageRolesPage() {
		Stage roleStage = new Stage();
		roleStage.setTitle("Manage User Roles");
		
		ComboBox<String> userDropdown = new ComboBox<>();
		for (User u : userManager.listUsers()) {
			userDropdown.getItems().add(u.getUsername());
		}
		
		Label messageLabel = new Label();
		
		CheckBox adminCheckBox = new CheckBox("Admin");
		CheckBox studentCheckBox = new CheckBox("Student");
		CheckBox instructorCheckBox = new CheckBox("Instructor");
		
		Button updateRolesButton=new Button("Update Roles");
		updateRolesButton.setOnAction(e -> {
			String selectedUsername = userDropdown.getValue();
			if (selectedUsername == null || selectedUsername.isEmpty()) {
				messageLabel.setText("Please select a user.");
				return;
			}
			
			User selectedUser = userManager.getUser(selectedUsername);
			if (selectedUser == null) {
				messageLabel.setText("User not Found.");
				return;
			}
			
			Set<String> newRoles = new HashSet<String>();
			if (adminCheckBox.isSelected()) {
				newRoles.add("Admin");
			}
			
			if (studentCheckBox.isSelected()) {
				newRoles.add("Student");
			}
			
			if (instructorCheckBox.isSelected()) {
				newRoles.add("Instructor");
			}
			
			if (newRoles.isEmpty()) {
				messageLabel.setText("Please select at least one role.");
				return;
			}
			
			selectedUser.setRoles(newRoles);
			userManager.updateUserRoles(selectedUser);
			
			messageLabel.setText("Roles updated for " + selectedUser.getUsername());
		});
		
		VBox layout = new VBox(10, new Label("Selected user"), userDropdown, adminCheckBox, studentCheckBox,instructorCheckBox, updateRolesButton, messageLabel);
		layout.setAlignment(Pos.CENTER);
		Scene scene = new Scene(layout, 300, 300);
		roleStage.setScene(scene);
		roleStage.show();
	}
		
	// This method shows the generated OTP and has a button to close the window. 
	private void showOtpPopup(String otp) {
		
		Stage otpStage = new Stage();
		otpStage.setTitle("OTP Generated");
		
		Label otpLabel = new Label("Generated OTP: " + otp);
		
		Button closeButton = new Button("Close");
		closeButton.setOnAction(e -> otpStage.close());
		
		VBox otpLayout = new VBox(10, otpLabel, closeButton);
		otpLayout.setAlignment(Pos.CENTER);
		
		Scene otpScene = new Scene(otpLayout, 250, 150);
		otpStage.setScene(otpScene);
		otpStage.show();
	}
	
	// This method generates a 6-digit random OTP.
	private String generateOtp() {
		int otp = (int)(Math.random() * 900000) + 100000;
		return String.valueOf(otp);
	}
	
	// This method opens the window where admin can invite users and specify their roles.
	private void inviteUser() {
		Stage inviteStage = new Stage();
		inviteStage.setTitle("Invite User");
		
		TextField inviteUsernameField = new TextField();
		inviteUsernameField.setPromptText("Enter Username to Invite");
		
		PasswordField invitePasswordField = new PasswordField();
		invitePasswordField.setPromptText("Enter Temporary Password");
		
		CheckBox adminCheckBox = new CheckBox("Admin");
		CheckBox studentCheckBox = new CheckBox("Student");
		CheckBox instructorCheckBox = new CheckBox("Instructor");
		
		Label inviteMessage= new Label();
		
		Button inviteButton = new Button("Invite");
		inviteButton.setOnAction(e -> {
			String newUsername = inviteUsernameField.getText().trim();
			String tempPassword = invitePasswordField.getText();
			
			if (newUsername.isEmpty() || tempPassword.isEmpty()) {
				inviteMessage.setText("Username and Temperorary Password cannot be empty.");
				return;
			}
			
			String generatedOtp = generateOtp();
			char[] passwordHash = tempPassword.toCharArray();
			User newUser = new User(0, newUsername, passwordHash, true, LocalDateTime.now().plusHours(1), false, null, null, null, null, null, "Intermediate", new HashSet<>());
			
			if (adminCheckBox.isSelected()) newUser.addRole("Admin");
			if (studentCheckBox.isSelected()) newUser.addRole("Student");
			if (instructorCheckBox.isSelected()) newUser.addRole("Instructor");
			
			if (newUser.getRoles().isEmpty()) {
				inviteMessage.setText("Please select at least one role.");
				return;
			}
			
			newUser.setOtp(generatedOtp);
			userManager.addUser(newUser);
			
			showOtpPopup(generatedOtp);
			
			inviteStage.close();
			inviteMessage.setText("User invited successfully with OTP.");
		});
		
		VBox inviteLayout = new VBox(10, inviteUsernameField, invitePasswordField, adminCheckBox, studentCheckBox, instructorCheckBox, inviteButton, inviteMessage);
		inviteLayout.setAlignment(Pos.CENTER);
		Scene inviteScene = new Scene(inviteLayout, 300, 300);
		inviteStage.setScene(inviteScene);
		inviteStage.show();
	}
	
	//This deletes a user  but makes sure that the first admin is not dleeted by anyone 
	private void deleteUser() {
		Stage deleteStage = new Stage();
		deleteStage.setTitle("Delete User");
		
		TextField deleteUsernameField = new TextField();
		deleteUsernameField.setPromptText("Enter Username to Delete");
		
		Label deleteMessage = new Label();
		
		Button deleteButton = new Button("Delete User");
		deleteButton.setOnAction(e -> {
			String targetUsername = deleteUsernameField.getText().trim();
			
			if (targetUsername.isEmpty()) {
				deleteMessage.setText("Username cannot be empty.");
				return;
			}
			
			Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
					"Are you sure you want to delete user '" + targetUsername + "'?",
					ButtonType.YES, ButtonType.NO);
			confirmation.showAndWait().ifPresent(response -> {
				if (response == ButtonType.YES) {
					User targetUser = userManager.getUser(targetUsername);
					if (targetUser != null) {
						if (UserManager.isFirstAdmin(targetUser.getId())) {
							deleteMessage.setText("The first admin cannot be deleted.");
							return;
						}
						
						StringBuilder errorDetails = new StringBuilder();
						
						try {
							userManager.removeUserFromSpecialAccessGroups(targetUser.getId());
						} catch (Exception ex) {
							errorDetails.append("Failed to remove from special access groups. ");
						}
						
						try {
							userManager.removeUserFromFirstAdmin(targetUser.getId());
						} catch (Exception ex) {
							errorDetails.append("Failed to remove from admin. ");
						}
						
						try {
							userManager.removeUser(targetUser);
							if (errorDetails.length() == 0) {
								deleteMessage.setText("User deleted successfully.");
							} else {
								deleteMessage.setText("User deleted with issues: " + errorDetails.toString());
							}
						} catch (Exception ex) {
							deleteMessage.setText("Failed to delete user. " + errorDetails.toString());
						}
					} else {
						deleteMessage.setText("User not found.");
					}
				}
			});
		});
		VBox deleteLayout = new VBox(10, deleteUsernameField, deleteButton, deleteMessage);
		deleteLayout.setAlignment(Pos.CENTER);
		Scene deleteScene = new Scene(deleteLayout, 300, 250);
		deleteStage.setScene(deleteScene);
		deleteStage.show();		
	}	
	
	//This lists all the users
	private void listUsers() {
				Stage listStage = new Stage();
				listStage.setTitle("List of Users");
				
				TextArea usersTextArea = new TextArea();
				usersTextArea.setEditable(false);
				//Retrieves all the necessary details to display
				for (User user : userManager.listUsers()) {
					usersTextArea.appendText("Username: " + user.getUsername() + "\n");
					usersTextArea.appendText("Name: "+ user.getDisplayName()+ "\n");
					usersTextArea.appendText("Email: " + user.getEmail() + "\n");
					usersTextArea.appendText("Roles: " + String.join(", ", user.getRoles())+"\n");
					usersTextArea.appendText("-----------------------------\n");
				}
				//layout
				VBox listLayout = new VBox(10, usersTextArea);
				listLayout.setAlignment(Pos.CENTER);
				Scene listScene = new Scene(listLayout, 400, 400);
				listStage.setScene(listScene);
				listStage.show();
	}
	// function to display a list of special access users in a new window.
private void listSpecialAccessUsers() {
            Stage listStage = new Stage();
            listStage.setTitle("List of Special Access Users");

            TextArea usersTextArea = new TextArea();
            usersTextArea.setEditable(false);

            for (Map<String, Object> user : userManager.listpecialAccessUsers()){
                usersTextArea.appendText("ID: " + user.get("id")  + "\n");
                usersTextArea.appendText("Username: " + user.get("username")  + "\n");
                usersTextArea.appendText("Name: " + user.get("first_name")  + " " + user.get("last_name") + "\n");
                usersTextArea.appendText("Email: " + user.get("email")  + "\n");
                usersTextArea.appendText("Role: " + user.get("role")  + "\n");
                usersTextArea.appendText("User Type: " + user.get("user_type")  + "\n");
                usersTextArea.appendText("Expertise Level: " + user.get("expertise_level")  + "\n");
                usersTextArea.appendText("-----------------------------\n");


            }

            VBox listLayout = new VBox(10, usersTextarea);
            listlayout.setAllignment(Pos.CENTER);
            Scene listScene = new Scene(listLayout, 500, 500);
            listStage.setScene(listScene);
            listStage.show();

        }

        // Function to open a new page for managing special access roles for users.
        private void openManageSpecialAccessRolesPage(){
            Stage roleStage = new Stage();
            roleStage.setTitle("Manage Special Access Role");

            ComboBox<Map<String, Object>> userDropdown = new ComboBox<>();
            for (Map<String, Object> userMap : userManager.listSpecialAccessUsers()) {
                userDropdown.getItems().add(userMap);

            }

            userDropdown.setCellFactory(param -> new ListCell<>() {
                @Override
                protected void updateItem(Map<String, Object> item, boolean empty) {
                    super.updateItem(item. empty);
                    if (empty || item == null){
                        setText(null);
                    } else {
                        setText((String) item.get("username"));
                    }
                }
            });
            userDropdown.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(Map<String, Object> item, boolean empty) {
                    super.updateItem(item. empty);
                    if (empty || item == null){
                        setText(null);
                    } else {
                        setText((String) item.get("username"));
                    }
                }
            });

            Label messageLabel1 = new Label();

            CheckBox viewCheckBox = new CheckBox("View");
            CheckBox adminCheckBox = new checkBox("Admin");
            
            
            Button updateRolesButton = new Button("Update Role");
            updateRolesButton.setOnAction(e -> {
                Map<String, Object> selectedUserMap = userDropdown.getValue();
              if (selectedUserMap == null) {
                messageLabel.setText("Please select a user.");
                return;
            }
            
            Integer userId = (Integer) selectedUserMap.get("user_id");
            String username = (String) selectedUserMap.get("username");
            
            String newRole = null;
            if (viewCheckBox.isSelected() && adminCheckBox.isSelected()) {
                messageLabel.setText("Select only one role at a time.");
                return;
            } else if (viewCheckBox.isSelected())) {
                newRole = "view";
            } else if (adminCheckBox.isSelected()) {
                newRole = "admin";
            }

            if (newRole == null) {
                messagelabel.seText("Please select a role.");
                return;
            }

            boolean success = userManager.updateSpecialAccessRole(userId, newRole);
            if (success) {
                messageLabel.setText("Role update successfully for " + username);
            } else {
                messageLabel.setText("Failed to update role for " + username);
            }
        });

        VBox layout = new VBox(10,
            new Label("Select User"), userDropdown,
            updateRolesButton, messageLabel);.
        layout.setAlignment(Pos.CENTER);

        Scene scene = new Scene(layout, 300, 300);
        roleStage.setScene(scene);
        roleStage.show();
    }

    // Function to open a new page for rmeoving users from the special access group.
    private void openRemoveSpecialAccessUserPage() {
        Stage removeStage = new Stage();
        removeStage.setTitle("Remove User from Special Access Group");

        ComboBox<Map<String, Object>> userDropdown = new ComboBox<>();
        for (Map<String, Object> userMap : userManager.listSpecialAccessUsers()) {
            userDropdown.getItems().add(userMap);
        }

        userDropdown.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Map<String, Object> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText((String) item.get("username"));
                }
            }
        });
        userDropdown.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Map<String, Object> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText((String) item.get("username"));
                }
            }
        });

        Label messageLabel = new Label();

        Button removeUserButton = new Button("Remove User");
        removeUserButton.setOnAction(e -> {
            Map<String, Object> selectedUserMap = userDropdown.getValue();
            if (selectedUserMap == null) {
                messageLabel.setText("Please select a user to remove.");
                return;
            }

            Integer userId = (Integer) selectedUserMap.get("user_id");
            String username = (String) selectedUserMap.get("usrename");

            try {
                userManager.removeUserFromSpecialAccessGroups(userId);
                messageLabel.setText("User " + username + " removed successfully.");
                userDropdown.getItems().remove(selectedUserMap);
            } catch (Exception ex) {
                messageLabel.setText("Failed to remove user: " + ex.getMessage()); 
            }
        });

        VBox layout = new VBox(10, new Label("Select User"), userDropdown, removeUserButton, messageLabel);
        layout.setAlignment(Pos.CENTER);

        Scene scene = new Scene(layout, 300, 200);
        removeStage.setScene(scene);
        removeStage.show();
    }


			//launches the javafx application.
	public static void main(String[] args) {
				launch(args);
	}
		
}
