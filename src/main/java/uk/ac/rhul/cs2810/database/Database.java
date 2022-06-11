package uk.ac.rhul.cs2810.database;

import org.apache.commons.lang3.StringUtils;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Thread.sleep;

/**
 * A basic database class containing the common functionality
 * between the different database classes.
 */
abstract class Database {
  
  protected static String dataPath;
  protected final String URL;
  protected final String userName;
  protected final String password;
  protected final String format;
  protected final int tableCount;
  protected final Map<String, char[]> layouts = new HashMap<>();
  protected final Map<String, String> insertStatments = new HashMap<>();
  protected final String[] tables;
  protected final boolean testing;
  protected final int pollingRate;
  
  /**
   * Instantiates a new Database object.
   *
   * @param tables  the tables
   * @param testing the testing
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError  if cannot construct tables if needed
   */
  Database(String[] tables, boolean testing) throws ConnectionError, ExecutionError {
    this.testing = testing;
    String configPos = "";
    if (testing) {
      configPos = configPos + "\\src\\test\\resources\\";
    } else {
      configPos = configPos + "\\src\\main\\resources\\";
    }
    dataPath = configPos;
    configPos = configPos + "Config.txt";
    
    String[] connectionData;
    try {
      connectionData = getConfigInfo(configPos);
    } catch (ConnectionError CE) {
      connectionData = getConfigInfo("/Config.txt");
      dataPath = "/";
    }
    
    URL = connectionData[0];
    userName = connectionData[1];
    password = connectionData[2];
    format = connectionData[3];
    tableCount = Integer.parseInt(connectionData[4]);
    pollingRate = Integer.parseInt(connectionData[5]);
    
    this.tables = tables;
    
    if (!doTablesExist(getStatement())) {
      makeTables();
    }
  }
  
  /**
   * Checks if the given tables exist.
   *
   * @param st     the statement to connect to the DB
   * @param tables the tables to check for
   * @return do all the tables exist
   */
  static boolean doTablesExist(Statement st, String[] tables) {
    try {
      for (String table : tables) {
        st.execute("SELECT * FROM " + table + ";");
      }
    } catch (SQLException SQLE) {
      return false;
    }
    return true;
  }
  
  /**
   * Gets the config info from the config file.
   *
   * @param configFile the config file location
   * @return the configuration data
   * @throws ConnectionError if unable to connect to the database
   */
  static String[] getConfigInfo(String configFile) throws ConnectionError {
    BufferedReader config;
    String configPos = configFile;
    String[] data = new String[6];
    
    InputStreamReader inputStream;
    
    try {
      inputStream = new InputStreamReader(Database.class.getResourceAsStream(configFile),
          StandardCharsets.UTF_8);
      config = new BufferedReader(inputStream);
    } catch (NullPointerException NPE) {
      if (configFile.contains("test")) {
        try {
          config = new BufferedReader(new FileReader(configFile));
        } catch (FileNotFoundException FNFE) {
          throw new ConnectionError("Can't find config file at " + configFile, FNFE);
        }
      } else {
        throw new ConnectionError("Can't find config file at " + configFile, NPE);
      }
    }
    
    try {
      for (int i = 0; i < data.length; i++) {
        String line = config.readLine();
        String[] splitLine = line.split("="); // Splits into name and data
        String info = splitLine[1].strip().substring(1, splitLine[1].length() - 2);
        // Strips quotes
        
        // The if statements stop errors if the lines are rearranged
        if (splitLine[0].contains("URL")) {
          data[0] = info;
        } else if (splitLine[0].contains("UserName")) {
          data[1] = info;
        } else if (splitLine[0].contains("Password")) {
          data[2] = info;
        } else if (splitLine[0].contains("Format")) {
          data[3] = info;
        } else if (splitLine[0].contains("Table")) {
          data[4] = info;
        } else if (splitLine[0].contains("Polling")) {
          data[5] = info;
        }
      }
    } catch (IOException IOE) {
      throw new ConnectionError("Cannot get connection info", IOE);
    }
    
    try {
      Connection connection = DriverManager.getConnection(data[0], data[1], data[2]);
      // Have to use DriverManager as getConnection hides the errors that are being filtered for
      connection.close();
    } catch (SQLException SQLE) {
      if (SQLE.getMessage().contains("password")) {
        throw new ConnectionError("Username and password invalid", SQLE);
      } else {
        throw new ConnectionError("Cannot connect to database", SQLE);
      }
    }
    
    return data;
  }
  
  /**
   * Gets a statement connected to the given database.
   *
   * @param URL      the url
   * @param username the username
   * @param password the password
   * @return the statement
   * @throws ConnectionError if unable to connect to the database
   */
  protected static Statement getStatement(String URL, String username, String password)
      throws ConnectionError {
    Connection connection = getConnection(URL, username, password);
    Statement st;
    try {
      st = connection.createStatement();
    } catch (SQLException SQLE) {
      throw new ConnectionError("Couldn't make a new statement", SQLE);
    }
    return st;
  }
  
  /**
   * Gets a connection to the given database.
   *
   * @param URL      the url of the database
   * @param username the username to login with
   * @param password the password to login with
   * @return the connection object
   * @throws ConnectionError if unable to connect to the database
   */
  protected static Connection getConnection(String URL, String username, String password)
      throws ConnectionError {
    Connection connection = null;
    int failiureCount = 0; // Counts the number of failed attempts
    //System output is for debugging which tests are not having all connections closed.
    while (connection == null) {
      try {
        connection = DriverManager.getConnection(URL, username, password);
      } catch (SQLException SQLE) {
        if (failiureCount > 10) {
          if (SQLE.getMessage().contains("too many clients")) {
            throw new ConnectionError("Too many clients", SQLE);
          } else {
            throw new ConnectionError("Could not connect to database", SQLE);
          }
        } else {
          try {
            sleep(40); // Retrys after a delay that
          } catch (InterruptedException IE) {
            // Not an issue for the user
          }
        }
        failiureCount++;
      }
    }
    
    return connection;
  }
  
  /**
   * Closes the given connection.
   *
   * @param connection the connection to close
   */
  protected static void closeConnection(Connection connection) {
    try {
      connection.close();
    } catch (SQLException SQLE) {
      System.err.println("Failed to close connection");
    }
  }
  
  /**
   * Gets the database polling rate.
   * Gives a useful base reference point for how often the check if the database has updated.
   *
   * @return the polling rate
   */
  public int getPollingRate() {
    return pollingRate;
  }
  
  /**
   * Checks if the relevant tables exist.
   *
   * @param st the statement to connect to the DB
   * @return do all the tables exist
   */
  boolean doTablesExist(Statement st) {
    return doTablesExist(st, tables);
  }
  
  /**
   * Gets a statement connected to the database.
   *
   * @return the statement
   * @throws ConnectionError if unable to connect to the database
   */
  protected Statement getStatement() throws ConnectionError {
    return getStatement(URL, userName, password);
  }
  
  /**
   * Gets a connection to the database.
   *
   * @return the connection
   * @throws ConnectionError if unable to connect to the database
   */
  protected Connection getConnection() throws ConnectionError {
    return getConnection(URL, userName, password);
  }
  
  /**
   * Makes the tables represented by the object.
   *
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError  if theres an error creating the tables on the database end
   */
  protected abstract void makeTables() throws ConnectionError, ExecutionError;
  
  /**
   * Inputs a given data list into the specified table.
   * The table must be represented by the database object and have insertStatements and layout set.
   * <p>
   * The inputs should all be string values of the intended value so they can be parsed back to
   * there original types except for files where the file path should specified.
   *
   * @param table      the table to insert into
   * @param inputs     the inputs to enter
   * @param connection the connection to the database
   * @return a prepared statement including the insert.
   * @throws ExecutionError if cannot insert data into the table
   * @throws SQLException   Any sql exceptions that need more info to meaningfully handle
   */
  protected PreparedStatement inputIntoTable(String table, String[] inputs, Connection connection)
      throws ExecutionError, SQLException {
    PreparedStatement ps;
    try {
      String insertStatment = insertStatments.get(table);
      ps = connection.prepareStatement(insertStatment);
    } catch (SQLException SQLE) {
      throw new ExecutionError("Couldn't make prepared statement", SQLE);
    } catch (NullPointerException NPE) {
      if (!insertStatments.containsKey(table)) {
        throw new ExecutionError("Table " + table
            + " doesn't have a defined insert statement\n" +
            "Defined insert statements are : " + insertStatments.keySet(), NPE);
      } else {
        throw NPE;
      }
    }
    inputIntoTable(table, inputs, ps);
    return ps;
  }
  
  /**
   * Inputs a given data list into the specified table.
   * The table must be represented by the database object and have insertStatements and layout set.
   * <p>
   * The inputs should all be string values of the intended value so they can be parsed back to
   * there original types except for files where the file path should specified.
   *
   * @param table  the table to insert into
   * @param inputs the inputs to enter
   * @param ps     a prepared statement including the insert.
   * @throws SQLException   Any sql exceptions which need more context info to enter
   * @throws ExecutionError if theres an issue inputting the data into the database
   */
  protected void inputIntoTable(String table, String[] inputs, PreparedStatement ps)
      throws SQLException, ExecutionError {
    char[] layout = layouts.get(table);
    if (inputs.length > layout.length) {
      throw new ExecutionError("Input array too large for insert statment");
    } else if (inputs.length < layout.length) {
      throw new ExecutionError("Input array too small for insert statment");
    }
    inputValuesIntoPreparedStatement(inputs, layout, ps);
    ps.addBatch();
  }
  
  /**
   * Executes an insert prepared statement.
   *
   * @param ps the prepared statement to execute
   * @throws ExecutionError if theres an issue inserting the data into the database
   */
  protected void executeInsert(PreparedStatement ps) throws ExecutionError {
    try {
      ps.executeBatch();
    } catch (SQLException SQLE) {
      String message = SQLE.getMessage();
      if (message.contains("duplicate")) {
        throw new ExecutionError("Input contains duplicate key", SQLE);
      } else if (message.contains("tableid")) {
        throw new ExecutionError("Table num " + StringUtils.substringBefore(
            StringUtils.substringAfter(message, "tableid)=("), ")")
            + " is not valid", SQLE);
      } else {
        throw new ExecutionError("Could not insert data into table", SQLE);
      }
    }
  }
  
  /**
   * Input values into the prepared statement.
   * <p>
   * The inputs should all be string values of the intended value so they can be parsed back to
   * there original types except for files where the file path should specified.
   * <p>
   * The char to type mapping is:
   * i - int, s - string, b - boolean, t - time, B - Blob (file), d - date
   *
   * @param inputs the inputs to add to the prepared statement
   * @param layout the layout of the inputs (What type they are)
   * @param ps     the prepared statement with the insert statement
   * @throws ExecutionError if theres an issue inserting the data into the database
   */
  protected void inputValuesIntoPreparedStatement(String[] inputs, char[] layout,
                                                  PreparedStatement ps) throws ExecutionError {
    for (int i = 0; i < layout.length; i++) {
      try {
        switch (layout[i]) {
          case 'i':
            try {
              ps.setInt(i + 1, Integer.parseInt(inputs[i].strip()));
            } catch (NumberFormatException NFE) {
              throw new ExecutionError("Could not insert item " + (i + 1) + " as "
                  + inputs[i].strip()
                  + " is not a valid int", NFE);
            }
            break;
          case 's':
            ps.setString(i + 1, inputs[i].strip());
            break;
          case 'b':
            ps.setBoolean(i + 1, Boolean.parseBoolean(inputs[i].strip()));
            break;
          case 't':
            String input = inputs[i].strip();
            Time time;
            try {
              time = Time.valueOf(input);
            } catch (IllegalArgumentException IAE) {
              throw new ExecutionError("Could not insert item " + i + " as "
                  + inputs[i].strip()
                  + " is not a valid time", IAE);
            }
            ps.setTime(i + 1, time);
            break;
          case 'B':
            try {
              InputStream fileStream = getFileAsStream(inputs[i]);
              if (fileStream == null) {
                throw new ExecutionError("Cannot find file " + inputs[i],
                    new FileNotFoundException("Cannot find file " + inputs[i]));
              }
              ps.setBinaryStream(i + 1, fileStream);
            } catch (FileNotFoundException FNFE) {
              throw new ExecutionError("File " + inputs[i] + " not found", FNFE);
            }
            break;
          case 'd':
            if (inputs[i].equalsIgnoreCase("null")) {
              ps.setDate(i + 1, null);
            } else {
              ps.setDate(i + 1, Date.valueOf(LocalDate.parse(inputs[i])));
            }
            break;
        }
      } catch (SQLException SQLE) {
        throw new ExecutionError("Could not add input " + inputs[i].strip() + " with format "
            + layout[i] + " into prepared statment at position " + i);
      }
    }
  }
  
  
  /**
   * Gets a buffered reader of the given file.
   *
   * @param fileName the files name
   * @return the buffered reader
   * @throws FileNotFoundException if unable to find the file
   */
  protected BufferedReader loadFile(String fileName) throws FileNotFoundException {
    return new BufferedReader(new InputStreamReader(getFileAsStream(fileName),
        StandardCharsets.UTF_8));
  }
  
  /**
   * Gets an input stream representing the given file.
   *
   * @param fileName the files name
   * @return the stream
   * @throws FileNotFoundException if unable to find the file
   */
  protected InputStream getFileAsStream(String fileName) throws FileNotFoundException {
    InputStream stream;
    try {
      stream = getClass().getResourceAsStream(dataPath + fileName);
    } catch (NullPointerException NPE) {
      try {
        stream = getClass().getResourceAsStream(dataPath.substring(1) + fileName);
        dataPath = dataPath.substring(1);
      } catch (NullPointerException npe) {
        try {
          stream = getClass().getResourceAsStream("/" + fileName);
          dataPath = "/";
        } catch (NullPointerException NullPointer) {
          throw new FileNotFoundException("Could not find file at " + dataPath + fileName);
        }
      }
    }
    return stream;
  }
}
