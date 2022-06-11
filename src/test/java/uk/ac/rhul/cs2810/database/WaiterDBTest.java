package uk.ac.rhul.cs2810.database;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("WaiterDB")
class WaiterDBTest extends DatabaseTest {
  
  private WaiterDB waiterDB;
  
  @BeforeAll
  static void setup(){
    tables = new String[]{"WaiterData"};
  }
  
  @BeforeEach
  void getDatabase() throws ConnectionError, ExecutionError {
    waiterDB = DatabaseFactory.getTestWaiterDB();
  }
  
  @Test
  void testGetWaiterFromIDContainsDateOfBirth() throws ConnectionError, ExecutionError {
    assertEquals(LocalDate.of(2000, 10, 30), waiterDB.getWaiterFromID(2).getDateOfBirth());
  }
  
  @Test
  void testGetWaiterFromIDContainsDateHired() throws ConnectionError, ExecutionError {
    assertEquals(LocalDate.of(2020, 1, 30), waiterDB.getWaiterFromID(2).getDateOfHire()   );
  }
  
  @Test
  void testGetSetHalfHoursWorked() throws ConnectionError, ExecutionError {
    waiterDB.addHalfHoursWorked(1, 10);
    assertEquals(10, waiterDB.getHalfHoursWorked(1));
  }
  
  @Test
  void testAddOrderConfirmTime() throws ConnectionError, ExecutionError, SQLException {
    waiterDB.addOrderConfirmTime(1, 10);
    Statement st = getStatement();
    ResultSet rs = st.executeQuery("SELECT numordersconfirmed, totaltimeordersunconfirmed" +
        " FROM waiterdata  WHERE waiterid = 1");
    rs.next();
    assertEquals(1, rs.getInt(1));
    assertEquals(10, Time.valueOf(rs.getString(2)).toLocalTime().getMinute());
  }
  
  @Test
  void testGetAverageOrderConfirmTime() throws ConnectionError, ExecutionError {
    waiterDB.addOrderConfirmTime(1, 10);
    waiterDB.addOrderConfirmTime(1, 8);
    assertEquals(9f, waiterDB.getAverageConfirmTime(1));
  }
  
  @Test
  void testAddOrderServedTime() throws ConnectionError, ExecutionError, SQLException {
    waiterDB.addOrderServeTime(1, 10);
    Statement st = getStatement();
    ResultSet rs = st.executeQuery("SELECT numordersserved, totaltimeordersnotserved" +
        " FROM waiterdata  WHERE waiterid = 1");
    rs.next();
    assertEquals(1, rs.getInt(1));
    assertEquals(10, Time.valueOf(rs.getString(2)).toLocalTime().getMinute());
  }
  
  @Test
  void testGetAverageOrderServeTime() throws ConnectionError, ExecutionError {
    waiterDB.addOrderServeTime(1, 10);
    waiterDB.addOrderServeTime(1, 8);
    assertEquals(9f, waiterDB.getAverageServeTime(1));
  }
}