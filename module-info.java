module THE_DATABASE {
    requires javafx.controls;
    requires java.sql;

    // If you use any other JavaFX modules or external libraries, add them here
    // requires javafx.fxml;
    
    opens application to javafx.graphics, javafx.fxml;
}
