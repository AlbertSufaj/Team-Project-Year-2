package uk.ac.rhul.cs2810.users;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import uk.ac.rhul.cs2810.Exceptions.*;
import uk.ac.rhul.cs2810.containers.Item;
import uk.ac.rhul.cs2810.containers.Order;
import uk.ac.rhul.cs2810.database.DatabaseFactory;
import uk.ac.rhul.cs2810.database.MenuDB;
import uk.ac.rhul.cs2810.database.OrderDB;
import uk.ac.rhul.cs2810.database.TableDB;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Displays the menu to the customer and adds the extra customer functions.
 */
public class Customer extends Application {
  
  private TableSelector tableSelector;
  private Menu menu;
  private OrderDB orderDB;
  private TableDB tableDB;
  private MenuDB menuDB;
  private static Order order;
  private static boolean orderFromCust;
  private int pollingDelay = 10;
  
  @FXML
  private AnchorPane menuPane = null;
  @FXML
  private AnchorPane mainPane = null;
  
  @FXML
  private Button changeTable = null;
  @FXML
  private Button callWaiter = null;
  @FXML
  private Button checkout = null;
  @FXML
  private Label showTable = null;
  @FXML
  private Label error = null;
  
  /**
   * Blank constructor for customer to be used by other classes.
   */
  public Customer() {
  }
  
  /**
   * Allows the customer to call the waiter to their table.
   */
  @FXML
  public void callWaiter() {
    try {
      tableDB.setWaiterAlerted(Integer.parseInt(tableSelector.getTableNum()), true);
      callWaiter.setText("Called");
      callWaiter.setStyle("-fx-background-color: #FF0000;");
    } catch (NumberFormatException nfe) {
      // Never thrown
    } catch (ConnectionError | ExecutionError dbe) {
      dbe.printStackTrace();
      error.setText("Error: " + dbe.getMessage());
    }
  }
  
  /**
   * Resets the call waiter button when the waiter resets it.
   */
  public void dismissCall() {
    Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(this.pollingDelay), e -> {
      
      try {
        if (!tableDB.getWaiterAlerted(Integer.parseInt(tableSelector.getTableNum()))) {
          callWaiter.setText("Call Waiter");
          callWaiter.setStyle(null);
        }
        
        dismissCall();
      } catch (NumberFormatException nfe) {
        // Never thrown
      } catch (ConnectionError | ExecutionError dbe) {
        dbe.printStackTrace();
        error.setText("Error: " + dbe.getMessage());
      }
    }));
    
    timeline.play();
    
  }
  
  /**
   * When the user presses the change table button, it brings up the menu to change the table.
   */
  @FXML
  public void changeTable() {
    openWindow("TableSelector.fxml");
  }
  
  /**
   * Gets the order from the menu, pushes it to the database and opens the order tracker.
   */
  @FXML
  public void placeOrder() {
    order = menu.getOrder();
    List<Item> menuItems;
    try {
      menuItems = menuDB.getMenu();
      Map<Item, Integer> orderOcc = order.getOccuranceMap();
      orderFromCust = true;
      
      boolean orderInStock = true;
      
      for (Item item : menuItems) {
        if (item.getStock() < orderOcc.getOrDefault(item, 0)) {
          orderInStock = false;
          break;
        }
      }
      
      if (orderInStock) {
        try {
          try {
            orderDB.addOrder(order);
            openWindow("CustomerPaymentWindow.fxml");
          } catch (OrderAlreadyExistsException oaee) {
            orderDB.modifyOrder(order);
          }
        } catch (OrderNotFoundException | MissingDataError oe) {
          oe.printStackTrace();
          error.setText(oe.getMessage());
        }
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
      
      
    } catch (ConnectionError | ExecutionError dbe) {
      dbe.printStackTrace();
      error.setText("Error: " + dbe.getMessage());
    }
  }
  
  /**
   * Gets the order created.
   *
   * @return The order created in the menu.
   */
  public Order getOrder() {
    return order;
  }
  
  /**
   * Checks if the order has been set by the customer or waiter.
   *
   * @return If the order was placed by the customer.
   */
  public boolean isOrderFromCust() {
    return orderFromCust;
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
      ioe.printStackTrace();
      error.setText("Error: " + ioe.getMessage());
    }
  }
  
  /**
   * Initialises the class variables Opens the menu within the customer window.
   */
  @FXML
  void initialize() {
    // Initialise class variables
    this.tableSelector = new TableSelector();
    this.menu = new Menu();
    try {
      orderDB = DatabaseFactory.getOrderDB();
      tableDB = DatabaseFactory.getTableDB();
      menuDB = DatabaseFactory.getMenuDB();
    } catch (ConnectionError | ExecutionError dbe) {
      dbe.printStackTrace();
      error.setText("Error: " + dbe.getMessage());
    }
    
    // Loads the menu window into the customer window
    try {
      AnchorPane menu = FXMLLoader.load(getClass().getClassLoader().getResource("Menu.fxml"));
      menuPane.getChildren().setAll(menu);
      showTable.setText("Customer - Current Table: " + tableSelector.getTableNum());
    } catch (IOException ioe) {
      ioe.printStackTrace();
      error.setText("Error: " + ioe.getMessage());
    }
    
    // Opens a thread to check when waiter has disabled request
    Platform.runLater(new Thread(() -> {
      dismissCall();
    }));
  }
  
  @Override
  public void start(Stage primaryStage) throws IOException {
    // Unused as scene set in driver, block required for compiler.
  }
}
