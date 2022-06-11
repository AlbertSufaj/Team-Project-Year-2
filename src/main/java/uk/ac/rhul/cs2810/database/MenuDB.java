package uk.ac.rhul.cs2810.database;

import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;
import uk.ac.rhul.cs2810.Exceptions.InvalidItemException;
import uk.ac.rhul.cs2810.containers.Item;
import uk.ac.rhul.cs2810.containers.ItemCategory;
import uk.ac.rhul.cs2810.containers.Order;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.time.LocalTime;
import java.util.*;

import static java.lang.Math.max;
import static java.lang.Math.round;

/**
 * Deals with interacting with the menu part of the DB.
 */
public class MenuDB extends Database {
  
  private boolean ignoreStock = true;
  private final int ttlSeconds = 60;
  private Map<Filter, List<Item>> filterToItems;
  private Map<Filter, LocalTime> lastUpdated;
  
  /**
   * Instantiates a new Menu db.
   *
   * @param testing the testing
   * @throws ConnectionError when unable to connect to the database
   * @throws ExecutionError  when there's an issue setting up the tables
   */
  MenuDB(boolean testing) throws ConnectionError, ExecutionError {
    super(new String[]{"allergens", "menu", "allergensInItems"}, testing);
    
    filterToItems = new HashMap<>();
    lastUpdated = new HashMap<>();
    
    layouts.put("menu", new char[]{'i', 's', 's', 'i', 'i', 's', 'b', 'b', 'b', 'i', 'i'});
    layouts.put("allergens", new char[]{'i', 's'});
    layouts.put("allergensInItems", new char[]{'i', 'i'});
    
    insertStatments.put("menu", "INSERT INTO Menu (ITEMID, ITEMNAME, ITEMDESC, CALORIES," +
        " PRICE, CATEGORY, IsVegi, IsVegen, IsGlutenFree, Stock, CostPrice)" +
        " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
    insertStatments.put("allergens", "INSERT INTO Allergens VALUES (?, ?);");
    insertStatments.put("allergensInItems", "INSERT INTO AllergensInItems VALUES (?, ?);");
    
    Statement st = getStatement();
    for (String table : tables) {
      try {
        ResultSet rs = st.executeQuery("SELECT count(*) FROM " + table + ";");
        rs.next();
        if (rs.getInt(1) == 0) {
          if (format.equals("S")) {
            populateTable(table);
          } else {
            populateTables("menu.csv");
            break;
          }
        }
      } catch (SQLException SQLE) {
        throw new ExecutionError("Couldn't count items in table " + table, SQLE);
      }
    }
    try {
      st.close();
      Database.closeConnection(st.getConnection());
    } catch (SQLException SQLE) {
      // Not worried with closing issues
    }
  }
  
  /**
   * Gets the menu.
   *
   * @return the menu
   * @throws ConnectionError when unable to connect to database
   * @throws ExecutionError when able to fetch the menu
   */
  public List<Item> getMenu() throws ConnectionError, ExecutionError {
    return getFilteredMenu(new Filter(new HashSet<>(), false, false, false));
  }
  
  /**
   * Gets a map of item id to menu item.
   *
   * @return the map
   * @throws ExecutionError when unable to fetch the data from the database
   * @throws ConnectionError when unable to connect to database
   */
  public Map<Integer, Item> getMap() throws ExecutionError, ConnectionError {
    Map<Integer, Item> itemMap = new HashMap<>();
    
    Statement st = getStatement();
    ResultSet rs = null;
    
    // Gets items
    try {
      rs = st.executeQuery("SELECT * FROM menu");
    } catch (SQLException SQLE) {
      throw new ExecutionError("Couldn't execute query to get items", SQLE);
    }
    try {
      while (rs.next()) {
        itemMap.put(rs.getInt(1), // Items ID
            new Item(rs.getInt(1), rs.getString(2), rs.getString(3), // Item
                rs.getInt(4), rs.getInt(5), ItemCategory.toCategory(rs.getString(6)),
                rs.getBoolean(7), rs.getBoolean(8), rs.getBoolean(9), rs.getInt(10),
                rs.getInt(11)));
      }
    } catch (SQLException SQLE) {
      throw new ExecutionError("Couldn't read next item", SQLE);
    }
    
    // Gets each items allergens
    for (Item item : itemMap.values()) {
      addAllergensToItem(st, item);
    }
    
    try {
      Database.closeConnection(st.getConnection());
    } catch (SQLException SQLE) {
      // Closing errors not considered important
    }
    
    return itemMap;
  }
  
  /**
   * Gets a filtered menu.
   *
   * @param filter the filter to use
   * @return the list of items
   * @throws ConnectionError when unable to connect to the database
   * @throws ExecutionError when unable to get the menu from the database
   */
  public List<Item> getFilteredMenu(Filter filter)
      throws ConnectionError, ExecutionError {
    
    LocalTime time = LocalTime.now();
    LocalTime validTime = time.minusSeconds(2);
  
    // If a recent cache is kept use the cache
    if (lastUpdated.containsKey(filter) && lastUpdated.get(filter).isAfter(validTime)) {
      return filterToItems.get(filter);
    }
    
    Connection connection;
    Statement st = getStatement();
    
    try { // Get connection
      connection = DriverManager.getConnection(URL, userName, password);
    } catch (SQLException SQLE) {
      throw new ConnectionError("Could not connect to database", SQLE);
    }
    
    try { // Clear old data
      st.execute("DROP TABLE IF EXISTS badallergenid CASCADE");
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not remove redundant table", SQLE);
    }
    
    StringBuilder blanks;
    String statement = "";
    if (filter.getAllergens().size() > 0) {
      blanks = new StringBuilder("(");
      blanks.append("?, ".repeat(filter.getAllergens().size()));
      blanks = new StringBuilder(blanks.substring(0, blanks.length() - 2));
      blanks.append(")");
      statement = "CREATE TABLE badAllergenID AS " +
          "SELECT allergenid FROM allergens " +
          "WHERE allergenname IN " + blanks + ";";
    } else {
      statement = "CREATE TABLE badAllergenID AS " +
          "SELECT allergenid FROM allergens " +
          "WHERE allergenId != allergenId";
    }
    
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      ps = connection.prepareStatement(statement);
      
      int count = 1; // SQL indexes from 1
      for (String allergen : filter.getAllergens()) {
        ps.setString(count, allergen);
        count++;
      }
      ps.execute();
      
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not fetch allergenIDs from table", SQLE);
    }
    
    try {
      ps = connection.prepareStatement("CREATE VIEW badItemID AS " +
          "SELECT itemID FROM allergensinitems " +
          "WHERE allergenid IN (SELECT * FROM badAllergenID);");
      ps.execute();
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not fetch itemIDs from table", SQLE);
    }
    
    try {
      ps = connection.prepareStatement("SELECT * FROM menu " +
          "WHERE itemid NOT IN (SELECT * FROM badItemID) " +
          "AND isVegi >= ? AND isVegen >= ? AND isGlutenFree >= ? AND Stock > ?;");
      ps.setBoolean(1, filter.isVegi());
      ps.setBoolean(2, filter.isVegan());
      ps.setBoolean(3, filter.isGlutenFree());
      if (ignoreStock) {
        ps.setInt(4, -1);
      } else {
        ps.setInt(4, 0);
      }
      rs = ps.executeQuery();
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not fetch itemIDs from table", SQLE);
    }
    
    List<Item> items = getItemsFromResultSet(rs);
    
    try {
      ps.close();
      rs.close();
      st.close();
      connection.close();
    } catch (SQLException SQLE) {
      //Not concerned with closing errors
    }
    
    time = LocalTime.now();
    // Adds the items to cache
    lastUpdated.put(filter, time);
    filterToItems.put(filter, items);
    
    return items;
  }
  
  /**
   * Gets a list of allergens present in any of the food.
   *
   * @return the list of allergens
   * @throws ConnectionError when unable to connect to the database
   * @throws ExecutionError when unable to get the list of allergens from the database
   */
  public List<String> getAllergens() throws ConnectionError, ExecutionError {
    Statement st = getStatement();
    ResultSet rs;
    List<String> allergens = new LinkedList<>();
    
    try {
      rs = getStatement().executeQuery("SELECT allergenname FROM allergens");
      while (rs.next()) {
        allergens.add(rs.getString(1));
      }
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not get allergen list", SQLE);
    }
    
    try {
      rs.close();
      Database.closeConnection(st.getConnection());
    } catch (SQLException SQLE) {
      // Closing exceptions not an issue atm
    }
    
    return allergens;
  }
  
  /**
   * Sets whether or not to ignore items not in stock.
   *
   * @param ignoreStock whether or not to ignore items out of stock
   */
  public void setIgnoreOutOfStock(boolean ignoreStock) {
    this.ignoreStock = ignoreStock;
  }
  
  /**
   * Decreases the stock level
   *
   * @param order the order to decrease the stock by
   * @throws ConnectionError when unable to connect to the database
   * @throws ExecutionError when unable to update the database
   */
  public void decreaseStock(Order order) throws ConnectionError, ExecutionError {
    Connection connection = getConnection();
    try {
      PreparedStatement ps = connection.prepareStatement("UPDATE menu SET stock = stock - 1" +
          " WHERE itemid = ?");
      for (Item item : order.getItemsInOrder()) {
        ps.setInt(1, item.getID());
        ps.addBatch();
      }
      ps.executeBatch();
    } catch (SQLException SQLE) {
      if (SQLE.getMessage().contains("stock")) {
        throw new ExecutionError("Order out of stock");
      } else {
        throw new ExecutionError("Could not update stock levels", SQLE);
      }
    } finally {
      closeConnection(connection);
    }
  }
  
  /**
   * Adds the given item to the database.
   *
   * @param item the item to add
   * @throws ConnectionError      when unable to connect to the database
   * @throws ExecutionError       when unable to update the database
   * @throws InvalidItemException when the item contains invalid data
   */
  public void addItem(Item item) throws ConnectionError, ExecutionError, InvalidItemException {
    
    List<String> seenAllergens = getAllergens();
    
    String[] itemData = new String[11]; // Gets the correct item data
    if (item.getID() == -1) {
      itemData[0] = String.valueOf(Collections.max(getMap().keySet()) + 1);
    } else {
      itemData[0] = String.valueOf(item.getID()); // Allows replacing items
    }
    itemData[1] = item.getName();
    itemData[2] = item.getDescription();
    itemData[3] = String.valueOf(item.getCalories()); // Gets the actual numbers
    itemData[4] = String.valueOf(item.getPrice().getPriceValue());
    itemData[5] = item.getCategory().toString();
    itemData[6] = String.valueOf(item.isVegi());
    itemData[7] = String.valueOf(item.isVegan());
    itemData[8] = String.valueOf(item.isGlutenFree());
    itemData[9] = String.valueOf(item.getStock());
    itemData[10] = String.valueOf(item.getCostPrice().getPriceValue());
    
    Connection connection = getConnection();
    PreparedStatement ps;
    try {
      ps = inputIntoTable("menu", itemData, connection);
    } catch (SQLException SQLE) {
      closeConnection(connection);
      throw new ExecutionError("Could not add the main item data");
    }
    try {
      executeInsert(ps);
    } catch (ExecutionError EE) {
      closeConnection(connection);
      dealWithItemError(EE, itemData);
    }
    
    PreparedStatement allergenPs = null;
    PreparedStatement allergenInItemsPs = null;
    
    for (String allergen : item.getAllergens()) {
      try {
        if (!seenAllergens.contains(allergen)) {
          seenAllergens.add(allergen);
          String allergenID = String.valueOf(seenAllergens.indexOf(allergen) - 1);
          if (allergenPs == null) {
            allergenPs = inputIntoTable("allergens", new String[]{allergenID, allergen},
                connection);
          } else {
            inputIntoTable("allergens", new String[]{allergenID, allergen}, allergenPs);
          }
        }
        
        String allergenID = String.valueOf(seenAllergens.indexOf(allergen) - 1);
        if (allergenInItemsPs == null) {
          allergenInItemsPs = inputIntoTable("allergensInItems", new String[]{allergenID,
              itemData[0]}, connection);
        } else {
          inputIntoTable("allergensInItems", new String[]{allergenID, itemData[0]},
              allergenInItemsPs);
        }
      } catch (SQLException SQLE) {
        throw new ExecutionError("Couldn't add allergens to item");
      }
    }
    if (allergenInItemsPs != null) {
      executeInsert(allergenInItemsPs);
    }
  }
  
  /**
   * Removes the item with the given ID from the database.
   *
   * @param itemID the item id of the item to remove
   * @throws ConnectionError when unable to connect to the database
   * @throws ExecutionError  when unable to remove the item
   */
  public void removeItem(int itemID) throws ConnectionError, ExecutionError {
    Connection connection = getConnection();
    
    try {
      PreparedStatement ps = connection.prepareStatement("DELETE FROM allergensinitems" +
          " WHERE itemid = ?");
      ps.setInt(1, itemID);
      ps.execute();
    } catch (SQLException SQLE) {
      closeConnection(connection);
      throw new ExecutionError("Could not remove item", SQLE);
    }
    
    try {
      PreparedStatement ps = connection.prepareStatement("DELETE FROM menu WHERE itemid = ?");
      ps.setInt(1, itemID);
      ps.execute();
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not remove item", SQLE);
    } finally {
      closeConnection(connection);
    }
    
  }
  
  /**
   * Modifies the item in the database.
   * Replaces the item with the same ID with the passed item
   *
   * @param item the new version of the item
   * @throws ConnectionError      when unable to connect to the database
   * @throws ExecutionError       when unable to change the item in the database
   * @throws InvalidItemException when the new version of the item is invalid
   */
  public void modifyItem(Item item) throws ConnectionError, ExecutionError, InvalidItemException {
    Connection connection = getConnection();
    lastUpdated.put(new Filter(new HashSet<>(), false, false, false),
        LocalTime.MIN);
    
    Item origItem = null;
    List<Item> originalMenu = getMenu();
    for (Item i: originalMenu){
      if (i.getID() == item.getID()){
        origItem = i;
      }
    }
    
    if (origItem == null){
      throw new InvalidItemException("Item not in database", null, null, -1);
    }
    
    try {
      PreparedStatement ps = connection.prepareStatement("UPDATE menu SET itemname = ?," +
          " itemdesc = ?, price = ?, calories = ?, category = ?, isvegi = ?, isvegen = ?," +
          " isglutenfree = ?, stock =  ?, costPrice = ? " +
          "WHERE itemid = ?");
      
      ps.setString(1, item.getName());
      ps.setString(2, item.getDescription());
      ps.setInt(3, item.getPrice().getPriceValue());
      ps.setInt(4, item.getCalories());
      ps.setString(5, item.getCategory().toString());
      ps.setBoolean(6, item.isVegi());
      ps.setBoolean(7, item.isVegan());
      ps.setBoolean(8, item.isGlutenFree());
      ps.setInt(9, item.getStock());
      ps.setInt(10, item.getCostPrice().getPriceValue());
      ps.setInt(11, item.getID());
      ps.execute();
    } catch (SQLException SQLE) {
      closeConnection(connection);
      String[] itemData = new String[11];
      
      itemData[1] = item.getName(); // Only need the parts likely to throw an error
      itemData[2] = item.getDescription();
      itemData[4] = String.valueOf(item.getPrice().getPriceValue());
      itemData[9] = String.valueOf(item.getStock());
      itemData[10] = String.valueOf(item.getCostPrice());
      
      dealWithItemError(new ExecutionError("Could not modify item", SQLE), itemData);
    }
    
    if (origItem.getAllergens() != item.getAllergens()){
      try {
        PreparedStatement ps = connection.prepareStatement("DELETE FROM allergensinitems " +
            "WHERE itemid = ?");
        ps.setInt(1, item.getID());
        ps.execute();
      } catch (SQLException SQLE) {
        throw new ExecutionError("Could not clear old allergens");
      }
      
      try {
        PreparedStatement ps = connection.prepareStatement("INSERT INTO allergensinitems " +
            "VALUES ((SELECT allergenid FROM allergens WHERE allergenname = ?), ?)");
        for (String allergen: item.getAllergens()) {
          ps.setString(1, allergen);
          ps.setInt(2, item.getID());
          ps.addBatch();
        }
        ps.executeBatch();
      } catch (SQLException SQLE) {
        throw new ExecutionError("Could not add new allergens");
      }
    }
    
  }
  
  /**
   * Gets average time to cook the given item.
   *
   * @param itemID the items id
   * @return the average time to cook
   * @throws ConnectionError when unable to connect to the database
   * @throws ExecutionError  when unable to fetch the data from the database
   */
  public float getAverageTimeToCook(int itemID) throws ConnectionError, ExecutionError {
    Connection connection = getConnection();
    int timeUncooked;
    int numOrders;
    try {
      PreparedStatement ps = connection.prepareStatement("SELECT NumServed, TotalTimeToCook " +
          "FROM menu WHERE itemid = ?");
      ps.setInt(1, itemID);
      ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
        throw new ExecutionError("Could not find item with id " + itemID);
      }
      numOrders = rs.getInt(1);
      timeUncooked = rs.getTime(2).toLocalTime().getMinute();
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not get data", SQLE);
    } finally {
      closeConnection(connection);
    }
    return (float) timeUncooked / (float) numOrders;
  }
  
  /**
   * Adds an order to the cooking time data.
   *
   * @param order the order to add
   * @param time  the time it took to cook
   * @throws ConnectionError when unable to connect to the database
   * @throws ExecutionError when unable to add the data to the database
   */
  public void addOrderToData(Order order, int time) throws ConnectionError, ExecutionError {
    int averageTime = round((float) time / (float) order.getItemCount());
    Connection connection = getConnection();
    try {
      PreparedStatement ps = connection.prepareStatement("UPDATE menu " +
          "SET NumServed = NumServed + 1, TotalTimeToCook = TotalTimeToCook + ?::INTERVAL " +
          "WHERE itemid = ?");
      for (Item item : order.getItemsInOrder()) {
        ps.setString(1, averageTime + " minutes");
        ps.setInt(2, item.getID());
        ps.addBatch();
      }
      ps.executeBatch();
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not update cooking time stats", SQLE);
    } finally {
      closeConnection(connection);
    }
  }
  
  @Override
  protected void makeTables() throws ConnectionError, ExecutionError {
    Statement st = getStatement();
    
    try {
      st.execute("CREATE TABLE IF NOT EXISTS menu(" + //
          "ItemID int PRIMARY KEY," + //
          "ItemName varchar(30) not null ," + //
          "ItemDesc varchar(70) not null ," + //
          "Price int not null ," + //
          "Calories int CHECK ( Calories>=0 )," + //
          "Category varchar(15)," + //
          "IsVegi bool DEFAULT false," + //
          "IsVegen bool DEFAULT false," + //
          "IsGlutenFree bool DEFAULT false," + //
          "Stock int DEFAULT 0," + //
          "CostPrice int not null, " + //
          "NumServed int DEFAULT 0," + //
          "TotalTimeToCook INTERVAL DEFAULT '0 mins'::INTERVAL," + //
          "CONSTRAINT Profit CHECK ( Price >= 1.6*CostPrice )," + //
          "CONSTRAINT StockLevel CHECK ( Stock>=0 )" + //
          ");");
    } catch (SQLException SQLE) {
      throw new ExecutionError("Couldn't make table menu", SQLE);
    }
    
    try {
      st.execute("CREATE TABLE IF NOT EXISTS allergens(" +
          "AllergenID int PRIMARY KEY," +
          "AllergenName varchar(20)" +
          ");");
    } catch (SQLException SQLE) {
      throw new ExecutionError("Couldn't make table allergens", SQLE);
    }
    
    try {
      st.execute("CREATE TABLE IF NOT EXISTS allergensInItems(" +
          "AllergenID int," +
          "ItemID int," +
          "PRIMARY KEY (AllergenID, ItemID)," +
          "FOREIGN KEY (AllergenID) REFERENCES allergens(AllergenID)," +
          "FOREIGN KEY (ItemID) REFERENCES menu(ItemID)" +
          ");");
    } catch (SQLException SQLE) {
      throw new ExecutionError("Couldn't make table allergensInItems", SQLE);
    }
    
    try {
      Database.closeConnection(st.getConnection());
    } catch (SQLException SQLE) {
      // Closing errors not considered important
    }
  }
  
  /**
   * @param table The name of the table to populate
   * @throws ExecutionError  Thrown when theres an error
   * @throws ConnectionError Thrown when the system can't connect to the database
   */
  private void populateTable(String table) throws ExecutionError, ConnectionError {
    Connection connection;
    PreparedStatement ps = null;
    BufferedReader tableFile = null;
    
    
    // Connects to the database
    try {
      connection = DriverManager.getConnection(URL, userName, password);
    } catch (SQLException SQLE) {
      throw new ConnectionError("Could not connect to database", SQLE);
    }
    
    try {
      tableFile = loadFile(table + ".txt");
      
      String line = tableFile.readLine();
      
      while (line != null) {
        if (ps == null) {
          ps = inputIntoTable(table, line.split(","), connection);
        } else {
          inputIntoTable(table, line.split(","), ps);
        }
        line = tableFile.readLine();
      }
      
      if (ps != null) {
        executeInsert(ps);
      }
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not execute insert", SQLE);
    } catch (FileNotFoundException FNFE) {
      throw new ExecutionError("Could not find find file to populate table " + table, FNFE);
    } catch (IOException IOE) {
      throw new ExecutionError("Could not read file", IOE);
    } finally {
      try {
        connection.close();
        if (ps != null) {
          ps.close();
        }
        if (tableFile != null) {
          tableFile.close();
        }
      } catch (SQLException | IOException CE) {
        // Closing errors not considered important
      }
    }
  }
  
  private void populateTables(String fileName) throws ConnectionError, ExecutionError {
    Connection connection;
    
    // Connects to the database
    try {
      connection = DriverManager.getConnection(URL, userName, password);
    } catch (SQLException SQLE) {
      throw new ConnectionError("Could not connect to database", SQLE);
    }
    
    // Inits prepared statements
    PreparedStatement toMenu = null;
    PreparedStatement toAllergens = null;
    PreparedStatement toAllergensInItems = null;
    
    try {
      BufferedReader fileReader = loadFile(fileName);
      String line = fileReader.readLine();
      int numAllergens = 0;
      List<String> seenAllergens = new LinkedList<>();
      
      toMenu = null;
      
      int[] inOrder = new int[]{0, 1, 2, 4, 7, 3, 9};
      int[] allergens = new int[]{6};
      int[] vegGluten = new int[]{5};
      int stock = 8;
      String[] vegGlutenFlags = new String[]{"V", "VE", "GF"};
      
      Map<Integer, Integer> inputPosToOutputPos = Map.of(
          0, 0,
          1, 1,
          2, 2,
          3, 5,
          4, 3,
          7, 4,
          8, 9,
          9, 10);
      
      while (line != null) {
        
        if (line.charAt(0) != '!') {
          // This is a line with data
          String[] segments = line.split(",");
          
          // Gets the info for an item
          String[] itemInfo = new String[layouts.get("menu").length];
          for (int i: inputPosToOutputPos.keySet()) {
            try {
              itemInfo[inputPosToOutputPos.get(i)] = segments[i].strip();
            } catch (ArrayIndexOutOfBoundsException AIOOBE) {
              System.err.println("Num segments = "+segments.length);
              System.err.println("Attempted read index = "+i);
              System.err.println("Line = "+line);
              throw new ExecutionError("Could not load data into database " +
                  "(Not enough data found)", AIOOBE);
            }
          }
          
          // Adds allergens to the end of the list.
          for (int i = 0; i < vegGlutenFlags.length; i++) {
            if (segments[vegGluten[0]].contains(vegGlutenFlags[i])) {
              itemInfo[i + 6] = "true";
            } else {
              itemInfo[i + 6] = "false";
            }
          }
          
          if (toMenu == null) {
            toMenu = inputIntoTable("menu", itemInfo, connection);
          } else {
            inputIntoTable("menu", itemInfo, toMenu);
          }
          
          // Adds new allergens to the database
          String[] allergenList = null;
          for (int i = 0; i < allergens.length; i++) {
            allergenList = segments[allergens[0]].split(";");
          }
          
          for (int i = 0; i < allergenList.length; i++) { // Removes spare spaces
            allergenList[i] = allergenList[i].strip();
          }
          
          // Adds item to allergen mapping
          for (String allergen : allergenList) {
            if (!seenAllergens.contains(allergen) && !allergen.isEmpty()) {
              seenAllergens.add(allergen);
              String[] allergenInfo = new String[]{Integer.toString(numAllergens),
                  allergen.strip()};
              numAllergens++;
              if (toAllergens == null) {
                toAllergens = inputIntoTable("allergens", allergenInfo, connection);
              } else {
                inputIntoTable("allergens", allergenInfo, toAllergens);
              }
            }
          }
          
          for (String allergen : allergenList) {
            if (allergen.isEmpty()) {
              continue; // Skips empty allergesn
            }
            int allergenId = seenAllergens.indexOf(allergen);
            if (allergenId < 0) {
              System.out.println(allergen);
            }
            if (toAllergensInItems == null) {
              toAllergensInItems = inputIntoTable("allergensInItems",
                  new String[]{String.valueOf(allergenId), itemInfo[0]}, connection);
            } else {
              inputIntoTable("allergensInItems",
                  new String[]{String.valueOf(allergenId), itemInfo[0]}, toAllergensInItems);
            }
          }
        }
        
        line = fileReader.readLine();
      }
      if (toMenu != null) {
        executeInsert(toMenu);
        executeInsert(toAllergens);
        executeInsert(toAllergensInItems);
      }
    } catch (FileNotFoundException FNFE) {
      throw new ExecutionError("Could not find find file", FNFE);
    } catch (IOException IOE) {
      throw new ExecutionError("Could not read file", IOE);
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not execute insert", SQLE);
    } finally {
      try {
        toMenu.close();
        toAllergens.close();
        toAllergensInItems.close();
        closeConnection(connection);
      } catch (SQLException | NullPointerException CE) {
        // Closing errors not important
      }
    }
    
  }
  
  private List<Item> getItemsFromResultSet(ResultSet rs) throws ExecutionError, ConnectionError {
    List<Item> items = new LinkedList<>();
    Statement st = getStatement();
    try {
      while (rs.next()) {
        items.add(new Item(rs.getInt(1), rs.getString(2), rs.getString(3),
            rs.getInt(4), rs.getInt(5), ItemCategory.toCategory(rs.getString(6)),
            rs.getBoolean(7), rs.getBoolean(8), rs.getBoolean(9), rs.getInt(10),
            rs.getInt(11)));
      }
    } catch (SQLException SQLE) {
      throw new ExecutionError("Couldn't read next item", SQLE);
    }
    
    // Gets each items allergens
    for (Item item : items) {
      addAllergensToItem(st, item);
    }
    
    try {
      rs.close();
      Database.closeConnection(st.getConnection());
    } catch (SQLException SQLE) {
      // Not concerned with closing errors
    }
    
    return items;
  }
  
  private void addAllergensToItem(Statement st, Item item) throws ExecutionError {
    ResultSet rs;
    try {
      rs = st.executeQuery("SELECT allergens.allergenName FROM allergens, allergensinitems " +
          "WHERE allergens.allergenid = allergensinitems.allergenid " +
          "AND itemid = " + item.getID() + ";");
    } catch (SQLException SQLE) {
      throw new ExecutionError("Couldn't get allergens for item " + item.getName(), SQLE);
    }
    try {
      while (rs.next()) {
        item.addAllergen(rs.getString(1));
      }
    } catch (SQLException SQLE) {
      throw new ExecutionError("Couldn't read next allergen", SQLE);
    } finally {
      try {
        rs.close();
      } catch (SQLException SQLE) {
        // Not worried with closing issues.
      }
    }
  }
  
  private void dealWithItemError(ExecutionError EE, String[] data)
      throws ExecutionError, ConnectionError, InvalidItemException {
  
    // Used for checking which column caused the error
    Map<String, Integer> columnToData = new HashMap<>();
    columnToData.put("itemname", 1);
    columnToData.put("itemdesc", 2);
    columnToData.put("category", 5);
  
    // Gets the underlying error as it has the easiest message to deal with
    Throwable baseError = EE;
    while (baseError.getCause() != null) {
      baseError = baseError.getCause();
    }
  
    String baseMessage = baseError.getMessage(); // Gets the message to process
    
    if (baseMessage.contains("profit")){
      throw new InvalidItemException("Price too low", EE, "price", -1);
    }
    
    // Gets the string version of the input length
    String lengthStr = baseMessage.substring(baseMessage.lastIndexOf('(') + 1,
        baseMessage.lastIndexOf(')'));
  
    int length;
    try {
      length = Integer.parseInt(lengthStr);
    } catch (NumberFormatException NFE) {
      throw EE; // Cannot find the value to deal with
    }
  
    Statement st = getStatement();
    try {
      ResultSet rs = st.executeQuery("SELECT column_name FROM information_schema.columns" +
          " WHERE table_name = 'menu' " +
          "AND character_maximum_length = " + lengthStr);
      while (rs.next()) {
        String name = rs.getString(1);
        if (data[columnToData.get(rs.getString(1))].length() > length) {
          closeConnection(st.getConnection());
          throw new InvalidItemException("Too long input in column " + name, EE, name, length);
        }
      }
    
    } catch (SQLException SQLE) {
      throw EE; // Original error message will be useful than the error dealing with the error.
    } finally {
      try {
        closeConnection(st.getConnection());
      } catch (SQLException SQLE) {
        // Not worried with closing errors
      }
    }
  }
}
