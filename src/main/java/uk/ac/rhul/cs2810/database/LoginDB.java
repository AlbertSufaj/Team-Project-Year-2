package uk.ac.rhul.cs2810.database;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.google.common.hash.Hashing;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;

import java.sql.*;
import java.time.Duration;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.round;

/**
 * The object for interacting with the login database.
 */
public class LoginDB extends Database {
  
  private TableDB tableDB;
  
  /**
   * Instantiates a new loginDB.
   *
   * @param testing should this connect to the test database
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError  if unable to setup the tables
   */
  LoginDB(boolean testing) throws ConnectionError, ExecutionError {
    super(new String[] {"WaiterLogin"}, testing);

    insertStatments.put("WaiterLogin", "INSERT INTO WaiterLogin VALUES (?, ?, ?);");
    layouts.put("WaiterLogin", new char[] {'i', 's', 's'});

    Statement st = null;
    try {
      st = getStatement();
      ResultSet rs = st.executeQuery("SELECT count(*) FROM waiterlogin;");
      rs.next();
      if (rs.getInt(1) <= 1) {
        populateTables();
      }
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not check state of waiterlogin table");
    } finally {
      try {
        if (st != null) {
          closeConnection(st.getConnection());
        }
      } catch (SQLException SQLE) {
        // Not concerned with closing errors
      }
    }
  }
  
  /**
   * Hashes a pin.
   *
   * @param i the pin to hash
   * @return the hashed pin
   */
  public static String hash(int i) { // Final used to stop overrides
    return Hashing.sha256().hashInt(i).toString();
  }
  
  /**
   * Gets the user id given the hashed password and logs them in.
   *
   * @param hash the hashed password
   * @return the user id (returns -1 if user not in database)
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError  if unable to access or update the database
   */
  public int getID(String hash) throws ConnectionError, ExecutionError {
    Connection connection = getConnection();
    int pin;
    try {
      PreparedStatement ps =
          connection.prepareStatement("SELECT waiterid FROM waiterlogin WHERE hashedpin = ?");
      ps.setString(1, hash);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        pin = rs.getInt(1);
        ps = connection.prepareStatement("UPDATE waiterlogin SET loggedIn = TRUE," +
            "  SignedInAt = ? WHERE waiterid = ? AND loggedin = FALSE");
        ps.setTime(1, Time.valueOf(LocalTime.now()));
        ps.setInt(2, pin);
        ps.execute();
      } else {
        pin = -1;
      }
    } catch (SQLException SQLE) {
      throw new ExecutionError("Couldn't fetch ID", SQLE);
    } finally {
      Database.closeConnection(connection);
    }

    return pin;
  }
  
  /**
   * Gets the user name from the id.
   *
   * @param id the users id
   * @return the users name
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError  if unable to access the database
   */
  public String getName(int id) throws ConnectionError, ExecutionError {
    Connection connection = getConnection();
    String username = "";

    try {
      PreparedStatement ps =
          connection.prepareStatement("SELECT waitername FROM waiterlogin WHERE waiterid = ?");
      ps.setInt(1, id);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        username = rs.getString(1);
      }
    } catch (SQLException SQLE) {
      throw new ExecutionError("Couldn't fetch pin", SQLE);
    } finally {
      Database.closeConnection(connection);
    }

    return username;
  }
  
  /**
   * Gets a list logged in waiters.
   * The list contains the management and kitchen as well
   *
   * @return the list of logged in waiters
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError  if unable to setup the tables
   */
  public List<Integer> getLoggedInWaiters() throws ConnectionError, ExecutionError {
    Statement st = getStatement();
    List<Integer> waiters = new LinkedList<>();
    try {
      ResultSet rs = st.executeQuery("SELECT waiterid FROM waiterlogin WHERE loggedIn = TRUE");
      while (rs.next()) {
        waiters.add(rs.getInt(1));
      }
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not get list of online waiters", SQLE);
    } finally {
      try {
        closeConnection(st.getConnection());
      } catch (SQLException SQlE) {
        // Not worried with closing errors
      }
    }
    
    return waiters;
  }
  
  /**
   * Logs out the waiter with the given ID.
   *
   * @param id the id of the waiter
   * @return the number of half hours worked in the last shift. Half hours are used as it is
   * neater then doing floats when the only fraction is a half
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError  if unable to update the database
   */
  public int logOut(int id) throws ConnectionError, ExecutionError {
    Connection connection = getConnection();
    if (testing) {
      tableDB = DatabaseFactory.getTestTableDB();
    } else {
      tableDB = DatabaseFactory.getTableDB();
    }
  
    PreparedStatement ps;
  
    try {
      ps = connection.prepareStatement("UPDATE waiterlogin SET loggedIn = FALSE" +
          " WHERE waiterid = ?");
      ps.setInt(1, id);
      ps.execute();
    } catch (SQLException SQLE) {
      closeConnection(connection);
      throw new ExecutionError("Could not update the database", SQLE);
    }
  
    int result;
    try {
      ps = connection.prepareStatement("SELECT SignedInAt FROM waiterlogin WHERE waiterid = ?");
      ps.setInt(1, id);
      ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
        throw new ExecutionError("Could not get hours worked");
      }
      Time signedInAt = rs.getTime(1);
      long minutesWorked = Duration.between(signedInAt.toLocalTime(), LocalTime.now()).toMinutes();
      result = abs(round((float) minutesWorked / 30f));
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not get hours worked", SQLE);
    } finally {
      closeConnection(connection);
    }
  
    // Has to be run after or the tables will be assigned back to them
    tableDB.reassignTablesFromWaiter(id);
  
    return result;
  }
  
  /**
   * Gets all staff in the system.
   *
   * @return a list of all the staff in the system
   * @throws ExecutionError  if unable to
   * @throws ConnectionError if unable to connect to the database

   */
  public List<Integer> getAllWaiters() throws ExecutionError, ConnectionError {
    Statement st = getStatement();
    List<Integer> waiters = new LinkedList<>();
    try {
      ResultSet rs = st.executeQuery("SELECT waiterid FROM waiterlogin WHERE waiterid>0");
      while (rs.next()) {
        waiters.add(rs.getInt(1));
      }
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not get list of online waiters");
    } finally {
      try {
        closeConnection(st.getConnection());
      } catch (SQLException SQlE) {
        // Not worried with closing errors
      }
    }
  
    return waiters;
  }
  
  private void populateTables() throws ConnectionError, ExecutionError {
    Connection connection = getConnection();
    String pin = hash(1234);
    try {
      PreparedStatement ps =
          connection.prepareStatement("INSERT INTO WaiterLogin VALUES (-1, 'Default', ?)");
      ps.setString(1, pin);
      ps.execute();
    } catch (SQLException SQLE) {
      if (!SQLE.getMessage().contains("duplicate")){
        throw new ExecutionError("Could not insert data into waiter login", SQLE);
      }
    }

    BufferedReader br;
    PreparedStatement waiters = null;
    try {
      br = loadFile("Waiters.txt");
    } catch (FileNotFoundException FNFE) {
      throw new ExecutionError("Could not load login info into database");
    }
    
    try {
      String line = br.readLine();
      while (line != null) {
        String[] segments = line.split(", ");
        String[] realSegs = new String[3];
        for (int i = 0; i<segments.length; i++){
          if (i<2){
            realSegs[i] = segments[i];
          } else if (i>2){
            realSegs[i-1] = segments[i];
          }
        }
        
        if (waiters == null) {
          waiters = inputIntoTable("WaiterLogin", realSegs, connection);
        } else {
          inputIntoTable("WaiterLogin", realSegs, waiters);
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
    }

    Database.closeConnection(connection);
  }

  @Override
  protected void makeTables() throws ConnectionError, ExecutionError {
    Statement st = getStatement();
    try {
      st.execute("CREATE TABLE IF NOT EXISTS WaiterLogin(" +
          "WaiterID int PRIMARY KEY," +
          "WaiterName varchar(20)," +
          "HashedPin varchar(64) UNIQUE," +
          "SignedInAt time DEFAULT null," +
          "LoggedIn boolean DEFAULT FALSE)");
    } catch (SQLException SQLE) {
      throw new ExecutionError("Could not create table WaiterLogin", SQLE);
    }

    try {
      Database.closeConnection(st.getConnection());
    } catch (SQLException SQLE) {
      // Not concerned with closing issues
    }
  }
}
