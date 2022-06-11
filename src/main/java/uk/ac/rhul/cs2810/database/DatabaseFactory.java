package uk.ac.rhul.cs2810.database;

import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;

/**
 * Contains all the other objects to ensure only one instance exists.
 */
public class DatabaseFactory {
  
  private static MenuDB menuDB = null;
  private static OrderDB orderDB = null;
  private static MenuDB testMenu = null;
  private static OrderDB testOrder = null;
  private static LoginDB testLoginDB = null;
  private static LoginDB loginDB = null;
  private static TableDB testTable = null;
  private static TableDB tableDB = null;
  private static ImageDB testImageDB = null;
  private static ImageDB imageDB = null;
  private static WaiterDB testWaiterDB = null;
  private static WaiterDB waiterDB = null;
  
  private static int errorCount = 0;
  
  /**
   * Gets the object for interacting with the production menu DB.
   *
   * @return the menu database
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to setup the relevant tables
   */
  public static MenuDB getMenuDB() throws ConnectionError, ExecutionError {
    if (menuDB == null) {
      menuDB = new MenuDB(false);
    }
    return menuDB;
  }
  
  /**
   * Gets the object for interacting with the production order DB
   *
   * @return the order database
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to setup the relevant tables
   */
  public static OrderDB getOrderDB() throws ConnectionError, ExecutionError {
    if (orderDB == null) {
      getMenuDB();
      try {
        orderDB = new OrderDB(false);
      } catch (ExecutionError EE) {
        // Generates dependent tables
        
        if (EE.getCause().getMessage().contains("tables")) {
          getTestTableDB();
          return getOrderDB();
        } else {
          throw EE;
        }
      }
    }
    return orderDB;
  }
  
  /**
   * Gets the object for interacting with the production login DB
   *
   * @return the login database
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to setup the relevant tables
   */
  public static LoginDB getLoginDB() throws ConnectionError, ExecutionError {
    if (loginDB == null) {
      loginDB = new LoginDB(false);
    }
    return loginDB;
  }
  
  /**
   * Gets the menuDB object connected to the test DB.
   *
   * @return the test menu
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to setup the relevant tables
   */
  static MenuDB getTestMenu() throws ConnectionError, ExecutionError {
    // Is package private as only needed for testing
    if (testMenu == null) {
      testMenu = new MenuDB(true);
    }
    return testMenu;
  }
  
  /**
   * Gets the orderDB object connected to the test DB.
   *
   * @return the test order db
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to setup the relevant tables
   */
  static OrderDB getTestOrderDB() throws ConnectionError, ExecutionError {
    if (testOrder == null) {
      getTestMenu();
      try {
        testOrder = new OrderDB(true);
      } catch (ExecutionError EE) {
        // Generates dependent tables
        if (EE.getCause().getMessage().contains("tables")) {
          getTestTableDB();
          return getTestOrderDB();
        } else {
          throw EE;
        }
      }
    }
    return testOrder;
  }
  
  /**
   * Gets login db object connected to the test db.
   *
   * @return the test login db
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to setup the relevant tables
   */
  static LoginDB getTestLoginDB() throws ConnectionError, ExecutionError {
    if (testLoginDB == null) {
      testLoginDB = new LoginDB(true);
    }
    return testLoginDB;
  }
  
  /**
   * Gets the table db object connected to the test database.
   *
   * @return the test table db
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to setup the relevant tables
   */
  static TableDB getTestTableDB() throws ConnectionError, ExecutionError {
    if (testTable == null) {
      try {
        testTable = new TableDB(true);
      } catch (ExecutionError EE) {
        // Generates dependent tables
        if (EE.getCause().getMessage().contains("login")) {
          getTestLoginDB();
          return getTestTableDB();
        } else {
          throw EE;
        }
      }
    }
    return testTable;
  }
  
  /**
   * Gets the table db object connected to the production database.
   *
   * @return the table db
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to setup the relevant tables
   */
  public static TableDB getTableDB() throws ConnectionError, ExecutionError {
    if (tableDB == null) {
      try {
        tableDB = new TableDB(false);
      } catch (ExecutionError EE) {
        // Generates dependent tables
        if (EE.getCause().getMessage().contains("login")) {
          getTestLoginDB();
          return getTestTableDB();
        } else {
          throw EE;
        }
      }
    }
    return tableDB;
  }
  
  /**
   * Gets the image db object connected to the test database.
   *
   * @return the test image db
   * @throws ExecutionError if unable to setup the relevant tables
   * @throws ConnectionError if unable to connect to the database
   */
  static ImageDB getTestImageDB() throws ExecutionError, ConnectionError {
    if (testImageDB == null) {
      try {
        testImageDB = new ImageDB(true);
      } catch (ExecutionError EE) {
        if (EE.getCause().getMessage().contains("menu")) {
          getTestMenu();
          return getTestImageDB();
        } else {
          throw EE;
        }
        
      }
    }
    return testImageDB;
  }
  
  /**
   * Gets the image db object connected to the production database.
   *
   * @return the image db object
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to setup the relevant tables
   *
   */
  public static ImageDB getImageDB() throws ConnectionError, ExecutionError {
    if (imageDB == null) {
      try {
        imageDB = new ImageDB(false);
      } catch (ExecutionError EE) {
        Throwable cause = EE.getCause();
        if (cause == null || cause.getMessage() == null){
          throw EE;
        } else if (cause.getMessage().contains("menu") && errorCount < 10) {
          getMenuDB();
          errorCount++;
          return getImageDB();
        } else {
          throw EE;
        }
        
      }
    }
    return imageDB;
  }
  
  /**
   * Gets the waiter db object connected to the test database.
   *
   * @return the test waiter db
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to setup the relevant tables
   */
  static WaiterDB getTestWaiterDB() throws ConnectionError, ExecutionError {
    if (testWaiterDB == null){
      testWaiterDB = new WaiterDB(true);
    }
    return testWaiterDB;
  }
  
  /**
   * Gets the waiter db object connected to the production database.
   *
   * @return the waiter db
   * @throws ConnectionError if unable to connect to the database
   * @throws ExecutionError if unable to setup the relevant tables
   */
  public static WaiterDB getWaiterDB() throws ConnectionError, ExecutionError {
    if (waiterDB == null){
      waiterDB = new WaiterDB(false);
    }
    return waiterDB;
  }
  
  /**
   * Resets all the instances incase some external change happens.
   */
  static void reset() {
    menuDB = null;
    orderDB = null;
    testOrder = null;
    testMenu = null;
    testLoginDB = null;
    testTable = null;
    loginDB = null;
    testImageDB = null;
    imageDB = null;
    testWaiterDB = null;
    waiterDB = null;
  }
}
