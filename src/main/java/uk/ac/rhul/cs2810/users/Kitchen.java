package uk.ac.rhul.cs2810.users;

import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;
import uk.ac.rhul.cs2810.Exceptions.OrderNotFoundException;
import uk.ac.rhul.cs2810.containers.KitchenOrderTableData;
import uk.ac.rhul.cs2810.containers.Order;
import uk.ac.rhul.cs2810.containers.OrderState;
import uk.ac.rhul.cs2810.database.DatabaseFactory;
import uk.ac.rhul.cs2810.database.MenuDB;
import uk.ac.rhul.cs2810.database.OrderDB;

/**
 * Represents the kitchen staff using the system.
 */
public class Kitchen extends Application {

  private Login login;
  private OrderDB odb;
  private MenuDB mdb;
  private int pollingRate;
  private ObservableList<KitchenOrderTableData> ordersConfirmedData;
  private ObservableList<KitchenOrderTableData> ordersPrepairingData;
  private List<Order> ordersConfirmed;
  private List<Order> ordersPreparing;
  private KitchenOrderTableData selectedOrder;

  @FXML
  private ListView<String> completeList = null;
  @FXML
  private Button dismissButton = null;
  @FXML
  private Button markAsCompleteButton = null;
  @FXML
  private Button changeStatusButton = null;
  @FXML
  private Button confirmChangeButton = null;
  @FXML
  private Text statusMessage = null;
  @FXML
  private ComboBox<String> statusOptions = null;
  @FXML
  private TableView<KitchenOrderTableData> orderTable = null;
  @FXML
  private TableColumn<KitchenOrderTableData, String> orderIdView = null;
  @FXML
  private TableColumn<KitchenOrderTableData, String> statusView = null;
  @FXML
  private TableColumn<KitchenOrderTableData, String> timeView = null;
  @FXML
  private TableColumn<KitchenOrderTableData, String> itemView = null;

  /**
   * Constructs an instance of the Kitchen User class.
   */
  public Kitchen() {}

  /**
   * Show a status message on the UI.
   *
   * @param error whether it is an error or not
   * @param message the content of the message
   */
  private void pushStatusMessage(boolean error, String message) {
    Color fill;
    if (error) {
      fill = Color.RED;
    } else {
      fill = Color.WHITE;
    }
    statusMessage.setFill(fill);
    statusMessage.setText(message);
    dismissButton.setVisible(true);
    statusMessage.setVisible(true);
  }

  /**
   * Choose status of order.
   */
  public void changeStatus() {
    if (statusOptions.isVisible()) {
      statusOptions.setVisible(false);
      confirmChangeButton.setVisible(false);
    } else {
      statusOptions.setVisible(true);
      confirmChangeButton.setVisible(true);
      statusOptions.getSelectionModel().select("Preparing");
    }
  }

  /**
   * Stored the item clicked to preserve when table is updated.
   */
  public void onOrderClicked() {
    this.selectedOrder = orderTable.getSelectionModel().getSelectedItem();
  }

  /**
   * Change status of order.
   */
  public void confirmChangeStatus() {
    try {
      if (selectedOrder != null) {
        OrderState state = OrderState.CONFIRMED;
        String selectedItem = statusOptions.getSelectionModel().getSelectedItem();

        if (selectedItem == null) {
          pushStatusMessage(true, "No status selected");
        } else if (selectedItem.equals("Preparing")) {
          state = OrderState.PREPARING;
        } else if (selectedItem.equals("Confirmed")) {
          state = OrderState.CONFIRMED;
        }

        odb.setOrderState(odb.getOrderFromID(selectedOrder.getId()), state);
      } else {
        pushStatusMessage(true, "No orders are selected");
      }
    } catch (ConnectionError | ExecutionError dbe) {
      statusMessage.setText("Error: " + dbe.getMessage());
    } catch (OrderNotFoundException onfe) {
      statusMessage.setText("Error: Order not found to be paid");
    }
  }

  /**
   * Set order state to ready.
   */
  @FXML
  public void markFinished() {
    try {
      if (selectedOrder != null) {
        Order order = odb.getOrderFromID(selectedOrder.getId());
        int time = odb.setOrderState(order, OrderState.READY);
        completeList.getItems().add(order.toString());
        orderTable.getItems().removeAll(orderTable.getSelectionModel().getSelectedItem());
        mdb.addOrderToData(order, time);
      } else {
        pushStatusMessage(true, "Cannot mark as finished as no orders are selected");
      }
    } catch (ConnectionError | ExecutionError dbe) {
      statusMessage.setText("Error: " + dbe.getMessage());
    } catch (OrderNotFoundException onfe) {
      statusMessage.setText("Error: Order not found to be paid");
    }

  }

  /**
   * Display orders on the UI.
   * 
   * @param data the list to add order items to
   * @param orders the order items to add
   */
  public void displayOrders(ObservableList<KitchenOrderTableData> data, List<Order> orders) {
    for (Order order : orders) {
      KitchenOrderTableData orderToAdd = new KitchenOrderTableData(order.getID(),
          order.formatTime(), order.getState(), order.getItemsFormatted());
      orderTable.getItems().add(orderToAdd);
      data.add(orderToAdd);
    }
    orderTable.getSelectionModel().select(selectedOrder);
  }

  /**
   * Gets a list of confirmed orders from the database.
   */
  private void updateOrdersFromDB() {
    orderTable.getItems().clear();
    try {
      ordersConfirmed = odb.getOrdersAssignedToWaiterFromState(OrderState.CONFIRMED, login.getID());
      ordersPreparing = odb.getOrdersAssignedToWaiterFromState(OrderState.PREPARING, login.getID());
    } catch (ConnectionError | ExecutionError dbe) {
      statusMessage.setText("Error: " + dbe.getMessage());
    }

    // Populates the orders table with the new data
    if (ordersConfirmed != null) {
      displayOrders(ordersConfirmedData, ordersConfirmed);
    }

    if (ordersPreparing != null) {
      displayOrders(ordersPrepairingData, ordersPreparing);
    }

    Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(pollingRate), e -> {
      updateOrdersFromDB();
    }));

    timeline.play();
  }

  /**
   * Hides the message when the staff have seen it.
   */
  @FXML
  public void dismissStatusMessage() {
    statusMessage.setVisible(false);
    dismissButton.setVisible(false);
  }

  /**
   * Initialises variables and table. Starts a thread for updating the orders from the database.
   */
  @FXML
  void initialize() {
    // Initialises the database
    try {
      this.odb = DatabaseFactory.getOrderDB();
      this.mdb = DatabaseFactory.getMenuDB();
    } catch (ConnectionError | ExecutionError dbe) {
      statusMessage.setText("Error: " + dbe.getMessage());
    }
    
    this.pollingRate = odb.getPollingRate();
    this.login = new Login();

    // Populates the status options combo box
    statusOptions.getItems().addAll(OrderState.CONFIRMED.toString(),
        OrderState.PREPARING.toString());

    // Declares table cell values using the viewable order container
    ordersConfirmedData = FXCollections.observableArrayList();
    ordersPrepairingData = FXCollections.observableArrayList();
    orderIdView.setCellValueFactory(new PropertyValueFactory<KitchenOrderTableData, String>("id"));
    statusView
        .setCellValueFactory(new PropertyValueFactory<KitchenOrderTableData, String>("status"));
    timeView.setCellValueFactory(new PropertyValueFactory<KitchenOrderTableData, String>("time"));
    itemView.setCellValueFactory(new PropertyValueFactory<KitchenOrderTableData, String>("items"));

    // Runs a thread to update the orders from the database and display them.
    Platform.runLater(new Thread(() -> {
      updateOrdersFromDB();
    }));
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    // Called by driver
  }
}
