package uk.ac.rhul.cs2810.database;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("Database")
class DatabaseClassTest extends DatabaseTest {
  
  @BeforeAll
  static void setup(){
    tables = new String[]{};
  }
  
  @Test
  void inputIntoTableTableDoesntExist() throws ConnectionError, ExecutionError, SQLException {
    OrderDB orderDB = DatabaseFactory.getTestOrderDB();
    Connection connection = getStatement().getConnection();
    assertThrows(ExecutionError.class, () -> {
      orderDB.inputIntoTable("Test", new String[]{}, connection);
    });
    Database.closeConnection(connection);
  }
  
  @Test
  void testInputIntoTableInvalidInt() throws ConnectionError, ExecutionError, SQLException {
    OrderDB orderDB = DatabaseFactory.getTestOrderDB();
    Connection connection = getStatement().getConnection();
    assertThrows(ExecutionError.class, () -> {
      orderDB.inputIntoTable("orders", new String[]{"a", "a", String.valueOf(LocalTime.MAX)}, connection);
    });
    Database.closeConnection(connection);
  }
  
  @Test
  void testInputIntoTableInvalidTime() throws ConnectionError, ExecutionError, SQLException {
    OrderDB orderDB = DatabaseFactory.getTestOrderDB();
    Connection connection = getStatement().getConnection();
    assertThrows(ExecutionError.class, () -> {
      orderDB.inputIntoTable("orders", new String[]{"1", "1", "a"}, connection);
    });
    Database.closeConnection(connection);
  }
  
  @Test
  void testInputIntoTableInvalidBoolean() throws ConnectionError, ExecutionError, SQLException {
    MenuDB menuDB = DatabaseFactory.getTestMenu();
    Connection connection = getStatement().getConnection();
    assertThrows(ExecutionError.class, () -> {
      menuDB.inputIntoTable("orders", new String[]{"1", "1", "1", "1", "1", "1", "1", "1", "1", "1"}, connection);
    });
    Database.closeConnection(connection);
  }
  
  @Test
  void testInputIntoTableNotEnoughData() throws ConnectionError, ExecutionError, SQLException {
    MenuDB menuDB = DatabaseFactory.getTestMenu();
    Connection connection = getStatement().getConnection();
    try {
      menuDB.inputIntoTable("menu", new String[]{"1", "1", "1", "1", "1", "1", "true", "true", "true"}, connection);
    } catch (Exception e) {
      testExecutionError(e, "Input array too small");
    }
    Database.closeConnection(connection);
  }
  
  @Test
  void testInputIntoTableTooMuchData() throws ConnectionError, ExecutionError, SQLException {
    MenuDB menuDB = DatabaseFactory.getTestMenu();
    Connection connection = getStatement().getConnection();
    try {
      menuDB.inputIntoTable("menu", new String[]{"1", "1", "1", "1", "1", "1", "true", "true", "true", "1", "1"}, connection);
    } catch (Exception e) {
      testExecutionError(e, "Input array too large");
    }
    Database.closeConnection(connection);
  }
  
  @Test
  void testGetPollingRate() throws ConnectionError, ExecutionError {
    MenuDB menuDB = DatabaseFactory.getTestMenu();
    assertEquals(1000, menuDB.getPollingRate());
  }
}