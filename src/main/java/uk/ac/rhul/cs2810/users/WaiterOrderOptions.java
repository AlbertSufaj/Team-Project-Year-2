package uk.ac.rhul.cs2810.users;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import uk.ac.rhul.cs2810.containers.Order;
import uk.ac.rhul.cs2810.containers.OrderState;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.database.DatabaseFactory;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;
import uk.ac.rhul.cs2810.database.OrderDB;
import uk.ac.rhul.cs2810.Exceptions.OrderNotFoundException;
import uk.ac.rhul.cs2810.database.TableDB;

/**
 * Displays the options the waiter has for changing the status of an order.
 */
public class WaiterOrderOptions extends Application {
  private Waiter waiter;
  private OrderDB orderDB;
  private TableDB tableDB;
  private TableSelector tb;

  @FXML
  private AnchorPane mainPane = null;
  @FXML
  private Label orderID = null;
  @FXML
  private Label tableNum = null;
  @FXML
  private Label error = null;
  @FXML
  private Button cancelOrder = null;
  @FXML
  private Button exit = null;
  @FXML
  private Button confirmed = null;
  @FXML
  private Button served = null;
  @FXML
  private Button resetRequest = null;
  @FXML
  private Button addToOrder = null;

  private static boolean isModifyOrder = false;

  /**
   * Blank constructor
   */
  public WaiterOrderOptions() {

  }

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
   * Removes the order from the database and closes the window.
   */
  @FXML
  public void setCancelled() {
    try {
      orderDB.removeOrder(orderDB.getOrderFromID(waiter.getClickedOrderID()));
      exit();
    } catch (ConnectionError | ExecutionError dbe) {
      error.setText("Error: " + dbe.getMessage());
    }
  }

  /**
   * Changes the status of the order to confirmed in the database and closes the window.
   */
  @FXML
  public void setConfirmed() {
    try {
      orderDB.confirmOrder(orderDB.getOrderFromID(waiter.getClickedOrderID()));
      exit();
    } catch (ConnectionError | ExecutionError | OrderNotFoundException dbe) {
      error.setText("Error: " + dbe.getMessage());
    }
  }

  /**
   * Checks if the order is paid, if it is removes it, otherwise changes the status to served.
   */
  @FXML
  public void setServed() {
    try {
      Order order = orderDB.getOrderFromID(waiter.getClickedOrderID());
      if (orderDB.getPayed(order)) {
        orderDB.removeOrder(order);
      } else {
        orderDB.setOrderState(order, OrderState.SERVED);
      }
      exit();
    } catch (ConnectionError | ExecutionError | OrderNotFoundException dbe) {
      error.setText("Error: " + dbe.getMessage());
    }

  }

  /**
   * Resets the customer request after they have attended to the customer, then exits.
   */
  @FXML
  public void resetRequest() {
    try {
      tableDB.setWaiterAlerted(waiter.getClickedTableNum(), false);
      exit();
    } catch (ConnectionError | ExecutionError dbe) {
      error.setText("Error: " + dbe.getMessage());
    }
    
  }

  /**
   * Opens the menu with the order placed by the customer loaded in.
   */
  @FXML
  public void addToOrder() {
    isModifyOrder = true;
    tb.setTableNum(String.valueOf(waiter.getClickedTableNum()));
    try {
      AnchorPane menu = FXMLLoader.load(getClass().getClassLoader().getResource("WaiterMenu.fxml"));
      mainPane.getChildren().setAll(menu);
    } catch (IOException ioe) {
      error.setText("Error: " + ioe.getMessage());
    }
  }

  /**
   * Initialises class variables and confirms which buttons to disable on launch.
   */
  @FXML
  void initialize() {
    this.waiter = new Waiter();

    try {
      this.orderDB = DatabaseFactory.getOrderDB();
      this.tableDB = DatabaseFactory.getTableDB();
    } catch (ConnectionError | ExecutionError dbe) {
      error.setText("Error: " + dbe.getMessage());
    }


    orderID.setText("Order ID: " + waiter.getClickedOrderID());
    tableNum.setText("Table Number: " + waiter.getClickedTableNum());
    this.tb = new TableSelector();

    // Checks which piece of code has opened this interface and disables the correct
    // buttons
    switch (waiter.getButtonDisable()) {
      case 1:
        // Disables all but confirm and cancel
        served.setDisable(true);
        resetRequest.setDisable(true);
        break;
      case 2:
        // Disables all but complete
        confirmed.setDisable(true);
        cancelOrder.setDisable(true);
        resetRequest.setDisable(true);
        addToOrder.setDisable(true);
        break;
      case 3:
        // Disables all but reset request
        confirmed.setDisable(true);
        cancelOrder.setDisable(true);
        served.setDisable(true);
        addToOrder.setDisable(true);
        break;
      default:
        break;
    }
  }

  @Override
  public void start(Stage primaryStage) {
    // Stub required by compiler
  }

}
