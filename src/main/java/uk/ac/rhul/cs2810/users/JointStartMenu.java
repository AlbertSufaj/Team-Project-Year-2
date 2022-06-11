package uk.ac.rhul.cs2810.users;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.util.List;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;
import uk.ac.rhul.cs2810.containers.Item;
import uk.ac.rhul.cs2810.database.DatabaseFactory;
import uk.ac.rhul.cs2810.database.ImageDB;
import uk.ac.rhul.cs2810.database.MenuDB;

/**
 * Shows the initial screen for both customers and staff.
 */
public class JointStartMenu extends Application {

  private Login login;

  @FXML
  private AnchorPane mainPane = null;
  @FXML
  private Button customerButton = null;
  @FXML
  private Button staffButton = null;
  @FXML
  private Label error = null;

  /**
   * Blank constructor.
   */
  public JointStartMenu() {

  }

  /**
   * A separate constructor to launch the program and prevent multiple instance conflict.
   * 
   * @param b A boolean given to start the program.
   */
  public JointStartMenu(boolean b) {
    Application.launch(JointStartMenu.class);
  }

  /**
   * Opens customer table selector on start button click.
   */
  @FXML
  public void customerClick() {
    login.setUserType('c');
    openWindow("TableSelector.fxml");
  }

  /**
   * Opens login screen for staff on staff click.
   */
  @FXML
  public void staffClick() {
    openWindow("LoginWithPin.fxml");
  }

  /**
   * Opens the specified fxml window.
   */
  private void openWindow(String window) {
    try {
      AnchorPane checkoutView = FXMLLoader.load(getClass().getClassLoader().getResource(window));
      mainPane.getChildren().setAll(checkoutView);
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  /**
   * Gets an instance of the menu database and stores the images.
   */
  private void cacheImages() {
    try {
      MenuDB menuDB = DatabaseFactory.getMenuDB();
      ImageDB imageDB = DatabaseFactory.getImageDB();
      List<Item> menu = menuDB.getMenu();
      
      for (Item item: menu) {
        imageDB.getImage(item.getID());
      }
    } catch (ConnectionError | ExecutionError dbe) {
      error.setVisible(true);
      error.setText(dbe.getMessage());
    }
  }

  /**
   * Initialises the login variable Starts a thread to cache the images to reduce load times.
   */
  @FXML
  void initialize() {
    this.login = new Login();
    error.setVisible(false);

    // Starts a thread to cache the images to reduce load times
    Platform.runLater(new Thread(() -> {
      cacheImages();
    }));
    
    try { // Sets up printing error to file
      PrintStream out = new PrintStream(
          new FileOutputStream("error.txt"));
      System.setOut(out);
      System.setErr(out);
    } catch (FileNotFoundException ignored) {
    }
  }

  @Override
  public void start(Stage primaryStage) {
    try {
      Pane newPane =
          FXMLLoader.load(getClass().getClassLoader().getResource("JointStartMenu.fxml"));
      Scene scene = new Scene(newPane);
      primaryStage.setResizable(false);
      primaryStage.setScene(scene);
      primaryStage.setTitle("Oaxaca");
      primaryStage.show();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }
}
