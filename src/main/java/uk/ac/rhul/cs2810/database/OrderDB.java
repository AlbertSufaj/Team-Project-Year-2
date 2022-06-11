package uk.ac.rhul.cs2810.database;

import org.postgresql.util.PSQLException;
import uk.ac.rhul.cs2810.Exceptions.*;
import uk.ac.rhul.cs2810.containers.Item;
import uk.ac.rhul.cs2810.containers.Order;
import uk.ac.rhul.cs2810.containers.OrderState;

import java.sql.*;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

import static java.lang.Math.abs;

/**
 * An object for abstracting interaction with the order parts of the database.
 */
public class OrderDB extends Database {
  private final boolean testing;
  
  private final Map<OrderState, List<Order>> orderCache;
  private final Map<OrderState, LocalTime> timeFetched;
  
  /**
   * Initiates a new Order db.
   *
   * @param testing should this connect to the test database
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError  if unable to setup the tables
   */
  OrderDB(boolean testing) throws ConnectionError, ExecutionError {
    super(new String[]{"orders", "itemsInOrder"}, testing);
    
    timeFetched = new HashMap<>();
    orderCache = new HashMap<>();
    
    insertStatments.put("orders", "INSERT INTO orders VALUES (?, ?, ?);");
    insertStatments.put("itemsInOrder", "INSERT INTO itemsInOrder VALUES (?, ?, ?);");
    
    layouts.put("orders", new char[]{'i', 'i', 't'});
    layouts.put("itemsInOrder", new char[]{'i', 'i', 'i'});
    
    this.testing = testing;
  }
  
  /**
   * Adds an order to the database. Gets attributes of the order, then cycles through the items.
   * Adding each item to the order database.
   *
   * @param order the order to be added to the database
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError  if unable a database error occurs when adding the order
   * @throws MissingDataError if the order doesn't contain any items
   * @throws OrderAlreadyExistsException if an order with the same ID is already in the database
   * (consider using modifyOrder)
   */
  public void addOrder(Order order)
      throws ConnectionError, ExecutionError, MissingDataError, OrderAlreadyExistsException {
    
    // Checks the order has items as an empty order is worthless
    if (order.getItemCount() == 0) {
      throw new MissingDataError("Order must contain items");
    }
    
    Connection connection = Database.getConnection(URL, userName, password);
    Statement st = getStatement();
    
    // Gets important info from the order
    String id = String.valueOf(order.getID());
    String tableNumber = String.valueOf(order.getTableNumber());
    String timeOfOrder = String.valueOf(Time.valueOf(LocalTime.now()));
    List<Item> items = order.getItemsInOrder();
    
    // Prepared statements allow faster execution and provide more security
    PreparedStatement intoOrder;
    PreparedStatement intoItemsInOrders = null;
    
    if (Integer.parseInt(id) < 0) { // Generates ID if needed
      try {
        ResultSet rs = st.executeQuery("SELECT MAX(orderid) FROM orders");
        rs.next();
        id = String.valueOf(rs.getInt(1) + 1);
        rs.close();
      } catch (SQLException SQLE) {
        throw new ExecutionError("Could not assign ID", SQLE);
      }
    }
    
    order.setID(Integer.parseInt(id));
    
    try {
      intoOrder = inputIntoTable("orders", new String[]{id, tableNumber, timeOfOrder},
          connection);
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not insert order into table", SQLE);
    }
    
    // Adds all the items into the database
    Map<Item, Integer> occuranceMap = order.getOccuranceMap();
    for (Item item : occuranceMap.keySet()) {
      
      String itemCount = String.valueOf(occuranceMap.get(item));
      
      try {
        if (intoItemsInOrders == null) {
          intoItemsInOrders = inputIntoTable("itemsInOrder",
              new String[]{id, String.valueOf(item.getID()), itemCount}, connection);
        } else {
          inputIntoTable("itemsInOrder", new String[]{id, String.valueOf(item.getID()),
                  itemCount},
              intoItemsInOrders);
        }
      } catch (SQLException SQLE) {
        throw new ExecutionError("Could not insert item into table", SQLE);
      }
    }
    
    // Adds all the data to the database
    try {
      executeInsert(intoOrder);
    } catch (ExecutionError EE) {
      Throwable baseException = EE;
      while (baseException.getCause() != null) {
        baseException = baseException.getCause();
      }
      if (baseException.getClass() != PSQLException.class) {
        throw EE; // Not the error we're looking for
      }
      if (baseException.getMessage().toLowerCase().contains("orderid")) {
        throw new OrderAlreadyExistsException("Cannot add an order with ID " + order.getID()
            + " as order with that ID already exists", order.getID());
      }
    }
    if (intoItemsInOrders != null) {
      executeInsert(intoItemsInOrders);
    }
    
    try {
      setOrderState(order, order.getState());
    } catch (OrderNotFoundException ONFE) {
      throw new ExecutionError("Order unsuccessfully added with ID " + ONFE.getOrderId());
    }
    
    try {
      Database.closeConnection(st.getConnection());
      Database.closeConnection(connection);
      intoOrder.close();
      intoItemsInOrders.close();
    } catch (SQLException SQLE) {
      // Errors in closing are not a major issue
    }
  }
  
  
  /**
   * Modifies an order in the database
   *
   * @param order the new version of the order. Must have the same id as the order to replace
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if a database error occurs when modifying the item
   * @throws OrderNotFoundException if the order to modify isn't in the database
   */
  public void modifyOrder(Order order)
      throws ConnectionError, ExecutionError, OrderNotFoundException {
    int ID = order.getID();
    Order oldOrder = getOrderFromID(ID);
    
    if (oldOrder == null) { // Can't modify an order that doesn't exist
      throw new OrderNotFoundException("Order with id " + ID + " not found", order.getID());
    }
    
    Connection connection = getConnection();
    
    try { // Updates the table number
      if (order.getTableNumber() != oldOrder.getTableNumber()) {
        PreparedStatement ps =
            connection.prepareStatement("UPDATE orders SET tableid = ? WHERE orderid = ?");
        ps.setInt(1, order.getTableNumber());
        ps.setInt(2, ID);
        ps.execute();
      }
    } catch (SQLException SQLE) {
      closeConnection(connection);
      throw new ExecutionError("Could not change table number", SQLE);
    }
    
    try {
      if (!order.getItemsInOrder().equals(oldOrder.getItemsInOrder())) {
        PreparedStatement removeItems = connection
            .prepareStatement("DELETE FROM itemsinorder WHERE orderid = ?" + " AND itemid = ?");
        PreparedStatement changeItemCount = connection.prepareStatement(
            "UPDATE itemsinorder SET numinorder = ? " + "WHERE orderid = ? AND itemid = ?");
        PreparedStatement addItems = null;
        
        Map<Item, Integer> newOrderItemOccuranceMap = order.getOccuranceMap();
        Map<Item, Integer> oldOrderItemOccuranceMap = oldOrder.getOccuranceMap();
        
        Set<Item> newOrderItems = newOrderItemOccuranceMap.keySet();
        Set<Item> oldOrderItems = oldOrderItemOccuranceMap.keySet();
        
        for (Item newItem : newOrderItems) {
          int occurances = newOrderItemOccuranceMap.get(newItem);
          
          if (!oldOrderItems.contains(newItem)) { // Adds the item to the addItems prepared
            // statement
            if (addItems == null) {
              addItems = inputIntoTable("itemsInOrder",
                  new String[]{String.valueOf(order.getID()), String.valueOf(newItem.getID()),
                      String.valueOf(occurances)}, connection);
            } else {
              inputIntoTable("itemsInOrder", new String[]{String.valueOf(order.getID()),
                  String.valueOf(newItem.getID()), String.valueOf(occurances)}, addItems);
            }
            
          } else if (oldOrderItemOccuranceMap.get(newItem) != occurances) { // Updates item counts
            changeItemCount.setInt(1, occurances);
            changeItemCount.setInt(2, ID);
            changeItemCount.setInt(3, newItem.getID());
            changeItemCount.addBatch();
          }
        }
        
        for (Item oldItem : oldOrderItems) {
          if (!newOrderItems.contains(oldItem)) {
            removeItems.setInt(1, ID);
            removeItems.setInt(2, oldItem.getID());
            removeItems.addBatch();
          }
        }
        
        if (addItems != null) {
          executeInsert(addItems);
        }
        changeItemCount.executeBatch();
        removeItems.executeBatch();
      }
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not change the items in the order", SQLE);
    } finally {
      closeConnection(connection);
    }
    
    
  }
  
  /**
   * Gets a list of all orders currently in the database.
   *
   * @return the list of orders
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to fetch the orders
   */
  public List<Order> getOrders() throws ConnectionError, ExecutionError {
    // Use null as state for caching
    return getOrdersUsingQuery("SELECT * FROM orders", new String[0], new char[0]);
  }
  
  /**
   * Gets the orders state. Updates the original object as well as returning the state
   *
   * @param order the order to get the state of
   * @return the order state
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if a database error occurs when getting the orders state
   * @throws OrderNotFoundException if the order is not in the database
   */
  public OrderState getOrderState(Order order)
      throws ConnectionError, ExecutionError, OrderNotFoundException {
    Order newOrder = getOrderFromID(order.getID());
    
    if (newOrder == null) {
      throw new OrderNotFoundException("Cannot get the status for an order that doesn't exist",
          order.getID());
    } else {
      order.setState(newOrder.getState());
      return newOrder.getState();
    }
  }
  
  /**
   * Marks the orders as confirmed in the database.
   *
   * @param order the order to confirm
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to update the database
   * @throws OrderNotFoundException if the order is not in the database
   */
  public void confirmOrder(Order order)
      throws ConnectionError, ExecutionError, OrderNotFoundException {
    Connection connection;
    connection = getConnection();
    MenuDB menuDB;
    if (testing) {
      menuDB = DatabaseFactory.getTestMenu();
    } else {
      menuDB = DatabaseFactory.getMenuDB();
    }
    menuDB.decreaseStock(order);
    setOrderState(order, OrderState.CONFIRMED);
    order.setState(OrderState.CONFIRMED);
    Database.closeConnection(connection);
  }
  
  /**
   * Removes order from database.
   * If the order is already not in the database it does nothing
   *
   * @param order the order to remove
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to remove the order
   */
  public void removeOrder(Order order) throws ConnectionError, ExecutionError {
    Connection connection = getConnection();
    try {
      PreparedStatement ps = connection.prepareStatement("DELETE FROM orders WHERE orderid = ?");
      ps.setInt(1, order.getID());
      ps.execute();
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not remove order.", SQLE);
    }
    
    closeConnection(connection);
  }
  
  /**
   * Clears the order table.
   *
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to remove the orders
   */
  public void clearOrders() throws ConnectionError, ExecutionError {
    Statement st = getStatement();
    try {
      st.executeUpdate("TRUNCATE orders");
      st.executeUpdate("TRUNCATE itemsinorder");
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not clear order table.", SQLE);
    }
    try {
      closeConnection(st.getConnection());
    } catch (SQLException SQLE) {
      // Not worried with closing errors
    }
  }
  
  /**
   * Sets state of an order.
   *
   * @param order      the order in question
   * @param orderState the state it will be set to
   * @return How many minutes the order was in its old state
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to update the database
   * @throws OrderNotFoundException the order not found exception
   */
  public int setOrderState(Order order, OrderState orderState)
      throws ConnectionError, ExecutionError, OrderNotFoundException {
    Connection connection = getConnection();
    PreparedStatement ps;
    
    Order updatedOrder = getOrderFromID(order.getID());
    if (updatedOrder == null) {
      throw new OrderNotFoundException("Cannot set the state of an order not in the database",
          order.getID());
    }
    
    try {
      String statusCode = getCodeFromState(orderState);
      if (statusCode.equals("canc")) {
        removeOrder(order);
        return -1;
      }
      ps = connection.prepareStatement(
          "UPDATE orders SET status = ?," + " timeStatusChanged = ? where orderid = ?");
      ps.setString(1, statusCode);
      ps.setTime(2, Time.valueOf(LocalTime.now()));
      ps.setInt(3, order.getID());
      ps.execute();
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not change state.", SQLE);
    }
    closeConnection(connection);
    order.setState(orderState);
    int timeTaken =
        (int) Duration.between(LocalTime.now(), order.getTimeStatusChanged()).toMinutes();
    order.setTimeStatusChanged(LocalTime.now());
    return abs(timeTaken);
  }
  
  /**
   * Gets orders assigned to a waiter.
   *
   * @param waiterID the waiter id
   * @return the orders assigned to the waiter
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if a database error occurs when getting the orders
   */
  public List<Order> getOrdersAssignedToWaiter(int waiterID)
      throws ConnectionError, ExecutionError {
    
    List<Order> orders = getOrdersUsingQuery(
        "SELECT * FROM orders, tables "
            + "WHERE orders.tableid = tables.tableid AND tables.waiterid = ? ORDER BY timeadded",
        new String[]{String.valueOf(waiterID)}, new char[]{'i'});
    
    return orders;
  }
  
  /**
   * Gets an order from its id.
   *
   * @param id the orders id
   * @return the order - null if not present
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to get the order from the database
   */
  public Order getOrderFromID(int id) throws ConnectionError, ExecutionError {
    
    List<Order> orders = getOrdersUsingQuery("SELECT * FROM orders WHERE orderid = ?",
        new String[]{String.valueOf(id)}, new char[]{'i'});
    
    if (orders.size() != 1) {
      return null;
    }
    
    return orders.get(0);
  }
  
  /**
   * Gets orders orders by the given table.
   *
   * @param tableID the table id
   * @return the orders from table
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if a database error occurs when getting the orders
   */
  public List<Order> getOrdersFromTable(int tableID) throws ConnectionError, ExecutionError {
    return getOrdersUsingQuery("SELECT * FROM orders WHERE tableID = ?",
        new String[]{String.valueOf(tableID)}, new char[]{'i'});
  }
  
  /**
   * Gets whether an order has been payed for.
   *
   * @param order the order to check
   * @return if the order has been payed for
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to get the data from the database
   * @throws OrderNotFoundException if the order is not in the database
   */
  public boolean getPayed(Order order)
      throws ConnectionError, ExecutionError, OrderNotFoundException {
    Connection connection = getConnection();
    boolean result;
    
    try {
      PreparedStatement ps =
          connection.prepareStatement("SELECT payed from orders where orderid = ?");
      ps.setInt(1, order.getID());
      ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
        closeConnection(connection);
        throw new OrderNotFoundException("Cannot find order with ID", order.getID());
      }
      result = rs.getBoolean(1);
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not get order status", SQLE);
    } finally {
      closeConnection(connection);
    }
    
    return result;
  }
  
  /**
   * Stores whether or not an order has been payed for.
   *
   * @param order the order
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to update the database
   * @throws OrderNotFoundException if the order is not in the database
   */
  public void setOrderPayed(Order order)
      throws ConnectionError, ExecutionError, OrderNotFoundException {
    Connection connection = getConnection();
    if (getOrderFromID(order.getID()) == null) {
      throw new OrderNotFoundException("Cannot pay for an order not in the database",
          order.getID());
    }
    try {
      PreparedStatement ps =
          connection.prepareStatement("UPDATE orders SET payed = true WHERE orderid = ?");
      ps.setInt(1, order.getID());
      ps.execute();
    } catch (SQLException SQLE) {
      throw new ExecutionError("Couldn't set order as payed", SQLE);
    } finally {
      closeConnection(connection);
    }
  }
  
  /**
   * Gets orders assigned to the given waiter with the given state.
   *
   * @param state    the state
   * @param waiterID the waiters id - Use id = 0 to get orders assigned to anyone
   * @return the orders assigned to waiter with the given state
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to fetch the list of orders from the database
   */
  public List<Order> getOrdersAssignedToWaiterFromState(OrderState state, int waiterID)
      throws ConnectionError, ExecutionError {
    
    LocalTime lastFetched = LocalTime.MIN;
    
    if (timeFetched.containsKey(state)) {
      lastFetched = timeFetched.get(state);
    }
    
    List<Order> cached = orderCache.getOrDefault(state, new LinkedList<>());
    // Used to get the size and to stop reloading the same data
    
    List<Order> orders;
    if (waiterID <= 0) {
      orders = getOrdersFromState(state);
    } else {
      
      orders = getOrdersUsingQuery(
          "SELECT orders.OrderID, orders.tableID, orders.TimeAdded, orders.status," +
              " orders.timeStatusChanged " // Data to get
              + "FROM orders, tables " + // Need tables to get assigned waiter
              "WHERE orders.status = ? AND" // Only with correct status
              // Only get if theres changes
              + " ( (SELECT max(timeStatusChanged) FROM orders WHERE status = ?) >= ? " +
              "OR (SELECT count(*) FROM orders WHERE status = ?) != ? )" +
              "AND tables.tableID = orders.tableID AND tables.waiterID = ?", // checks waiter
          // waiter is assigned
          new String[]{getCodeFromState(state),
              getCodeFromState(state), String.valueOf(Time.valueOf(lastFetched)),
              getCodeFromState(state), String.valueOf(cached.size()),
              String.valueOf(waiterID)},
          new char[]{'s', 's', 't', 's', 'i', 'i'}); // Input format
    }
    
    if (orders.size() == 0) { // If no results need to check if there's nothing new or if data has
      // been removed
      Connection connection = getConnection();
      
      try {
        // Counts how many item with that state
        PreparedStatement ps = connection.prepareStatement("SELECT count(*)"
            + " FROM orders WHERE status = ?");
        ps.setString(1, getCodeFromState(state));
        ResultSet rs = ps.executeQuery();
        
        rs.next();
        int size = rs.getInt(1);
        if (size == 0) {
          orders = new LinkedList<>(); // If theres no items with the state return an empty list
        } else { // All items are known
          orders = cached;
        }
        
      } catch (SQLException SQLE) {
        throw new ExecutionError("Could not get state of cache");
      } finally {
        closeConnection(connection);
      }
    }
    
    if (orders == null) {
      orders = new LinkedList<>();
    }
    
    timeFetched.put(state, LocalTime.now()); // Adds data to the cache
    orderCache.put(state, orders);
    
    return orders;
  }
  
  /**
   * Gets all orders with a given state.
   *
   * @param state the state
   * @return the order with that state
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to get the list of orders
   */
  private List<Order> getOrdersFromState(OrderState state) throws ConnectionError, ExecutionError {
    
    LocalTime lastFetched = LocalTime.MIN;
    
    if (timeFetched.containsKey(state)) {
      lastFetched = timeFetched.get(state);
    }
    
    List<Order> orders = getOrdersUsingQuery("SELECT * FROM orders WHERE status = ? AND"
            + " ( (SELECT max(timeStatusChanged) FROM orders WHERE status = ?) >= ? " +
            " OR (SELECT count(*) FROM orders WHERE status = ?) != ? )",
        new String[]{getCodeFromState(state),
            getCodeFromState(state), String.valueOf(Time.valueOf(lastFetched)),
            getCodeFromState(state), String.valueOf(orderCache.getOrDefault(state,
            new LinkedList<>()).size())},
        new char[]{'s', 's', 't', 's', 'i'}); // The format of the inputs
    
    return orders;
  }
  
  @Override
  protected void makeTables() throws ConnectionError, ExecutionError {
    
    Statement st = getStatement();
    
    try {
      st.execute("CREATE TABLE IF NOT EXISTS orders(" //
          + "OrderID int PRIMARY KEY," //
          + "TableID int," //
          + "TimeAdded time," //
          + "Status varchar(4) DEFAULT 'ucnf'," //
          + "TimeStatusChanged time DEFAULT '00:00:00'," //
          + "Payed bool DEFAULT false," //
          + "FOREIGN KEY (TableID) REFERENCES tables(tableid)" + ");");
    } catch (SQLException SQLE) {
      throw new ExecutionError("Couldn't make table orders", SQLE);
    }
    
    try {
      st.execute("CREATE TABLE IF NOT EXISTS itemsInOrder(" //
          + "OrderID int," //
          + "ItemID int," //
          + "NumInOrder int DEFAULT 1," //
          + "PRIMARY KEY (OrderID, ItemID)," //
          + "FOREIGN KEY (ItemID) REFERENCES menu(ItemID)" + ");");
    } catch (SQLException SQLE) {
      throw new ExecutionError("Couldn't make table itemsInOrder", SQLE);
    }
    
    try {
      Database.closeConnection(st.getConnection());
    } catch (SQLException SQLE) {
      // Closing errors not considered major
    }
  }
  
  private List<Order> getOrdersUsingQuery(String query, String[] inputs, char[] format)
      throws ConnectionError, ExecutionError {
    Connection connection = getConnection();
    
    if (!query.toLowerCase().contains("select ")) { // Ensures the key parts of the
      // query are present
      query = "SELECT * FROM orders " + query.strip();
    }
    if (!query.toLowerCase().contains("order by timeadded")) {
      query = query.strip() + " ORDER BY timeadded ";
    }
    
    ResultSet rs;
    try {
      PreparedStatement ps = connection.prepareStatement(query);
      inputValuesIntoPreparedStatement(inputs, format, ps);
      
      rs = ps.executeQuery();
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not get data from database", SQLE);
    }
    
    List<Order> orders = getOrdersFromResultSet(rs);
    closeConnection(connection);
    
    return orders;
  }
  
  private List<Order> getOrdersFromResultSet(ResultSet orderResult)
      throws ConnectionError, ExecutionError {
    List<Order> orders = new LinkedList<>();
    MenuDB menuDB;
    Map<Integer, Item> menuMap;
    
    if (testing) {
      menuDB = DatabaseFactory.getTestMenu();
    } else {
      menuDB = DatabaseFactory.getMenuDB();
    }
    
    menuMap = menuDB.getMap();
    
    ResultSet itemResult = null;
    Statement st = getStatement();
    
    try {
      while (orderResult.next()) { // Goes through all the orders
        int id = orderResult.getInt(1);
        int tableID = orderResult.getInt(2);
        LocalTime timeAdded = orderResult.getTime(3).toLocalTime();
        String status = orderResult.getString(4);
        LocalTime timeStatusChanged = orderResult.getTime(5).toLocalTime();
        
        OrderState state = null;
        
        // Converts database info into OrderState
        switch (status) {
          case "conf":
            state = OrderState.CONFIRMED;
            break;
          case "prep":
            state = OrderState.PREPARING;
            break;
          case "srvd": // Served
            state = OrderState.SERVED;
            break;
          case "redy":
            state = OrderState.READY;
            break;
          default:
            state = OrderState.UNCONFIRMED;
        }
        
        // Gets all the items in the order
        
        // Prepared statements reduce the chance of errors damaging the database
        PreparedStatement ps = st.getConnection()
            .prepareStatement("SELECT itemid, numinorder FROM itemsinorder WHERE orderid = ?");
        ps.setInt(1, id);
        itemResult = ps.executeQuery();
        ps.close();
        
        // Adds the items to the order
        List<Item> items = new LinkedList<>();
        
        while (itemResult.next()) {
          int itemID = itemResult.getInt(1);
          int numInOrder = itemResult.getInt(2);
          for (int i = 0; i < numInOrder; i++) {
            items.add(menuMap.get(itemID));
          }
        }
        
        // Creates the order object
        Order order = new Order(id, items, tableID, timeAdded, timeStatusChanged);
        order.setState(state);
        order.setTimeStatusChanged(LocalTime.now());
        orders.add(order);
      }
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not read data from database", SQLE);
    }
    
    try {
      if (itemResult != null) {
        itemResult.close();
      }
      st.getConnection().close();
    } catch (SQLException SQLE) {
      // Not concerned with closing errors
    }
    
    return orders;
  }
  
  private String getCodeFromState(OrderState state) {
    switch (state) {
      case UNCONFIRMED:
        return "ucnf";
      case CONFIRMED:
        return "conf";
      case PREPARING:
        return "prep";
      case READY:
        return "redy";
      case SERVED:
        return "srvd";
      case CANCELLED:
        return "canc";
      default:
        throw new IllegalArgumentException("State " + state + " does not have a defined code");
    }
  }
}
