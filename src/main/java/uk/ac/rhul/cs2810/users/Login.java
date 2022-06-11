package uk.ac.rhul.cs2810.users;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;
import uk.ac.rhul.cs2810.database.DatabaseFactory;
import uk.ac.rhul.cs2810.database.LoginDB;

/**
 * Displays the different login menus for the different users and takes them to their respective
 * UIs.
 */
public class Login extends Application {
  private LoginDB loginDB;
  private static String pin;
  private static String userName;
  private static int id;
  private static char intOpt;

  @FXML
  private AnchorPane mainPane = null;
  @FXML
  private Button enterPin = null;
  @FXML
  private PasswordField pinField = null;
  @FXML
  private Label errorPinLogin = null;

  /**
   * A blank login constructor.
   */
  public Login() {}

  /**
   * Displays an error message on the UI.
   * 
   * @param message the content of the message
   */
  private void displayError(String message) {
    errorPinLogin.setVisible(true);
    errorPinLogin.setText("Error: " + message);
  }

  /**
   * Stores the pin in text to be passed to be encrypted.
   */
  public void setPin() {
    pin = pinField.getText();
  }

  /**
   * Gets the user name of the person that has logged into the system.
   * 
   * @return Returns the name of the person logged in
   */
  public String getUser() {
    return userName;
  }

  /**
   * Gets the user interface chosen.
   * 
   * @return The chosen interface
   */
  public char getUserType() {
    return intOpt;
  }

  /**
   * Sets the user type from the driver.
   * 
   * @param opt The option selected.
   */
  public void setUserType(char opt) {
    intOpt = opt;
  }

  /**
   * Gets the ID of the waiter or kitchen staff.
   * 
   * @return the ID of the person logged in, set after the pin has been hashed
   */
  public int getID() {
    return id;
  }

  /**
   * Opens the specified fxml window.
   */
  private void openWindow(String window) {
    try {
      AnchorPane customerPane =
          FXMLLoader.load(getClass().getClassLoader().getResource(window));
      mainPane.getChildren().setAll(customerPane);
    } catch (IOException ioe) {
      displayError("Error: " + ioe.getMessage());
    }
  }

  /**
   * Checks the waiter pin against the database, logs them in if it is correct.
   */
  @FXML
  public void loginWithPin() {
    setPin();
    try {
      if (pin.matches("\\d{4}?")) {
        int pinAsInt = Integer.parseInt(pin);
        String pinHash = LoginDB.hash(pinAsInt);
        id = loginDB.getID(pinHash);
        userName = loginDB.getName(id);

        // If the user exists in the database, get their username and launch the menu, otherwise set
        // an error message.
        if (id == -2) {
          intOpt = 'm';
          openWindow("Management.fxml");
        } else if (id == 0) {
          intOpt = 'k';
          openWindow("Kitchen.fxml");
        } else if (id > -1) {
          intOpt = 'w';
          openWindow("Waiter.fxml");
        } else {
          displayError("Invalid Login");
        }

      } else {
        displayError("Invalid PIN Format");
      }
    } catch (ConnectionError | ExecutionError dbe) {
      displayError("Error: " + dbe.getMessage());
    }
  }

  /**
   * Initialise database variables.
   * Sets style of components.
   */
  @FXML
  void initialize() {
    try {
      loginDB = DatabaseFactory.getLoginDB();
      String center = "-fx-alignment: center";
      errorPinLogin.setStyle("-fx-text-fill: red");
      pinField.setStyle(center);
      enterPin.setStyle(center);
    } catch (ConnectionError | ExecutionError dbe) {
      displayError("Error: " + dbe.getMessage());
    }
  }

  @Override
  public void start(Stage primaryStage) {
    // Called by driver
  }
}
