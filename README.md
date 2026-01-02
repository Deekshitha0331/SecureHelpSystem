SecureHelpSystem
Overview

SecureHelpSystem is a Java-based help management system designed to securely create, manage, and control access to help articles for different user roles. The system supports role-based access control, secure authentication, and structured article handling, making it suitable for academic help systems or internal knowledge bases.

This project emphasizes security, modular design, object-oriented principles, and test-driven validation.

-Features:

---Secure Login System

User authentication

Password strength evaluation

---User & Role Management

Admin users

Standard users

Special-access users

Centralized user management

---Help Article Management

Create, view, and manage help articles

Special access articles for restricted users

Structured article storage and retrieval

---JUnit Test Coverage

JUnit test cases created for core components

Tests validate password rules, user logic, and article handling

Ensures correctness, reliability, and regression safety

---Security-Focused Design

Controlled access to sensitive content

Validation of user input

Separation of concerns across system components

---Modular Architecture

Clear class responsibilities

Easy to extend and maintain

---Technologies Used

Language: Java

Testing Framework: JUnit

UI Styling: CSS (application.css)

Database Layer: Java-based database connection logic

Version Control: Git & GitHub

---Project Structure
SecureHelpSystem/
│
├── Admin.java
├── Article.java
├── ArticleManager.java
├── DatabaseConnection.java
├── LoginSystem.java
├── Main.java
├── PasswordEvaluator.java
├── SpecialAccessArticle.java
├── User.java
├── UserManager.java
├── module-info.java
├── application.css
├── README.md
└── (JUnit Test Files)

---Testing

JUnit test cases were developed to validate:

Password strength evaluation logic

User creation and role handling

Article access and management functionality

All test cases execute successfully, confirming correct system behavior

Testing was performed using black-box techniques such as:

Equivalence partitioning

Boundary value analysis

---How to Run the Project

Clone the repository:

git clone https://github.com/Deekshitha0331/SecureHelpSystem.git


Open the project in a Java IDE (IntelliJ IDEA, Eclipse, or VS Code).

Ensure Java and JUnit are configured correctly.

Run the application:

Main.java


To run tests:

Execute JUnit test classes from the IDE

Or use your IDE’s test runner


---Design Highlights

Object-Oriented Design with clear abstraction

Role-based access control

Security-aware implementation

Comprehensive unit testing

Suitable for academic submission and portfolio demonstration

---Future Enhancements

Persistent database storage

GUI enhancements using JavaFX

Improved password encryption (hashing & salting)

Search and filtering for help articles

Expanded test coverage and integration testing
