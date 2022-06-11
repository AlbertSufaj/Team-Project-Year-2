package uk.ac.rhul.cs2810.database;

import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;
import uk.ac.rhul.cs2810.containers.Employee;

import java.io.*;
import java.sql.*;

/**
 * The object for interacting with the waiter data in the database
 */
public class WaiterDB extends Database {
  
  /**
   * Instantiates a new Database.
   *
   * @param testing is this for testing
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to setup the tables
   */
  WaiterDB(boolean testing) throws ConnectionError, ExecutionError {
    super(new String[]{"WaiterData"}, testing);
    
    insertStatments.put("WaiterData", "INSERT INTO WaiterData Values (?, ?, ?, ?)");
    layouts.put("WaiterData", new char[]{'i', 's', 'd', 'd'});
    
    try {
      getWaiterFromID(1); // Checks the data has been populated
    } catch (ExecutionError EE){
      populateTables();
    }
  }
  
  /**
   * Gets waiter object from id.
   *
   * @param id the waiters id
   * @return the object representing the waiter
   * @throws ExecutionError if unable to get the waiter data from the database
   * @throws ConnectionError if unable to connect to the database
   */
  public Employee getWaiterFromID(int id) throws ExecutionError, ConnectionError {
    Connection connection = getConnection();
    Employee waiter;
    try {
      PreparedStatement ps = connection.prepareStatement("SELECT waitername, dateOfBirth," +
          " dateHired, halfHoursWorked " +
          "FROM waiterData " +
          "WHERE waiterID = ?");
      
      ps.setInt(1, id);
      ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
        closeConnection(connection);
        throw new ExecutionError("Waiter with id " + id + " not found");
      }
      waiter = new Employee(id, rs.getString(1), rs.getDate(2).toLocalDate(),
          rs.getDate(3).toLocalDate(),0, rs.getInt(4));
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not get waiter info", SQLE);
    } finally {
      closeConnection(connection);
    }
    
    return waiter;
  }
  
  /**
   * Added a shift to the time worked (measured in half hours).
   *
   * @param id              the waiters ID
   * @param halfHoursWorked the number of half hours worked
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to update the stats in the database
   */
  public void addHalfHoursWorked(int id, int halfHoursWorked)
      throws ConnectionError, ExecutionError {
    
    Connection connection = getConnection();
    
    try {
      PreparedStatement ps = connection.prepareStatement("UPDATE waiterData " +
          "SET HalfHoursWorked = HalfHoursWorked + ? " +
          "WHERE waiterID = ?");
      ps.setInt(1, halfHoursWorked);
      ps.setInt(2, id);
      ps.execute();
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not update the time worked", SQLE);
    } finally {
      closeConnection(connection);
    }
  }
  
  /**
   * Gets the number of half hours worked by the waiter in the last time period.
   *
   * @param id the waiters ID
   * @return the number of half hours worked
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError is unable to fetch the data from the database
   */
  public int getHalfHoursWorked(int id) throws ConnectionError, ExecutionError {
    Connection connection = getConnection();
    int result;
    try {
      PreparedStatement ps = connection.prepareStatement("SELECT HalfHoursWorked" +
          " FROM waiterData WHERE waiterId = ?");
      ps.setInt(1, id);
      ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
        throw new ExecutionError("Could not find waiter info");
      }
      result = rs.getInt(1);
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not access waiter info", SQLE);
    } finally {
      closeConnection(connection);
    }
    return result;
    
  }
  
  /**
   * Add time to confirm an order to the database.
   *
   * @param id            the waiters ID
   * @param minutesPassed the number of minutes taken to confirm the order
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to update the stats
   */
  public void addOrderConfirmTime(int id, int minutesPassed) throws ConnectionError, ExecutionError {
    Connection connection = getConnection();
    try {
      PreparedStatement ps = connection.prepareStatement("UPDATE waiterData " +
          "SET numordersconfirmed = numordersconfirmed+1," +
          " totaltimeordersunconfirmed = totaltimeordersunconfirmed + ?::INTERVAL " +
          "WHERE waiterid = ?");
      ps.setString(1, minutesPassed + " minutes");
      ps.setInt(2, id); // Casting causes the code analysis to believe there's an error
      // when there's not
      ps.execute();
    } catch (SQLException SQLE) {
      throw new ExecutionError("Couldn't add stats to database", SQLE);
    } finally {
      closeConnection(connection);
    }
  }
  
  /**
   * Gets average time the given waiter takes to confirm an order.
   *
   * @param id the waiters ID
   * @return the average time to confirm an order in minutes
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to fetch the data from the database
   */
  public float getAverageConfirmTime(int id) throws ConnectionError, ExecutionError {
    Connection connection = getConnection();
    int numOrders;
    int timeUnconfirmed;
    try {
      PreparedStatement ps = connection.prepareStatement("SELECT numordersconfirmed," +
          " totaltimeordersunconfirmed FROM waiterdata WHERE waiterid = ?");
      ps.setInt(1, id);
      ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
        throw new ExecutionError("Could not find data for waiter with ID " + id);
      }
      numOrders = rs.getInt(1);
      timeUnconfirmed = rs.getTime(2).toLocalTime().getMinute();
      
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not get the data", SQLE);
    } finally {
      closeConnection(connection);
    }
    return (float) timeUnconfirmed / (float) numOrders;
  }
  
  /**
   * Add the order serve time to the waiters stats.
   *
   * @param id           the waiters id
   * @param minutesTaken the minutes taken
   * @throws ExecutionError if unable to update the stats
   * @throws ConnectionError if unable to connect to the database
   */
  public void addOrderServeTime(int id, int minutesTaken) throws ExecutionError, ConnectionError {
    Connection connection = getConnection();
    try {
      PreparedStatement ps = connection.prepareStatement("UPDATE waiterData " +
          "SET numordersserved = numordersserved+1," +
          " totaltimeordersnotserved = totaltimeordersnotserved + ?::INTERVAL WHERE waiterid = ?");
      ps.setString(1, minutesTaken + " minutes");
      ps.setInt(2, id);
      ps.execute();
    } catch (SQLException SQLE) {
      throw new ExecutionError("Couldn't add stats to database", SQLE);
    } finally {
      closeConnection(connection);
    }
  }
  
  /**
   * Gets average time the given waiter takes to confirm an order.
   *
   * @param id the waiters ID
   * @return the average time to confirm an order
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to fetch the stats from the database
   */
  public float getAverageServeTime(int id) throws ConnectionError, ExecutionError {
    Connection connection = getConnection();
    int numOrders;
    int timeUnserved;
    try {
      PreparedStatement ps = connection.prepareStatement("SELECT numordersserved," +
          " totaltimeordersnotserved FROM waiterdata WHERE waiterid = ?");
      ps.setInt(1, id);
      ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
        throw new ExecutionError("Could not find data for waiter with ID " + id);
      }
      numOrders = rs.getInt(1);
      timeUnserved = rs.getTime(2).toLocalTime().getMinute();
      
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not get the data", SQLE);
    } finally {
      closeConnection(connection);
    }
    return (float) timeUnserved / (float) numOrders;
  }
  
  private void populateTables() throws ExecutionError, ConnectionError {
    BufferedReader br;
    PreparedStatement waiters = null;
  
    try {
      br = loadFile("WaiterData.txt");
    } catch (FileNotFoundException FNFE) {
      throw new ExecutionError("Could not load waiter data into database");
    }
    
    Connection connection = null;
    try {
      connection = getConnection();
      
      String line = br.readLine();
      while (line != null) {
        String[] segments = line.split(", ");
        
        if (waiters == null) {
          waiters = inputIntoTable("WaiterData", segments, connection);
        } else {
          inputIntoTable("WaiterData", segments, waiters);
        }
        line = br.readLine();
      }
      if (waiters != null) {
        executeInsert(waiters);
      }
    } catch (IOException IOE) {
      throw new ExecutionError("Could not read file", IOE);
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not insert into table", SQLE);
    } finally {
      if (connection != null) {
        Database.closeConnection(connection);
      }
    }
  }
  
  @Override
  protected void makeTables() throws ConnectionError, ExecutionError {
    Statement st = getStatement();
    try {
      st.execute("CREATE TABLE IF NOT EXISTS WaiterData(" +
          "WaiterID int PRIMARY KEY," +
          "WaiterName varchar(20) not null," +
          "DateOfBirth DATE," +
          "DateHired DATE," +
          "HalfHoursWorked int default 0 not null," +
          "NumOrdersConfirmed int default 0 not null, " +
          "TotalTimeOrdersUnconfirmed INTERVAL MINUTE default '0' not null, " +
          "NumOrdersServed int default 0 not null, " +
          "TotalTimeOrdersNotServed INTERVAL MINUTE default '0' not null, " +
          "FOREIGN KEY (WaiterID) REFERENCES waiterlogin(waiterid)" +
          ")");
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not create data login", SQLE);
    }
    try {
      closeConnection(st.getConnection());
    } catch (SQLException SQLE) {
      // Not concerned with closing issues
    }
  }
}
