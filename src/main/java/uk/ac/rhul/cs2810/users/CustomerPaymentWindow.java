package uk.ac.rhul.cs2810.users;

import java.io.IOException;
import java.util.Date;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;
import uk.ac.rhul.cs2810.Exceptions.OrderNotFoundException;
import uk.ac.rhul.cs2810.containers.Order;
import uk.ac.rhul.cs2810.database.DatabaseFactory;
import uk.ac.rhul.cs2810.database.OrderDB;

/**
 * Controller class for the payment window. Filters inputs to confirm they are valid before
 * proceeding to order tracker.
 */
public class CustomerPaymentWindow extends Application {
  private OrderDB orderDB;
  private static Customer cust;
  private TableSelector tabSel;

  @FXML
  private AnchorPane mainPane = null;
  @FXML
  private TextField nameField = null;
  @FXML
  private TextField numberField = null;
  @FXML
  private ComboBox monthCB = null;
  @FXML
  private ComboBox yearCB = null;
  @FXML
  private TextField cvvField = null;
  @FXML
  private Button payBut = null;
  @FXML
  private Label errorLab = null;

  private boolean error;
  private int yearSelected = 0;
  private int year;

  /**
   * Checks the values entered are valid, if they are, opens the order tracker.
   */
  @FXML
  public void pay() {
    error = false;
    checkName();
    checkNumber();
    checkYear();
    // Checks that the year has been entered before checking the month.
    if (yearSelected != 0) {
      checkMonth();
    }
    checkCvv();

    try {
      if (!error) {
        if (cust.isOrderFromCust()) {
          orderDB.setOrderPayed(cust.getOrder());
          try {
            AnchorPane checkoutView = FXMLLoader
                .load(getClass().getClassLoader().getResource("CustomerOrderTracker.fxml"));
            mainPane.getChildren().setAll(checkoutView);
          } catch (IOException ioe) {
            errorLab.setText("Error: " + ioe.getMessage());
          }

        } else {
          if (tabSel.isMultipleOrders()) {
            for (Order order : tabSel.getOrderList()) {
              orderDB.setOrderPayed(order);
            }
          } else {
            orderDB.setOrderPayed(tabSel.getOrder());
          }
        }
      }
    } catch (ConnectionError | ExecutionError dbe) {
      errorLab.setText("Error: " + dbe.getMessage());
    } catch (OrderNotFoundException onfe) {
      errorLab.setText("Error: Order not found to be paid");
    }

  }

  /**
   * Checks that the name box has been filled out.
   */
  private void checkName() {
    String name = nameField.getText();
    if (name.equals(null)) {
      this.error = true;
      nameField.setStyle("-fx-background-color: #FF0000;");
      errorLab.setText("Error: Name Missing");
    }
  }

  /**
   * Checks that a number of valid length is entered.
   */
  private void checkNumber() {
    String numStr = numberField.getText();
    if (!numStr.matches("\\d{15,16}?")) {
      this.error = true;
      numberField.setStyle("-fx-background-color: #FF0000;");
      errorLab.setText("Error: Invalid Card Number");
    }
  }

  /**
   * Checks that the year has been entered.
   * Updates the year selected if the year is valid.
   */
  private void checkYear() {
    if (yearCB.getSelectionModel().getSelectedItem().toString().equals("YY")) {
      this.error = true;
      yearCB.setStyle("-fx-background-color: #FF0000;");
      errorLab.setText("Error: Year Invalid");
    } else {
      yearSelected = Integer.parseInt(yearCB.getSelectionModel().getSelectedItem().toString());
    }
  }

  /**
   * Checks that the month has been entered.
   * Checks that the card is still in date by year and month.
   */
  private void checkMonth() {
    String itemSelected = monthCB.getSelectionModel().getSelectedItem().toString();
    if (itemSelected.equals("MM")) {
      this.error = true;
      monthCB.setStyle("-fx-background-color: #FF0000;");
      errorLab.setText("Error: Year Invalid");
    } else {
      int month = Integer.parseInt(itemSelected);
      if (month < ((new Date().getMonth())) && yearSelected == year) {
        this.error = true;
        monthCB.setStyle("-fx-background-color: #FF0000;");
        errorLab.setText("Error: Month Invalid");
      }
    }
  }

  /**
   * Checks that the CVV entered is a three digit number.
   */
  private void checkCvv() {
    String cvvStr = cvvField.getText();
    if (!cvvStr.matches("\\d{3}?")) {
      this.error = true;
      cvvField.setStyle("-fx-background-color: #FF0000;");
    }
  }

  /**
   * Initialises the class variables.
   * Populates the combo boxes for month and year with default values and current years.
   */
  @FXML
  void initialize() {
    try {
      this.orderDB = DatabaseFactory.getOrderDB();
    } catch (ConnectionError | ExecutionError dbe) {
      errorLab.setText("Error: " + dbe.getMessage());
    }
    cust = new Customer();
    year = (new Date()).getYear() + 1900;

    // Populates the combo box
    monthCB.getItems().add("MM");
    yearCB.getItems().add("YY");
    monthCB.getSelectionModel().select(0);
    yearCB.getSelectionModel().select(0);

    for (int i = 1; i < 12; i++) {
      monthCB.getItems().add(String.format("%02d", i));
      yearCB.getItems().add(String.valueOf(((year - 1) + i)));
    }
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    // Not used, called by driver
  }
}
