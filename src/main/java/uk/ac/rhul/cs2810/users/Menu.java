package uk.ac.rhul.cs2810.users;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;
import uk.ac.rhul.cs2810.Exceptions.MissingDataError;
import uk.ac.rhul.cs2810.containers.Item;
import uk.ac.rhul.cs2810.containers.ItemCategory;
import uk.ac.rhul.cs2810.containers.Order;
import uk.ac.rhul.cs2810.containers.Price;
import uk.ac.rhul.cs2810.database.*;

/**
 * Allows the customer to interact with a GUI to place orders which will be stored into the
 * database.
 */
public class Menu extends Application {

  private static Order newOrder;
  private TableSelector tableSelector;
  private static MenuDB menuDB;
  private ImageDB imageDB;
  private Login login;
  private static Item itemSelected;
  private static WaiterOrderOptions wOrdOpt;

  List<Item> menu;
  private Map<ListView, LinkedList<Item>> listToItemsPicked;
  private Map<ItemCategory, ListView> categoryToViewMap;

  // Mains Lists
  @FXML
  private ListView salads = null;
  @FXML
  private ListView tacos = null;
  @FXML
  private ListView quesadillas = null;
  @FXML
  private ListView burritos = null;
  @FXML
  private ListView enchiladas = null;
  @FXML
  private ListView extras = null;

  // Sides List
  @FXML
  private ListView sides = null;

  // Drinks Lists
  @FXML
  private ListView softDrinks = null;
  @FXML
  private ListView mocktails = null;
  @FXML
  private ListView beers = null;
  @FXML
  private ListView cocktails = null;
  @FXML
  private ListView tequilas = null;
  @FXML
  private ListView spirits = null;

  // Current Order
  @FXML
  private ListView showOrder = null;
  @FXML
  private Label total = null;
  @FXML
  private Label error = null;

  // Filters sets
  private boolean vegetarian = false;
  private boolean vegan = false;
  private boolean glutenFree = false;
  private List<String> allergens = null;
  private Set<String> allergensSelected = new HashSet<>();

  @FXML
  private AnchorPane mainPane = null;
  @FXML
  private ListView allergensList = null;
  @FXML
  private Button applyFilters = null;
  @FXML
  private CheckBox vegetarianCheckBox = null;
  @FXML
  private CheckBox veganCheckBox = null;
  @FXML
  private CheckBox glutenFreeCheckBox = null;
  @FXML
  private CheckBox unavailableCheckBox = null;
  @FXML
  private Label currentOrderLabel = null;

  // Running Total
  private Price runningTotal = new Price(0);

  private boolean ignoreStock;

  // Allergen Filtering
  private List<Integer> indicesList = new ArrayList<>();

  /**
   * Blank menu constructor.
   */
  public Menu() {}

  private void showOrderStored() {
    for (Item item : newOrder.getItemsInOrder()) {
      runningTotal = runningTotal.add(item.getPrice().getPriceValue());
      displayOrder(item);
    }
  }

  /*
   * ORDERING
   */

  /**
   * Returns the order which has been created.
   * 
   * @return Returns the order which has been created
   */
  public Order getOrder() {
    return newOrder;
  }

  /**
   * Resets the stored order from the waiter interface on checkout.
   */
  public void resetOrder() {
    newOrder = null;
  }

  /**
   * Removes the specified item from the order.
   */
  @FXML
  public void removeItem() {
    int index = showOrder.getSelectionModel().getSelectedIndex();
    if (index > -1) {
      List<Item> orderContents = newOrder.getItemsInOrder();
      Item itemToRemove = orderContents.get(index);
      newOrder.removeItemFromOrder(itemToRemove);
      showOrder.getItems().remove(index);
      runningTotal = runningTotal.subtract(itemToRemove.getPrice().getPriceValue());
      showTotal();
    }
  }

  /**
   * Displays the basket containing the items the user has added thus far and allows them to remove
   * or amend items.
   * 
   * @param itemToAdd The item which as been added to the order to be displayed.
   */
  public void displayOrder(Item itemToAdd) {
    showOrder.getItems().add(itemToAdd.getName());
    showTotal();
  }

  /**
   * Displays the running total cost of the order to the user.
   */
  public void showTotal() {
    total.setText("Total: " + runningTotal.toString());
  }

  /**
   * Gets the order number of the order created.
   * 
   * @return Returns the new order number.
   */
  public int getOrderNumber() {
    return newOrder.getID();
  }

  /*
   * MENU FILTERING
   */

  /**
   * Redisplays the menu including items which are out of stock.
   */
  @FXML
  public void showUnavailable() {
    ignoreStock = true;
    getMenu();
  }

  /**
   * Gets the list of allergens from the database and populates the list on the UI.
   */
  public void getAllergensFromDatabase() {
    try {
      allergens = DatabaseFactory.getMenuDB().getAllergens();
      for (String str : allergens) {
        str = str.substring(0, 1).toUpperCase() + str.substring(1);
        allergensList.getItems().add(str);
      }
    } catch (ConnectionError | ExecutionError dbe) {
      dbe.printStackTrace(); // Prints to error file
      error.setText("Error: " + dbe.getMessage());
    }
  }

  /**
   * When a user clicks on an allergen, stores it in a list and highlights the allergen selected. If
   * the user clicks again it is removed from the list.
   */
  @FXML
  void onAllergensListClick() {
    int index = allergensList.getSelectionModel().getSelectedIndex();
    if (index > -1) {
      String optSelected = allergens.get(allergensList.getSelectionModel().getSelectedIndex());
      if (!allergensSelected.contains(optSelected)) {
        allergensSelected.add(optSelected);
        indicesList.add(index);
      } else {
        allergensSelected.remove(optSelected);
        indicesList.remove(new Integer(index));
      }

      // Sets all of the items selected as active
      int[] indices = new int[indicesList.size()];
      for (int i = 0; i < indicesList.size(); i++) {
        indices[i] = indicesList.get(i);
      }
      allergensList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
      MultipleSelectionModel<?> selectionModel = allergensList.getSelectionModel();
      selectionModel.clearSelection();
      selectionModel.selectIndices(-1, indices);
    }
  }

  /**
   * Sets a flag for if vegetarian has been selected.
   */
  @FXML
  void vegetarianSelected() {
    this.vegetarian = vegetarianCheckBox.isSelected();
  }

  /**
   * Sets a flag for if vegan has been selected.
   */
  @FXML
  void veganSelected() {
    this.vegan = veganCheckBox.isSelected();
  }

  /**
   * Sets a flag for if gluten free has been selected.
   */
  @FXML
  void glutenFreeSelected() {
    this.glutenFree = glutenFreeCheckBox.isSelected();
  }

  /**
   * Gets the new menu from the database which as been filtered.
   */
  @FXML
  public void getFilteredMenu() {
    clearMenu();
    try {
      menu = DatabaseFactory.getMenuDB()
          .getFilteredMenu(new Filter(allergensSelected, vegetarian, vegan, glutenFree));
    } catch (ConnectionError | ExecutionError dbe) {
      dbe.printStackTrace(); // Prints to error file
      error.setText("Error: " + dbe.getMessage());
    }
    showFilteredMenu();
  }

  /**
   * Clears the menu items before a new menu is added with different items.
   */
  public void clearMenu() {
    for (ItemCategory key : categoryToViewMap.keySet()) {
      categoryToViewMap.get(key).getItems().clear(); // Clears old items
    }
    for (ListView listView : listToItemsPicked.keySet()) {
      listToItemsPicked.get(listView).clear();
    }
  }

  /**
   * Displays a list of filters to the customer and allows them to apply the filters, retrieves
   * filtered items from the menu DB.
   */
  public void showFilteredMenu() {
    populateMenu();
  }

  /*
   * GET ITEMS FROM MENU
   */

  /**
   * Gets the item which was selected from the menu.
   * 
   * @return The item which was selected in the interface.
   */
  public Item getItem() {
    return itemSelected;
  }

  /**
   * Gets the item which the user has selected in the UI and adds it to the order as customer. Gets
   * the item which the user has selected in the UI and opens a new window to modify/ delete as
   * management.
   */
  @FXML
  private void onClick() {
    boolean run = false;
    for (ListView listView : listToItemsPicked.keySet()) {
      int index = Math.floorDiv(listView.getSelectionModel().getSelectedIndex(), 2);
      if (index > -1) {
        List<Item> listItems = listToItemsPicked.get(listView);
        itemSelected = listItems.get(index);
        if (login.getUserType() == 'm') {
          // Opens the management window with the item already loaded.
          try {
            AnchorPane newPane = FXMLLoader
                .load(getClass().getClassLoader().getResource("ManagementModifyItem.fxml"));
            mainPane.getChildren().setAll(newPane);
          } catch (IOException ioe) {
            ioe.printStackTrace(); // Prints to error file
            error.setText("Error: " + ioe.getMessage());
          }

        } else {
          // Updates the menu to check available stock
          try {
            menu = DatabaseFactory.getMenuDB()
                .getFilteredMenu(new Filter(allergensSelected, vegetarian, vegan, glutenFree));
          } catch (ConnectionError | ExecutionError dbe) {
            dbe.printStackTrace(); // Prints to error file
            error.setText("Error: " + dbe.getMessage());
          }
          

          // Gets the current number of items in the order of that type
          Map<Item, Integer> occurance = newOrder.getOccuranceMap();
          int occurancesInOrder = occurance.getOrDefault(itemSelected, 0);

          // Compares the number of items available to the number of items selected
          if (itemSelected.getStock() > occurancesInOrder) {
            newOrder.addItemToOrder(itemSelected);
            runningTotal = runningTotal.add(itemSelected.getPrice().getPriceValue());
            displayOrder(itemSelected);
            break;
          } else {
            error.setStyle("-fx-background-color: #FF0000;");
            error.setText("Item not available");
            break;
          }
        }
      }
    }

    Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
      error.setStyle(null);
      error.setText("Click an item to remove it");
    }));

    timeline.setCycleCount(1);
    timeline.play();
  }

  /*
   * DISPLAY MENU
   */

  /**
   * Made a separate get menu method to get the menu at the start without filters applied.
   */
  private void getMenu() {
    try {
      menuDB.setIgnoreOutOfStock(ignoreStock);
      menu = menuDB.getMenu();
    } catch (ConnectionError | ExecutionError dbe) {
      dbe.printStackTrace(); // Prints to error file
      error.setText("Error: " + dbe.getMessage());
    }
  }

  /**
   * Populates each list on the menu with its relevant items.
   */
  private void populateMenu() {
    try {
      for (Item item : menu) {
        String itemAdded = item.toString();

        if (!categoryToViewMap.containsKey(item.getCategory())) {
          throw new MissingDataError("Category not set");
        } else {
          ListView listView = categoryToViewMap.get(item.getCategory());
          List<Item> itemList = listToItemsPicked.get(listView);
          listView.getItems().add(itemAdded);
          Image image = imageDB.getImage(item.getID());
          ImageView imageView = new ImageView(image);
          listView.getItems().add(imageView);
          itemList.add(item);
        }
      }
    } catch (ConnectionError | ExecutionError | MissingDataError dbe) {
      dbe.printStackTrace(); // Prints to error file
      error.setText("Error: " + dbe.getMessage());
    }
  }

  /*
   * START
   */

  /**
   * Initialise.
   */
  @FXML
  void initialize() {
    // Gets table number
    this.tableSelector = new TableSelector();
    this.login = new Login();
    this.ignoreStock = false;
    wOrdOpt = new WaiterOrderOptions();

    try {
      menuDB = DatabaseFactory.getMenuDB();
      this.imageDB = DatabaseFactory.getImageDB();
    } catch (ConnectionError | ExecutionError dbe) {
      dbe.printStackTrace(); // Prints to error file
      error.setText("Error: " + dbe.getMessage());
    }

    // Populates the maps
    listToItemsPicked = new HashMap<>();
    categoryToViewMap = new HashMap<>();
    for (Field field : Menu.class.getDeclaredFields()) { // Gets all the variables
      if (!Modifier.isPrivate(field.getModifiers()) || field.getType() != ListView.class) {
        continue; // Filters out the variables that aren't a private list view
      }

      if (!Arrays.toString(ItemCategory.values()).contains(field.getName().toUpperCase())) {
        continue; // Removes anything that isn't associated with an item category
      }
      try {
        ListView view = (ListView) field.get(this); // Gets the list view associated
        categoryToViewMap.put(ItemCategory.toCategory(field.getName()), view);
        listToItemsPicked.put(view, new LinkedList<>());
      } catch (IllegalAccessException IAE) {
        // We know we have access to this variable
        IAE.printStackTrace();
      }
    }

    // Populates menu information
    getMenu();
    populateMenu();
    getAllergensFromDatabase();


    // Gets the previously stored order for the table if there is one
    if (login.getUserType() == 'c' || login.getUserType() == 'w') {
      if (newOrder == null) {
        newOrder = new Order(Integer.parseInt(tableSelector.getTableNum()));
      }
      // Fills saved order
      showOrderStored();
    } else {
      // Disables part of the menu for the management interface
      showOrder.setVisible(false);
      error.setVisible(false);
      total.setVisible(false);
      currentOrderLabel.setVisible(false);
    }
  }

  @Override
  public void start(Stage primaryStage) {

  }

}
