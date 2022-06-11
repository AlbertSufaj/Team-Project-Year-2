package uk.ac.rhul.cs2810.users;

import java.io.IOException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;
import uk.ac.rhul.cs2810.containers.Order;
import uk.ac.rhul.cs2810.containers.OrderState;
import uk.ac.rhul.cs2810.database.DatabaseFactory;
import uk.ac.rhul.cs2810.database.OrderDB;
import uk.ac.rhul.cs2810.database.TableDB;

/**
 * Displays tracking progress for the order placed by the customer.
 */
public class CustomerOrderTracker extends Application {

  private TableSelector tableSelector;
  private Menu menu;
  private OrderDB orderDB;
  private TableDB tableDB;
  private int pollingRate;

  private int orderNum;

  @FXML
  private Label tableNumberCheckout = null;
  @FXML
  private Label orderNumber = null;
  @FXML
  private Label orderStatus = null;
  @FXML
  private Label error = null;
  @FXML
  private Button callWaiter = null;
  @FXML
  private Button exit = null;

  /**
   * Blank constructor.
   */
  public CustomerOrderTracker() {

  }

  /**
   * Closes the application and ends the program when the exit button is pressed.
   */
  @FXML
  public void exit() {
    Platform.exit();
    System.exit(0);
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
      error.setText("Error: " + dbe.getMessage());
    }
  }

  /**
   * Method running in a thread to update the order status displayed.
   * Once the order is delivered, it is removed and an end screen is shown.
   */
  private void updateOrderStatus() {
    try {
      Order order = orderDB.getOrderFromID(orderNum);
      if (order != null) {
        OrderState status = order.getState();
        orderStatus.setText("Order Status: " + status.toString());
        
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(pollingRate), e -> {
          updateOrderStatus();
        }));

        timeline.play();
      } else {
        // Updates view once order is marked as delivered
        tableNumberCheckout.setText("Thank you for your order");
        orderNumber.setText("Enjoy your food");
        orderStatus.setText("Served");
        exit.setDisable(false);
      }
    } catch (ConnectionError | ExecutionError dbe) {
      error.setText("Error: " + dbe.getMessage());
    }
  }
  
  /**
   * Initialises database and starts a thread to update the order status.
   */
  @FXML
  void initialize() {
    // Initialises class variables
    this.tableSelector = new TableSelector();
    this.menu = new Menu();
    try {
      this.orderDB = DatabaseFactory.getOrderDB();
      this.tableDB = DatabaseFactory.getTableDB();
    } catch (ConnectionError | ExecutionError dbe) {
      error.setText("Error: " + dbe.getMessage());
    }
    
    this.pollingRate = orderDB.getPollingRate();

    // Displays brief order details
    tableNumberCheckout.setText("Current Table: " + tableSelector.getTableNum());
    orderNum = menu.getOrderNumber();
    orderNumber.setText("Order Number: " + orderNum);

    // Disables exit button while order is not complete
    exit.setDisable(true);

    // Starts a thread to poll the database to get updates on the order
    Platform.runLater(new Thread(() -> {
      updateOrderStatus();
    }));
  }

  @Override
  public void start(Stage primaryStage) throws IOException {
    // Start not used as called by another class
  }

}
