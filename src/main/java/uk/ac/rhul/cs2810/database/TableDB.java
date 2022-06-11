package uk.ac.rhul.cs2810.database;

import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;

import static java.lang.Math.ceil;

/**
 * An object for interacting with the part of the database that deals with tables in the restaurant.
 */
public class TableDB extends Database {
  private int maxTableCount = -1;
  private final LoginDB loginDB;
  
  /**
   * Instantiates a new Table db.
   *
   * @param testing is this the testing version
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError  if unable to setup the database tables
   */
  TableDB(boolean testing) throws ConnectionError, ExecutionError {
    super(new String[]{"Tables"}, testing);
    
    layouts.put("Tables", new char[]{'i'});
    
    insertStatments.put("Tables", "INSERT INTO Tables (tableID) VALUES (?);");
    
    Statement st = getStatement();
    try {
      ResultSet rs = st.executeQuery("SELECT count(*) FROM tables;");
      rs.next();
      if (rs.getInt(1) == 0) {
        populateTables();
      }
    } catch (SQLException SQLE) {
      throw new ExecutionError("Couldn't count items in table tables", SQLE);
    }
    
    if (testing) {
      loginDB = DatabaseFactory.getTestLoginDB();
    } else {
      loginDB = DatabaseFactory.getLoginDB();
    }
    
    try {
      Database.closeConnection(st.getConnection());
    } catch (SQLException SQLE) {
      // Not worried with closing issues
    }
  }
  
  /**
   * Gets the id of the waiter assigned to a table.
   *
   * @param tableID the table id
   * @return the waiters id
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError  if unable to get the waiters id from the database
   */
  public int getWaiter(int tableID) throws ConnectionError, ExecutionError {
    Connection connection = getConnection();
    int waiterId;
    try {
      PreparedStatement ps = connection.prepareStatement("SELECT waiterid FROM tables" +
          " WHERE tableid = ?");
      ps.setInt(1, tableID);
      ResultSet rs = ps.executeQuery();
      rs.next();
      waiterId = rs.getInt(1);
    } catch (SQLException SQLE) {
      throw new ExecutionError("Couldn't get waiterId", SQLE);
    }
    try {
      connection.close();
    } catch (SQLException SQLE) {
      // Not concerned with closing issues
    }
    return waiterId;
  }
  
  /**
   * Sets the waiter assigned to the table.
   *
   * @param tableID  the table id
   * @param waiterID the waiter id
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError  the execution error
   */
  private void setWaiter(int tableID, int waiterID) throws ConnectionError, ExecutionError {
    if (tableID > getMaxTableNum() || tableID < 1){
      throw new ExecutionError("Table id not in database");
    }
    
    Connection connection = getConnection();
    
    try {
      PreparedStatement ps = connection.prepareStatement("UPDATE tables SET waiterid = ?" +
          " WHERE tableid = ?");
      ps.setInt(1, waiterID);
      ps.setInt(2, tableID);
      ps.execute();
    } catch (SQLException SQLE) {
      throw new ExecutionError("Can't set the waiterId", SQLE);
    }
    try {
      connection.close();
    } catch (SQLException SQLE) {
      // We're not worried with closing issues
    }
  }
  
  /**
   * Gets if the given table is clean.
   *
   * @param tableID the table id
   * @return is the table clean
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError  if unable to get data from the database
   */
  public boolean getClean(int tableID) throws ConnectionError, ExecutionError {
    return getColumn(tableID, "isClean");
  }
  
  /**
   * Updates the database's information on if a table is clean.
   *
   * @param tableID the table id
   * @param isClean is the table clean
   * @throws ExecutionError  if unable to update the database
   * @throws ConnectionError if unable to connect to the database
   */
  public void setClean(int tableID, boolean isClean) throws ExecutionError, ConnectionError {
    setColumn(tableID, isClean, "isClean");
  }
  
  /**
   * Gets if the given table is being used.
   *
   * @param tableID the table id
   * @return is the table used
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError  if unable to get the data from the database
   */
  public boolean getSeated(int tableID) throws ConnectionError, ExecutionError {
    return getColumn(tableID, "isSeated");
  }
  
  /**
   * Updates the database's information on if a table is being used.
   *
   * @param tableID  the table id
   * @param isSeated is the table being used
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError  if unable to update the database
   */
  public void setSeated(int tableID, boolean isSeated) throws ConnectionError, ExecutionError {
    setColumn(tableID, isSeated, "isSeated");
  }
  
  /**
   * Gets if a table has called for the waiter.
   *
   * @param tableID the table id
   * @return is the waiter called for
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError  if unable to fetch the data from the database
   */
  public boolean getWaiterAlerted(int tableID) throws ConnectionError, ExecutionError {
    return getColumn(tableID, "isAlerted");
  }
  
  /**
   * Sets if a table has called the waiter.
   *
   * @param tableId   the tables id
   * @param isAlerted has the table called waiter
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError  if unable to fetch the data from the database
   */
  public void setWaiterAlerted(int tableId, boolean isAlerted) throws ConnectionError,
      ExecutionError {
    setColumn(tableId, isAlerted, "isAlerted");
  }
  
  /**
   * Gets the tables assigned to a waiter.
   *
   * @param waiterID the waiters id
   * @return the list of assigned tables
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError  if unable to fetch the data from the database
   */
  public List<Integer> getAssignedTables(int waiterID) throws ConnectionError, ExecutionError {
    Connection connection = getConnection();
    List<Integer> tables = new LinkedList<>();
    try {
      PreparedStatement ps = connection.prepareStatement("SELECT tableid FROM tables" +
          " WHERE waiterid = ? AND isseated = TRUE");
      ps.setInt(1, waiterID);
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        tables.add(rs.getInt(1));
      }
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not get assigned tables", SQLE);
    }
    try {
      connection.close();
    } catch (SQLException SQLE) {
      // Not worried with closing issues
    }
    return tables;
  }
  
  /**
   * Gets all tables that need a given waiter.
   *
   * @param waiterID the waiters id
   * @return the list of tables that need the waiter
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError  if unable to get the list of tables from the database
   */
  public List<Integer> getNeededTables(int waiterID) throws ConnectionError, ExecutionError {
    Connection connection = getConnection();
    List<Integer> tables = new LinkedList<>();
    try {
      PreparedStatement ps = connection.prepareStatement("SELECT tableid FROM tables " +
          "WHERE waiterid = ? AND isAlerted = true");
      ps.setInt(1, waiterID);
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        tables.add(rs.getInt(1));
      }
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not get assigned tables", SQLE);
    }
    try {
      connection.close();
    } catch (SQLException SQLE) {
      // Not worried with closing issues
    }
    return tables;
  }
  
  /**
   * Gets the maximum table num.
   *
   * @return the max table num
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError  if unable to fetch the match value from the database
   */
  public int getMaxTableNum() throws ConnectionError, ExecutionError {
    int val;
    if (maxTableCount < 1) {
      Statement st = getStatement();
      try {
        ResultSet rs = st.executeQuery("SELECT MAX(tableid) FROM tables");
        rs.next();
        val = rs.getInt(1);
      } catch (SQLException SQLE) {
        throw new ExecutionError("Could not read from the database", SQLE);
      }
      try {
        Database.closeConnection(st.getConnection());
      } catch (SQLException SQLE) {
        // Not concerned with closing errors
      }
      maxTableCount = val;
    } else {
      val = maxTableCount;
    }
    return val;
  }
  
  /**
   * Assigns a waiter to the given table.
   * Assigns only logged in waiters so if no waiters are signed in a waiter will be assigned later
   *
   * @param tableID the table id
   * @throws ExecutionError  if a database error occurs when assigning the waiter
   * @throws ConnectionError if unable to connect to the database
   */
  public void assignTable(int tableID) throws ExecutionError, ConnectionError {
    List<Integer> waiters = loginDB.getLoggedInWaiters();
    for (int waiter: waiters) {
      if (waiter <= 0){
        waiters.remove(new Integer(waiter));
        // Have to leave the Integer call in to make the compiler treat it as an element
        // not an index
      }
    }
    if (waiters.size() == 0) {
      setWaiter(tableID, -1);
    } else {
      float blockSize = (float) getMaxTableNum() / (float) waiters.size();
      int block = (int) ceil(tableID / blockSize);
      setWaiter(tableID, waiters.get(block - 1));
    }
    setSeated(tableID, true);
  }
  
  /**
   * Reassigns tables from the given waiter.
   *
   * @param waiterID the waiter id
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError  if a database error occurs when reassigning the tables
   */
  public void reassignTablesFromWaiter(int waiterID) throws ConnectionError, ExecutionError {
    List<Integer> assignedTables = getAssignedTables(waiterID);
    for (int table: assignedTables) {
      setWaiter(table, -1);
      assignTable(table);
    }
  }
  
  /**
   * Assigns unassigned tables after a 30 second delay to let other waiters sign in.
   */
  public void assignUnassignedTables(){
    new Thread(() -> {
      try {
        Thread.sleep(30*1000);
        reassignTablesFromWaiter(-1);
      } catch (ConnectionError | ExecutionError | InterruptedException error) {
        error.printStackTrace();
      }
    }).start();
  }
  
  @Override
  protected void makeTables() throws ConnectionError, ExecutionError {
    Statement st = getStatement();
    try {
      st.execute("CREATE TABLE IF NOT EXISTS Tables(" +
          "TableID int PRIMARY KEY," +
          "WaiterID int DEFAULT -1," +
          "isClean bool DEFAULT TRUE," +
          "isSeated bool DEFAULT FALSE," +
          "isAlerted bool DEFAULT FALSE," +
          "FOREIGN KEY (WaiterID) REFERENCES waiterlogin(waiterid)" +
          ")");
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not make table tables", SQLE);
    } finally {
      try {
        Database.closeConnection(st.getConnection());
      } catch (SQLException SQLE) {
        // We're not worried with closing issues
      }
    }
  }
  
  private void populateTables() throws ConnectionError, ExecutionError {
    Connection connection = getConnection();
    PreparedStatement ps = null;
    try {
      for (int i = 1; i <= tableCount; i++) { // We want to index from 1 not 0 for user readability
        if (ps == null) {
          ps = inputIntoTable("Tables", new String[]{String.valueOf(i)}, connection);
        } else {
          inputIntoTable("Tables", new String[]{String.valueOf(i)}, ps);
        }
      }
      if (ps != null) {
        executeInsert(ps);
      }
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not fill table tables", SQLE);
    } finally {
      try {
        connection.close();
      } catch (SQLException SQLE) {
        // Not concerned with closing errors
      }
    }
  }
  
  private void setColumn(int tableID, boolean value, String columnName)
      throws ConnectionError, ExecutionError {
    Connection connection = getConnection();
    try {
      PreparedStatement ps = connection.prepareStatement("UPDATE tables SET " + columnName +
          " = ? WHERE tableid = ?");
      ps.setInt(2, tableID);
      ps.setBoolean(1, value);
      ps.execute();
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not update table data", SQLE);
    }
    try {
      connection.close();
    } catch (SQLException SQLE) {
      // Not concerned with closing issues
    }
  }
  
  private boolean getColumn(int tableID, String columnName) throws ConnectionError, ExecutionError {
    Connection connection = getConnection();
    boolean value;
    try {
      PreparedStatement ps = connection.prepareStatement("SELECT " + columnName +
          " FROM tables WHERE tableid = ?");
      ps.setInt(1, tableID);
      ResultSet rs = ps.executeQuery();
      rs.next();
      value = rs.getBoolean(1);
    } catch (SQLException SQLE) {
      if (SQLE.getMessage().contains("next")) {
        throw new ExecutionError("Table " + tableID + " not found", SQLE);
      } else {
        throw new ExecutionError("Could not read from database", SQLE);
      }
    }
    
    try {
      connection.close();
    } catch (SQLException SQLE) {
      // Not worried by closing issues
    }
    
    return value;
  }
  
}
