package uk.ac.rhul.cs2810.users;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import uk.ac.rhul.cs2810.containers.Item;
import uk.ac.rhul.cs2810.containers.Order;
import uk.ac.rhul.cs2810.containers.OrdersToPayData;
import uk.ac.rhul.cs2810.containers.Price;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.database.DatabaseFactory;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;
import uk.ac.rhul.cs2810.database.OrderDB;
import uk.ac.rhul.cs2810.database.TableDB;

/*
 * An interface to enter the table number for which you want to place an order.
 */
public class TableSelector extends Application {

  private Login login;
  private TableDB tableDB;
  private OrderDB orderDB;

  @FXML
  private Button setTable = null;
  @FXML
  private AnchorPane mainPane = null;
  @FXML
  private TextField tableNumber = null;
  @FXML
  private Label errorLabel = null;
  @FXML
  private Button pay = null;
  @FXML
  private Button payAll = null;

  private ObservableList<OrdersToPayData> payData;
  @FXML
  private TableView ordersToBePaidTable = null;
  @FXML
  private TableColumn payOrderID = null;
  @FXML
  private TableColumn payTableNumber = null;
  @FXML
  private TableColumn payPrice = null;

  private static String tableNum;
  private static int maxTable;
  private boolean jumpToPay = false;
  private boolean multipleOrders = false;
  private static Order order;
  private static List<Order> ordersList;

  /**
   * Blank constructor.
   */
  public TableSelector() {

  }

  /**
   * Launches either the customer view or the waiter menu depending on the original launch.
   */
  @FXML
  public void confirmNumber() {
    try {
      setTableNum(tableNumber.getText());
      if (tableNum.matches("\\d{1,2}?")) {
        int tableNumber = Integer.parseInt(tableNum);
        if (tableNumber < maxTable + 1) {
          if (jumpToPay) {
            ordersList = orderDB.getOrdersFromTable(Integer.parseInt(tableNum));
            showPayInfo();
          } else {
            if (login.getUserType() == 'w') {
              tableDB.assignTable(tableNumber);
              openWindow("WaiterMenu.fxml");
            } else {
              tableDB.assignTable(tableNumber);
              openWindow("Customer.fxml");
            }
          }
        }
      } else {
        errorLabel.setText("Invalid Table Number");
      }
    } catch (ConnectionError | ExecutionError dbe) {
      dbe.printStackTrace();
      errorLabel.setText("Error: " + dbe.getMessage());
    }

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
      errorLabel.setText("Error: " + ioe.getMessage());
    }
  }

  /**
   * Sets the value of the table number stored as a string.
   * 
   * @param tabNum The table number entered.
   */
  public void setTableNum(String tabNum) {
    tableNum = tabNum;
  }

  /**
   * Returns the table number set previously.
   *
   * @return Returns the selected table number.
   */
  public String getTableNum() {
    return tableNum;
  }

  /**
   * Returns the flag for if multiple orders were paid for at once, so payment gets a list not
   * single order.
   *
   * @return Returns the flag for if multiple orders have been paid at once
   */
  public boolean isMultipleOrders() {
    return multipleOrders;
  }

  /**
   * Lets the customer jump to the payment menu if they have already made an order with the waiter.
   */
  @FXML
  public void goToPay() {
    jumpToPay = true;
    confirmNumber();
  }

  /**
   * Returns the order if it was placed by a waiter.
   *
   * @return The order if one was placed by a waiter
   */
  public Order getOrder() {
    return order;
  }

  /**
   * Returns a list of orders which all need to be paid.
   *
   * @return The list of orders which need to be paid.
   */
  public List<Order> getOrderList() {
    return ordersList;
  }

  /**
   * Gets the order selected from the table and sets it to be paid.
   */
  @FXML
  public void paySelectedOrder() {
    try {
      if (ordersToBePaidTable.getSelectionModel().getSelectedIndex() > -1) {
        int id = payData.get(ordersToBePaidTable.getSelectionModel().getSelectedIndex()).getID();
        order = orderDB.getOrderFromID(id);
        openWindow("CustomerPaymentWindow.fxml");
      }
    } catch (ConnectionError | ExecutionError dbe) {
      dbe.printStackTrace();
      errorLabel.setText("Error: " + dbe.getMessage());
    }
  }

  /**
   * Sets all orders to be paid and opens the payment window.
   */
  @FXML
  public void payAll() {
    this.multipleOrders = true;
    openWindow("CustomerPaymentWindow.fxml");
  }

  private void showPayInfo() {
    ordersToBePaidTable.setVisible(true);
    payAll.setVisible(true);

    for (Order order : ordersList) {
      Price price = null;
      for (Item item : order.getItemsInOrder()) {
        price = price.add(item.getPrice().getPriceValue());
      }
      OrdersToPayData dataToAdd = new OrdersToPayData(order.getID(), order.getTableNumber(), price);

      payData.addAll(dataToAdd);
      ordersToBePaidTable.getItems().add(dataToAdd);
    }
  }

  /**
   * Initialises class variables and sets prompt text.
   */
  @FXML
  void initialize() {
    this.login = new Login();

    try {
      this.tableDB = DatabaseFactory.getTableDB();
      this.orderDB = DatabaseFactory.getOrderDB();
      maxTable = tableDB.getMaxTableNum();
    } catch (ConnectionError | ExecutionError dbe) {
      dbe.printStackTrace();
      errorLabel.setText("Error: " + dbe.getMessage());
    }

    tableNumber.setPromptText("Enter a number 1 - " + maxTable);

    this.payData = FXCollections.observableArrayList();
    payOrderID.setCellValueFactory(new PropertyValueFactory<>("ID"));
    payTableNumber.setCellValueFactory(new PropertyValueFactory<>("TableNumber"));
    payPrice.setCellValueFactory(new PropertyValueFactory<>("Price"));

    ordersToBePaidTable.setVisible(false);
    payAll.setVisible(false);

    if (login.getUserType() == 'w') {
      pay.setVisible(false);
    }
  }

  @Override
  public void start(Stage primaryStage) {
    // Stage is set in login, not needed method.
  }

}
