package uk.ac.rhul.cs2810.users;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;
import uk.ac.rhul.cs2810.containers.Item;
import uk.ac.rhul.cs2810.containers.Order;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.database.DatabaseFactory;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;
import uk.ac.rhul.cs2810.database.MenuDB;
import uk.ac.rhul.cs2810.Exceptions.MissingDataError;
import uk.ac.rhul.cs2810.Exceptions.OrderAlreadyExistsException;
import uk.ac.rhul.cs2810.database.OrderDB;
import uk.ac.rhul.cs2810.Exceptions.OrderNotFoundException;

/**
 * Displays the menu interface with additional functions to the waiter.
 */
public class WaiterMenu extends Application {

  private Menu menu;
  private OrderDB orderDB;
  private MenuDB menuDB;
  private TableSelector tableSelector;

  @FXML
  private AnchorPane mainPane = null;
  @FXML
  private AnchorPane menuPane = null;
  @FXML
  private Button exit = null;
  @FXML
  private Button checkout = null;
  @FXML
  private Button changeTable = null;
  @FXML
  private TextField tableNumber = null;
  @FXML
  private Label error = null;

  /**
   * Returns the user back to the main screen.
   */
  @FXML
  public void exit() {
    try {
      AnchorPane newPane =
          FXMLLoader.load(getClass().getClassLoader().getResource("Waiter.fxml"));
      mainPane.getChildren().setAll(newPane);
    } catch (IOException ioe) {
      error.setText("Error: " + ioe.getMessage());
    }
  }

  /**
   * Completes the order and adds it to the database then exits.
   */
  @FXML
  public void checkout() {
    try {
      if (!tableSelector.getTableNum().equals("0")) {
        Order order = menu.getOrder();
        List<Item> menuItems = menuDB.getMenu();
        Map<Item, Integer> orderOcc = order.getOccuranceMap();

        boolean orderInStock = true;

        for (Item item : menuItems) {
          if (item.getStock() < orderOcc.getOrDefault(item, 0)) {
            error.setText("Sorry one or more items is out of stock");
            orderInStock = false;
            break;
          }
        }

        if (orderInStock) {
          try {
            orderDB.addOrder(order);
          } catch (OrderAlreadyExistsException oaee) {
            try {
              orderDB.modifyOrder(order);
            } catch (OrderNotFoundException onfe) {
              error.setText("Error: " + onfe.getMessage());
            }
          } catch (MissingDataError mde) {
            error.setText("Error: " + mde.getMessage());
          }

          menu.resetOrder();
          exit();
        } else {
          checkout.setStyle("-fx-background-color: #FF0000;");
          error.setStyle("-fx-background-color: #FF0000;");
          error.setText("Sorry some items in your order aren't available");

          Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(10), e -> {
            error.setStyle(null);
            checkout.setStyle(null);
          }));

          timeline.setCycleCount(1);
          timeline.play();
        }
      } else {
        error.setText("Error processing request");
      }
    } catch (ConnectionError | ExecutionError dbe) {
      error.setText("Error: " + dbe.getMessage());
    }
  }

  /**
   * Allows the waiter to switch to a different table to place an order.
   */
  @FXML
  public void changeTable() {
    try {
      AnchorPane waiterPane =
          FXMLLoader.load(getClass().getClassLoader().getResource("TableSelector.fxml"));
      mainPane.getChildren().setAll(waiterPane);
    } catch (IOException ioe) {
      error.setText("Error: " + ioe.getMessage());
    }
  }

  /**
   * Initialises variables.
   * Displays the menu window within the waiter window.
   */
  @FXML
  void initialize() {
    this.menu = new Menu();
    this.tableSelector = new TableSelector();
    try {
      orderDB = DatabaseFactory.getOrderDB();
      menuDB = DatabaseFactory.getMenuDB();
    } catch (ConnectionError | ExecutionError dbe) {
      error.setText("Error: " + dbe.getMessage());
    }
    
    try {
      Pane menu = FXMLLoader.load(getClass().getClassLoader().getResource("Menu.fxml"));
      menuPane.getChildren().setAll(menu);
    } catch (IOException ioe) {
      error.setText("Error: " + ioe.getMessage());
    }    
  }

  @Override
  public void start(Stage primaryStage) {
    // Blank code stub required for compiler
  }

}
