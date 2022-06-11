package uk.ac.rhul.cs2810.users;

import java.io.IOException;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import uk.ac.rhul.cs2810.containers.NotificationTableData;
import uk.ac.rhul.cs2810.containers.Order;
import uk.ac.rhul.cs2810.containers.OrderState;
import uk.ac.rhul.cs2810.containers.OrderTableData;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.database.DatabaseFactory;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;
import uk.ac.rhul.cs2810.database.LoginDB;
import uk.ac.rhul.cs2810.database.OrderDB;
import uk.ac.rhul.cs2810.database.TableDB;

/**
 * Displays the menu to the waiter from the menu class, as well as displaying additional information
 * required for the waiters.
 */
public class Waiter extends Menu {

  private int pollingRate;

  private Login login;
  private OrderDB orderDB;
  private LoginDB loginDB;
  private TableDB tableDB;


  ///////////////////////////////////////////////////////////////////////////

  @FXML
  private AnchorPane mainPane = null;
  @FXML
  private AnchorPane confPane = null;
  @FXML
  private Label statusMessage = null;
  @FXML
  private Label currentUser = null;
  @FXML
  private Button menuBut = null;
  @FXML
  private Button logoutBut = null;

  // Orders awaiting conf fxml
  private ObservableList<OrderTableData> ordersToBeConfData;
  @FXML
  private TableView ordersToBeConf = null;
  @FXML
  private TableColumn confOrderID = null;
  @FXML
  private TableColumn confTableNum = null;
  @FXML
  private TableColumn confOrderStat = null;
  @FXML
  private TableColumn confTimePlaced = null;

  private static int clickedOrderID;
  private static int clickedTableNumber;

  // Orders in prog
  private ObservableList<OrderTableData> ordersInProgData;
  @FXML
  private TableView ordersInProg = null;
  @FXML
  private TableColumn inProgOrderID = null;
  @FXML
  private TableColumn inProgTableNum = null;
  @FXML
  private TableColumn inProgOrderStat = null;
  @FXML
  private TableColumn inProgTimePlaced = null;

  // Notifications table
  private ObservableList<NotificationTableData> notificationData;
  @FXML
  private TableView notificationTable = null;
  @FXML
  private TableColumn notifOrderID = null;
  @FXML
  private TableColumn notifTableNum = null;
  @FXML
  private TableColumn notifRequest = null;

  // Not Paid Table
  private ObservableList<NotificationTableData> notPaidData;
  @FXML
  private TableView notPaidTable = null;
  @FXML
  private TableColumn notPaidID = null;
  @FXML
  private TableColumn notPaidTableNumber = null;
  @FXML
  private TableColumn notPaidStatus = null;

  private static int buttonDisable;
  private static boolean isWaiter;

  /////////////////////////////////////////////////////////////

  /**
   * Constructor allowing calling of the class.
   */
  public Waiter() {}

  /**
   * Instantiates a new Waiter and launches.
   *
   * @param first is this the first Waiter object made?
   */
  Waiter(boolean first, boolean testing) {
    if (first) {
      Application.launch(Waiter.class);
    }
  }

  /**
   * Notifies the waiter when an order is ready to collect from the kitchen.
   */
  public void notifyWaiterFoodReady() {
    try {
      List<Order> notifOrders =
          orderDB.getOrdersAssignedToWaiterFromState(OrderState.READY, login.getID());
      displayWaiterFoodReady(notifOrders);

      Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(pollingRate), e -> {
        notifyWaiterFoodReady();
      }));

      timeline.play();
    } catch (ConnectionError | ExecutionError dbe) {
      statusMessage.setVisible(true);
      statusMessage.setText("Error: " + dbe.getMessage());
    }

  }

  /**
   * Populate the notification table on the fxml window with notifications from the kitchen.
   * 
   * @param notifOrders The orders which are ready to be served.
   */
  private void displayWaiterFoodReady(List<Order> notifOrders) {
    notificationTable.getItems().clear();

    for (Order order : notifOrders) {
      NotificationTableData dataToAdd = new NotificationTableData(order.getID(),
          order.getTableNumber(), OrderState.READY.toString());

      notificationData.addAll(dataToAdd);
      notificationTable.getItems().add(dataToAdd);
    }
  }

  /**
   * Notifies the waiter when a customer has requested help.
   */
  public void notifyWaiterCustomerRequest() {
    try {
      List<Integer> tableRequest = tableDB.getNeededTables(login.getID());
      populateWaiterCustomerRequest(tableRequest);

      Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(pollingRate), e -> {
        notifyWaiterCustomerRequest();
      }));

      timeline.play();
    } catch (ConnectionError | ExecutionError dbe) {
      statusMessage.setVisible(true);
      statusMessage.setText("Error: " + dbe.getMessage());
    }
  }

  /**
   * Populate the notification table on the fxml window with customer requests.
   * 
   * @param tableRequest The requests to be added.
   */
  private void populateWaiterCustomerRequest(List<Integer> tableRequest) {
    for (Integer tableNumber : tableRequest) {
      NotificationTableData dataToAdd =
          new NotificationTableData(0, tableNumber, "Customer Needs Help");

      notificationData.addAll(dataToAdd);
      notificationTable.getItems().add(dataToAdd);
    }
  }

  /**
   * Displays the orders which are complete but have not been paid.
   */
  public void notifyWaiterUnpaidOrders() {
    try {
      List<Order> notPaid =
          orderDB.getOrdersAssignedToWaiterFromState(OrderState.SERVED, login.getID());
      displayUnpaidOrders(notPaid);

      Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(pollingRate), e -> {
        notifyWaiterUnpaidOrders();
      }));

      timeline.play();
    } catch (ConnectionError | ExecutionError dbe) {
      statusMessage.setVisible(true);
      statusMessage.setText("Error: " + dbe.getMessage());
    }


  }

  /**
   * Displays the orders which haven't been paid.
   */
  private void displayUnpaidOrders(List<Order> notPaid) {
    notPaidTable.getItems().clear();

    for (Order order : notPaid) {
      NotificationTableData dataToAdd = new NotificationTableData(order.getID(),
          order.getTableNumber(), OrderState.SERVED.toString());

      notPaidData.addAll(dataToAdd);
      notPaidTable.getItems().add(dataToAdd);
    }
  }

  /**
   * Allows the waiter to place an order by first requesting they enter a table number, then showing
   * the menu.
   */
  @FXML
  public void displayMenuForOrdering() {
    openWindow("TableSelector.fxml");
  }

  /*
   * Order table views
   */

  /**
   * When an item is clicked in the unconfirmed orders table display the options for that order.
   */
  @FXML
  public void onUnconfirmedOrderClick() {
    if (ordersToBeConf.getSelectionModel().getSelectedIndex() > -1 && isWaiter) {
      buttonDisable = 1;
      clickOrdersTables(this.ordersToBeConf, this.ordersToBeConfData);
    }
  }

  /**
   * When an item is clicked in the notifications table display the options for that order.
   */
  @FXML
  public void onNotificationClick() {
    int notifIndex = notificationTable.getSelectionModel().getSelectedIndex();
    if (notifIndex > -1 & login.getUserType() == 'w') {
      NotificationTableData selected = notificationData.get(notifIndex);

      clickedOrderID = selected.getID();
      clickedTableNumber = selected.getTableNumber();

      if (selected.getID() != 0) {
        buttonDisable = 2;
        openWindow("WaiterOrderOptions.fxml");
      } else {
        buttonDisable = 3;
        openWindow("WaiterOrderOptions.fxml");
      }
    }
  }

  /**
   * Gets the order id of the item clicked.
   * 
   * @return the orderID when clicked.
   */
  public int getClickedOrderID() {
    return clickedOrderID;
  }

  /**
   * Gets the table number of the item clicked.
   * 
   * @return the table number that was clicked.
   */
  public int getClickedTableNum() {
    return clickedTableNumber;
  }

  /**
   * When the waiter clicks on either type or orders table, display the relevant options to them.
   * 
   * @param table The table view on the window which is being updated.
   * @param data The data for the table which is being updated.
   */
  public void clickOrdersTables(TableView table, ObservableList<OrderTableData> data) {
    OrderTableData selected = data.get(table.getSelectionModel().getSelectedIndex());
    clickedOrderID = selected.getID();
    clickedTableNumber = selected.getTableNumber();

    openWindow("WaiterOrderOptions.fxml");
  }

  /**
   * Displays the information passed from the unconfirmed and confirmed order threads in the table
   * views.
   * 
   * @param table The table the data is to be added to.
   * @param ordersToBeAdded The list of orders containing the data for the table.
   * @param data The data which is to be added to the table.s
   */
  private void populateOrdersTable(TableView table, List<Order> ordersToBeAdded,
      ObservableList<OrderTableData> data) {
    table.getItems().clear();

    for (Order order : ordersToBeAdded) {
      OrderTableData dataToAdd = new OrderTableData(order.getID(), order.getTableNumber(),
          order.getState(), order.getTime().toString());

      data.addAll(dataToAdd);
      table.getItems().add(dataToAdd);
    }

  }

  /**
   * Run in a thread. Gets the orders to be confirmed and calls the method to display them.
   */
  public void displayOrdersToBeConfirmed() {
    try {
      List<Order> orders =
          orderDB.getOrdersAssignedToWaiterFromState(OrderState.UNCONFIRMED, login.getID());
      populateOrdersTable(this.ordersToBeConf, orders, this.ordersToBeConfData);

      Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(pollingRate), e -> {
        displayOrdersToBeConfirmed();
      }));

      timeline.play();
    } catch (ConnectionError | ExecutionError dbe) {
      statusMessage.setVisible(true);
      statusMessage.setText("Error: " + dbe.getMessage());
    }

  }

  /**
   * Displays the orders which are in progress, using a thread to update them from the database.
   */
  public void displayOrdersInProgress() {
    try {
      List<Order> orders =
          orderDB.getOrdersAssignedToWaiterFromState(OrderState.CONFIRMED, login.getID());
      orders
          .addAll(orderDB.getOrdersAssignedToWaiterFromState(OrderState.PREPARING, login.getID()));
      Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(pollingRate), e -> {
        populateOrdersTable(this.ordersInProg, orders, this.ordersInProgData);
        displayOrdersInProgress();
      }));

      timeline.play();
    } catch (ConnectionError | ExecutionError dbe) {
      statusMessage.setVisible(true);
      statusMessage.setText("Error: " + dbe.getMessage());
    }

  }


  /**
   * Gets the option used in the wauter options to disable the correct buttons. Set in the table
   * click methods.
   * 
   * @return The option for the buttons to be disabled in options.
   */
  public int getButtonDisable() {
    return buttonDisable;
  }

  /*
   * Exit
   */

  /**
   * Sets the waiter as logged out in the database and closes the program.
   */
  @FXML
  public void logout() {
    try {
      loginDB.logOut(login.getID());
    } catch (ConnectionError | ExecutionError dbe) {
      statusMessage.setVisible(true);
      statusMessage.setText("Error: " + dbe.getMessage());
    }
    Platform.exit();
    System.exit(0);
  }

  /*
   * Start
   */

  /**
   * Opens the specified fxml window.
   */
  private void openWindow(String window) {
    try {
      AnchorPane checkoutView = FXMLLoader.load(getClass().getClassLoader().getResource(window));
      mainPane.getChildren().setAll(checkoutView);
    } catch (IOException ioe) {
      statusMessage.setVisible(true);
      statusMessage.setText("Error: " + ioe.getMessage());
    }

  }

  /**
   * Hides the buttons when the window opened is management not a waiter.
   */
  public void hideButtonsForManagement() {
    if (!isWaiter) {
      menuBut.setVisible(false);
      logoutBut.setVisible(false);
    }
  }

  /**
   * Initialise.
   */
  @FXML
  void initialize() {
    // Initialise other classes
    this.login = new Login();
    statusMessage.setVisible(false);

    try {
      orderDB = DatabaseFactory.getOrderDB();
      loginDB = DatabaseFactory.getLoginDB();
      tableDB = DatabaseFactory.getTableDB();
    } catch (ConnectionError | ExecutionError dbe) {
      statusMessage.setVisible(true);
      statusMessage.setText("Error: " + dbe.getMessage());
    }

    this.pollingRate = orderDB.getPollingRate();
    isWaiter = login.getUserType() == 'w';

    // Displays the username in the window
    this.currentUser.setText("Current User: " + login.getUser());

    // Initialise table for orders to be confirmed
    this.ordersToBeConfData = FXCollections.observableArrayList();
    confOrderID.setCellValueFactory(new PropertyValueFactory<>("ID"));
    confTableNum.setCellValueFactory(new PropertyValueFactory<>("TableNumber"));
    confOrderStat.setCellValueFactory(new PropertyValueFactory<>("OrderState"));
    confTimePlaced.setCellValueFactory(new PropertyValueFactory<>("TimePlaced"));

    // Initialise table for orders in progress
    this.ordersInProgData = FXCollections.observableArrayList();
    inProgOrderID.setCellValueFactory(new PropertyValueFactory<>("ID"));
    inProgTableNum.setCellValueFactory(new PropertyValueFactory<>("TableNumber"));
    inProgOrderStat.setCellValueFactory(new PropertyValueFactory<>("OrderState"));
    inProgTimePlaced.setCellValueFactory(new PropertyValueFactory<>("TimePlaced"));

    // Initialise table for notifications
    this.notificationData = FXCollections.observableArrayList();
    notifOrderID.setCellValueFactory(new PropertyValueFactory<>("ID"));
    notifTableNum.setCellValueFactory(new PropertyValueFactory<>("TableNumber"));
    notifRequest.setCellValueFactory(new PropertyValueFactory<>("Request"));

    // Initialise table for orders not paid
    this.notPaidData = FXCollections.observableArrayList();
    notPaidID.setCellValueFactory(new PropertyValueFactory<>("ID"));
    notPaidTableNumber.setCellValueFactory(new PropertyValueFactory<>("TableNumber"));
    notPaidStatus.setCellValueFactory(new PropertyValueFactory<>("Request"));
    
    try {
      List<Order> unconfOrders =
          orderDB.getOrdersAssignedToWaiterFromState(OrderState.UNCONFIRMED, login.getID());
      List<Order> confOrders =
          orderDB.getOrdersAssignedToWaiterFromState(OrderState.CONFIRMED, login.getID());
      List<Integer> tableRequest = tableDB.getNeededTables(login.getID());
      List<Order> notifOrders =
          orderDB.getOrdersAssignedToWaiterFromState(OrderState.READY, login.getID());
      List<Order> unpaidOrders =
          orderDB.getOrdersAssignedToWaiterFromState(OrderState.SERVED, login.getID());
      
      // Initial population of orders tables
      if (unconfOrders != null) {
        populateOrdersTable(this.ordersToBeConf, unconfOrders, this.ordersToBeConfData);
      }
      if (confOrders != null) {
        populateOrdersTable(this.ordersInProg, confOrders, this.ordersInProgData);
      }
      if (tableRequest != null) {
        populateWaiterCustomerRequest(tableRequest);
      }
      if (notifOrders != null) {
        displayWaiterFoodReady(notifOrders);
      }
      if (unpaidOrders != null) {
        displayUnpaidOrders(unpaidOrders);
      }
    } catch (ConnectionError | ExecutionError dbe) {
      statusMessage.setVisible(true);
      statusMessage.setText("Error: " + dbe.getMessage());
    }

    // Starts a thread to display orders in to be confirmed
    Platform.runLater(new Thread(() -> {
      displayOrdersToBeConfirmed();
    }));

    // Starts a thread to display orders in progress
    Platform.runLater(new Thread(() -> {
      displayOrdersInProgress();
    }));

    // Starts a thread to get the orders awaiting delivery
    Platform.runLater(new Thread(() -> {
      notifyWaiterFoodReady();
    }));

    // Starts a thread to get the customer requests.
    Platform.runLater(new Thread(() -> {
      notifyWaiterCustomerRequest();
    }));

    // Starts a thread to get the unpaid orders.
    Platform.runLater(new Thread(() -> {
      notifyWaiterUnpaidOrders();
    }));

    hideButtonsForManagement();
  }

  /**
   * Method used to run Waiter.fxml file. Shows the UI to the user.
   */
  @Override
  public void start(Stage primaryStage) {
    // Removed unused start code as this is called by another class
  }
}
