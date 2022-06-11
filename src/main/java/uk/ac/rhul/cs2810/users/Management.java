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
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;
import uk.ac.rhul.cs2810.containers.Employee;
import uk.ac.rhul.cs2810.containers.EmployeeTableData;
import uk.ac.rhul.cs2810.containers.Item;
import uk.ac.rhul.cs2810.containers.StockTableData;
import uk.ac.rhul.cs2810.database.DatabaseFactory;
import uk.ac.rhul.cs2810.database.LoginDB;
import uk.ac.rhul.cs2810.database.MenuDB;
import uk.ac.rhul.cs2810.database.WaiterDB;

/**
 * The controller class for the management interface.
 */
public class Management extends Application {
  private Waiter waiter;
  private Login login;
  private MenuDB menuDB;
  private WaiterDB waiterDB;
  private LoginDB loginDB;
  private int pollingRate;

  @FXML
  private AnchorPane mainPane = null;
  @FXML
  private AnchorPane waiterInfoPane = null;
  @FXML
  private Button addItem = null;
  @FXML
  private Button removeItem = null;
  @FXML
  private Button modifyItem = null;
  @FXML
  private Label error = null;

  // FXML Variables for Stock
  private ObservableList<StockTableData> stockLevelData;
  @FXML
  private TableView stockTable = null;
  @FXML
  private TableColumn stockID = null;
  @FXML
  private TableColumn stockName = null;
  @FXML
  private TableColumn stockPrice = null;
  @FXML
  private TableColumn stockLevel = null;
  @FXML
  private TableColumn stockAvgTime = null;

  // FXML Variables for Employee Data
  private ObservableList<EmployeeTableData> employeeData;
  @FXML
  private TableView employeeTable = null;
  @FXML
  private TableColumn employeeID = null;
  @FXML
  private TableColumn employeeName = null;
  @FXML
  private TableColumn employeeDoB = null;
  @FXML
  private TableColumn employeeDoH = null;
  @FXML
  private TableColumn employeeNumOfOrder = null;
  @FXML
  private TableColumn employeeHoursWorked = null;

  private static char optChosen;


  /**
   * Blank Constructor.
   */
  public Management() {

  }

  /**
   * Opens a window which allows the manager to add a new item to the menu.
   */
  @FXML
  public void addItem() {
    optChosen = 'a';
    openWindow("ManagementModifyItem.fxml");
  }

  /**
   * Opens the menu in removal mode, when an item is selected, takes the item to be modified. From
   * this screen, the item can then be deleted.
   */
  @FXML
  public void removeItem() {
    optChosen = 'r';
    openWindow("Menu.fxml");
  }

  /**
   * Opens the menu in removal mode, when an item is selected, takes the item to be modified.
   */
  @FXML
  public void modifyItem() {
    optChosen = 'm';
    openWindow("Menu.fxml");
  }

  /**
   * Returns the option chosen, set by the button presses modifying items.
   * 
   * @return The char for the option chosen.
   */
  public char getOptChosen() {
    return optChosen;
  }

  /**
   * Populates the table on the management view for the stock items to show current stock levels.
   */
  public void populateStockTable() {
    try {
      stockTable.getItems().clear();
      List<Item> menuItems = menuDB.getMenu();
      for (Item item : menuItems) {
        StockTableData dataToAdd = new StockTableData(item.getID(), item.getName(), item.getPrice(),
            item.getStock(), menuDB.getAverageTimeToCook(item.getID()));

        stockLevelData.addAll(dataToAdd);
        stockTable.getItems().add(dataToAdd);
      }
    } catch (ConnectionError | ExecutionError dbe) {
      error.setVisible(true);
      error.setText("Error: " + dbe.getMessage());
    }
  }

  /**
   * Populates the table on the management view for the employee data, displaying key information
   * about employees.
   */
  public void populateEmployeeTable() {
    try {
      employeeTable.getItems().clear();
      List<Integer> employeeIDs = loginDB.getAllWaiters();
      for (Integer empID : employeeIDs) {
        Employee emp = waiterDB.getWaiterFromID(empID);
        EmployeeTableData dataToAdd =
            new EmployeeTableData(emp.getId(), emp.getName(), emp.getDateOfBirth(),
                emp.getDateOfHire(), emp.getNumOrdersAssigned(), emp.getHalfHoursWorked());

        employeeData.addAll(dataToAdd);
        employeeTable.getItems().add(dataToAdd);
      }
    } catch (ConnectionError | ExecutionError dbe) {
      error.setVisible(true);
      error.setText("Error: " + dbe.getMessage());
    }
  }

  /**
   * Calls the method to populate the stock table. Runs in a thread.
   */
  public void callPopulate() {
    Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(pollingRate), e -> {
      populateStockTable();
      populateEmployeeTable();
    }));

    timeline.play();
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
      error.setVisible(true);
      error.setText("Error: " + ioe.getMessage());
    }
  }

  /**
   * Initialises class variables when the interface is opened.
   */
  @FXML
  void initialize() {
    error.setVisible(false);
    
    try {
      AnchorPane waiterInfo =
          FXMLLoader.load(getClass().getClassLoader().getResource("Waiter.fxml"));
      waiterInfoPane.getChildren().setAll(waiterInfo);
    } catch (IOException ioe) {
      error.setVisible(true);
      error.setText("Error: " + ioe.getMessage());
    }
    
    try {
      this.menuDB = DatabaseFactory.getMenuDB();
      this.waiterDB = DatabaseFactory.getWaiterDB();
      this.loginDB = DatabaseFactory.getLoginDB();
    } catch (ConnectionError | ExecutionError dbe) {
      error.setVisible(true);
      error.setText("Error: " + dbe.getMessage());
    }
    
    pollingRate = menuDB.getPollingRate();

    // Initialises the stock data table
    this.stockLevelData = FXCollections.observableArrayList();
    stockID.setCellValueFactory(new PropertyValueFactory<>("ID"));
    stockName.setCellValueFactory(new PropertyValueFactory<>("Name"));
    stockPrice.setCellValueFactory(new PropertyValueFactory<>("Price"));
    stockLevel.setCellValueFactory(new PropertyValueFactory<>("Stock"));
    stockAvgTime.setCellValueFactory(new PropertyValueFactory<>("AvgTime"));

    // Initialises the employee data table
    this.employeeData = FXCollections.observableArrayList();
    employeeID.setCellValueFactory(new PropertyValueFactory<>("ID"));
    employeeName.setCellValueFactory(new PropertyValueFactory<>("Name"));
    employeeDoB.setCellValueFactory(new PropertyValueFactory<>("DateOB"));
    employeeDoH.setCellValueFactory(new PropertyValueFactory<>("DateOH"));
    employeeNumOfOrder.setCellValueFactory(new PropertyValueFactory<>("NumOrders"));
    employeeHoursWorked.setCellValueFactory(new PropertyValueFactory<>("HoursWorked"));

    // Initial population of tables
    populateStockTable();
    populateEmployeeTable();

    // Starts a thread to keep the stock level updated
    Platform.runLater(new Thread(() -> {
      callPopulate();
    }));
  }

  @Override
  public void start(Stage primaryStage) {
    // Unused, called by driver
  }

}
