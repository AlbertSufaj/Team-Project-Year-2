package uk.ac.rhul.cs2810.users;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import uk.ac.rhul.cs2810.containers.Item;
import uk.ac.rhul.cs2810.containers.ItemCategory;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.database.DatabaseFactory;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;
import uk.ac.rhul.cs2810.Exceptions.InvalidItemException;
import uk.ac.rhul.cs2810.database.MenuDB;

/**
 * Allows the manager to modify the items on the menu, opens a new interface with all of the
 * components of the item separated.
 */
public class ManagerModifyItem extends Application {

  private MenuDB menuDB;
  private Management management;
  private Menu menu;

  // Fields on view
  @FXML
  private AnchorPane mainPane = null;
  @FXML
  private TextField nameField = null;
  @FXML
  private TextArea descriptionField = null;
  @FXML
  private TextField priceField = null;
  @FXML
  private TextField costPriceField = null;
  @FXML
  private TextField caloriesField = null;
  @FXML
  private TextArea allergensField = null;
  @FXML
  private TextField stockLevel = null;
  @FXML
  private ComboBox categoryOptions = null;
  @FXML
  private CheckBox vegetarianCB = null;
  @FXML
  private CheckBox veganCB = null;
  @FXML
  private CheckBox glutenFreeCB = null;
  @FXML
  private Button addItem = null; // Should reload pane, not exit
  @FXML
  private Button removeItem = null;
  @FXML
  private Button updateItem = null;
  @FXML
  private Button exit = null;
  @FXML
  private Label errorMsg = null;

  private boolean error;
  private int itemID = -1;

  /**
   * Calls the methods to get the properties of the new item. If all properties match constraints,
   * adds item to the menu, otherwise displays an error.
   */
  @FXML
  public void addItem() {
    error = false;
    Item item = getItemToAdd();
    getAllergensFromWindow(item);

    if (!error) {
      try {
        menuDB.addItem(item);
        errorMsg.setText("Error: ");

        // Resets Field So Another Item Can Be Added
        nameField.setStyle(null);
        descriptionField.setStyle(null);
        categoryOptions.setStyle(null);

        nameField.clear();
        descriptionField.clear();
        categoryOptions.getSelectionModel().select(0);
        allergensField.clear();
        stockLevel.clear();
        vegetarianCB.setSelected(false);
        veganCB.setSelected(false);
        glutenFreeCB.setSelected(false);

      } catch (ConnectionError | ExecutionError dbe) {
        errorMsg.setText("Error: " + dbe.getMessage());
      } catch (InvalidItemException ivi) {
        String badInput = ivi.getBadInputColumn();
        int maxLength = ivi.getMaxLength();
        ivi.printStackTrace();

        switch (badInput) {
          case "itemname":
            errorMsg.setText("Too long input in name field");
            nameField.setStyle("-fx-background-color: #FF0000;");
            error = false;
            break;
          case "itemdesc":
            errorMsg.setText("Too long input in description field");
            descriptionField.setStyle("-fx-background-color: #FF0000;");
            error = false;
            break;
          case "category":
            errorMsg.setText("Invalid Category selected");
            categoryOptions.setStyle("-fx-background-color: #FF0000;");
            error = false;
            break;
          case "price":
            errorMsg.setText("Price not within profit margin");
            priceField.setStyle("-fx-background-color: #FF0000;");
            costPriceField.setStyle("-fx-background-color: #FF0000;");
            error = false;
            break;
          default:
            errorMsg.setText("Invalid input");
            break;
        }
      }
    }

  }

  /**
   * Performs all of the checks on the item in the interface and returns the item which was entered.
   * 
   * @return The item to add
   */
  private Item getItemToAdd() {
    this.error = false;
    ItemCategory category = (ItemCategory) categoryOptions.getSelectionModel().getSelectedItem();
    boolean vegetarian = vegetarianCB.isSelected();
    boolean vegan = veganCB.isSelected();
    boolean glutenFree = glutenFreeCB.isSelected();
    return new Item(itemID, getName(), getDescription(), getPrice(priceField),
        numCheck(caloriesField, caloriesField.getText()), category, vegetarian, vegan, glutenFree,
        numCheck(stockLevel, stockLevel.getText()), getPrice(costPriceField));
  }

  /**
   * Gets the name entered into the management window, checks if null and returns, setting error
   * true if null.
   * 
   * @return The name entered into the management window.
   */
  private String getName() {
    String name = nameField.getText();
    if (name == null) {
      this.error = true;
      nameField.setStyle("-fx-background-color: #FF0000;");
      errorMsg.setText("Error: Name not entered");
    }

    return name;
  }

  /**
   * Gets the description entered into the management window, checks if null and returns, setting
   * error true if null.
   * 
   * @return The description entered into the management window.
   */
  private String getDescription() {
    String description = descriptionField.getText();
    if (description == null) {
      this.error = true;
      descriptionField.setStyle("-fx-background-color: #FF0000;");
      errorMsg.setText("Error: Description not entered");
    }

    return description;
  }

  /**
   * Gets the price from the window, checks that it is formatted correctly and is a number.
   * 
   * @param text
   * 
   * @return The price, 0 if invalid entered
   */
  private int getPrice(TextField text) {
    String priceStr = getPricePenceStr(text);
    int price = numCheck(text, priceStr);
    if (price == 0) {
      text.setStyle("-fx-background-color: #FF0000;");
      errorMsg.setText("Error: Invalid Price");
      error = true;
    }

    return price;
  }

  private String getPricePenceStr(TextField tf) {
    String price = tf.getText();
    price = price.replace(".", "");
    price = price.replace("£", "");
    price = price.replace(" ", "");
    return price;
  }

  /**
   * Checks the string passed for a given field is valid as a number, if it is return as integer.
   * 
   * @param field The field which data is being taken from.
   * @return The integer value of the string passed, or 0 if it cannot be converted.
   */
  private int numCheck(TextField field, String str) {
    int i = 0;
    if (str.matches("\\d{1,10}?")) {
      i = Integer.parseInt(str);
    } else {
      field.setStyle("-fx-background-color: #FF0000;");
      errorMsg.setText("Error: Invalid Number");
      error = true;
    }

    return i;
  }

  /**
   * Gets the allergens entered into the allergens box.
   * 
   * @param item The new item.
   */
  private void getAllergensFromWindow(Item item) {
    String allergens = allergensField.getText();
    allergens = allergens.replace(",", "");
    for (String agn : allergens.split(" ")) {
      item.addAllergen(agn);
    }
  }

  /**
   * Removes the item selected from the database.
   */
  @FXML
  public void removeItem() {
    Item item = getItemToAdd();
    try {
      menuDB.removeItem(item.getID());
    } catch (ConnectionError | ExecutionError dbe) {
      errorMsg.setText("Error: " + dbe.getMessage());
    }
  }

  /**
   * Updates the item in the menu by getting the item, then giving it to the database to modify it.
   */
  @FXML
  public void updateItem() {
    error = false;
    Item item = getItemToAdd();
    getAllergensFromWindow(item);

    if (!error) {
      try {
        menuDB.modifyItem(item);
      } catch (ConnectionError | ExecutionError dbe) {
        errorMsg.setText("Error: " + dbe.getMessage());
      } catch (InvalidItemException ivi) {
        String badInput = ivi.getBadInputColumn();
        int maxLength = ivi.getMaxLength();
        ivi.printStackTrace();

        switch (badInput) {
          case "itemname":
            errorMsg.setText("Too long input in name field");
            nameField.setStyle("-fx-background-color: #FF0000;");
            error = false;
            break;
          case "itemdesc":
            errorMsg.setText("Too long input in description field");
            descriptionField.setStyle("-fx-background-color: #FF0000;");
            error = false;
            break;
          case "category":
            errorMsg.setText("Invalid Category selected");
            categoryOptions.setStyle("-fx-background-color: #FF0000;");
            error = false;
            break;
          case "price":
            errorMsg.setText("Price not within profit margin");
            priceField.setStyle("-fx-background-color: #FF0000;");
            costPriceField.setStyle("-fx-background-color: #FF0000;");
            error = false;
            break;
          default:
            errorMsg.setText("Invalid input");
            break;
        }
      }
    }
  }

  /**
   * Returns the user back to the main screen.
   */
  @FXML
  public void exit() {
    try {
      AnchorPane newPane =
          FXMLLoader.load(getClass().getClassLoader().getResource("Management.fxml"));
      mainPane.getChildren().setAll(newPane);
    } catch (IOException ioe) {
      errorMsg.setText("Error: " + ioe.getMessage());
    }
  }

  /**
   * Initialises variables. Populates the fields based upon the button pressed.
   */
  @FXML
  void initialize() {
    try {
      this.menuDB = DatabaseFactory.getMenuDB();
    } catch (ConnectionError | ExecutionError dbe) {
      errorMsg.setText("Error: " + dbe.getMessage());
    }
    // Adds all available categories
    ItemCategory[] cat = ItemCategory.values();
    for (ItemCategory category : cat) {
      categoryOptions.getItems().add(category);
    }

    this.management = new Management();
    this.menu = new Menu();

    if (management.getOptChosen() != 'a') {
      Item item = menu.getItem();
      itemID = item.getID();

      // Populates the fields on the interface from the item given
      nameField.setText(item.getName());
      descriptionField.setText(item.getDescription());
      priceField.setText(item.getPrice().toString());
      caloriesField.setText(String.valueOf(item.getCalories()));
      categoryOptions.getSelectionModel().select(item.getCategory());
      allergensField.setText(item.getAllergens().toString());
      stockLevel.setText(String.valueOf(item.getStock()));
      vegetarianCB.setSelected(item.isVegi());
      veganCB.setSelected(item.isVegan());
      glutenFreeCB.setSelected(item.isGlutenFree());
      costPriceField.setText(item.getCostPrice().toString());

      addItem.setDisable(true);
      removeItem.setDisable(true);

      if (management.getOptChosen() == 'r') {
        nameField.setDisable(true);
        descriptionField.setDisable(true);
        caloriesField.setDisable(true);
        categoryOptions.setDisable(true);
        allergensField.setDisable(true);
        stockLevel.setDisable(true);
        vegetarianCB.setDisable(true);
        veganCB.setDisable(true);
        glutenFreeCB.setDisable(true);
        priceField.setDisable(true);
        costPriceField.setDisable(true);
        removeItem.setDisable(false);
      }
    }
  }

  @Override
  public void start(Stage primaryStage) {
    // Not used, program started by driver
  }

}
