package uk.ac.rhul.cs2810.database;

import org.junit.jupiter.api.*;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

@Tag("TableDB")
class TableDBTest extends DatabaseTest{
  
  @BeforeAll
  static void setTables() {
    tables = new String[] {"Tables"};
  }
  
  @BeforeEach
  void generateTableDB() throws ConnectionError, ExecutionError {
    tableDB = DatabaseFactory.getTestTableDB();
    testLogin = DatabaseFactory.getTestLoginDB();
    for (int waiterID: testLogin.getLoggedInWaiters()){
      testLogin.logOut(waiterID);
    }
  }
  
  @Test
  void testCreateTables() throws ConnectionError, ExecutionError, SQLException {
    Statement st = getStatement();
    assertTrue(Database.doTablesExist(st, tables));
    Database.closeConnection(st.getConnection());
  }
  
  @Test
  void testPopulateTableList() throws ConnectionError, SQLException, ExecutionError {
    Statement st = getStatement();
    ResultSet rs = st.executeQuery("SELECT count(*) FROM tables;");
    rs.next();
    if (rs.getInt(1) == 0) {
      fail("Table tables not populated");
    }
    rs.close();
    Database.closeConnection(st.getConnection());
  }
  
  @Test
  void testGetWaiterNull() throws ConnectionError, ExecutionError {
    assertEquals(-1, tableDB.getWaiter(1));
  }
  
  @Test
  void testGetDefaultClean() throws ConnectionError, ExecutionError {
    assertTrue(tableDB.getClean(1));
  }
  
  @Test
  void testSetGetClean() throws ConnectionError, ExecutionError {
    tableDB.setClean(2, false);
    assertFalse(tableDB.getClean(2));
  }
  
  @Test
  void testGetDefaultSeated() throws ConnectionError, ExecutionError {
    assertFalse(tableDB.getSeated(1));
  }
  
  @Test
  void testSetGetSeated() throws ConnectionError, ExecutionError {
    tableDB.setSeated(2, true);
    assertTrue(tableDB.getSeated(2));
  }
  
  @Test
  void testGetDefaultAlerted() throws ConnectionError, ExecutionError {
    assertFalse(tableDB.getWaiterAlerted(1));
  }
  
  @Test
  void testSetGetAlerted() throws ConnectionError, ExecutionError {
    tableDB.setWaiterAlerted(2, true);
    assertTrue(tableDB.getWaiterAlerted(2));
  }
  
  @Test
  void testTableDoesntExist() throws ConnectionError, ExecutionError {
    try {
      tableDB.getClean(-10);
    } catch (ExecutionError EE) {
      assertThat(EE.getMessage(), containsString("-10 not found"));
    }
  }
  
  @Test
  void testGetTableList() throws ConnectionError, ExecutionError {
    testLogin = DatabaseFactory.getTestLoginDB();
    int ID = testLogin.getID(LoginDB.hash(8149));
    tableDB.assignTable(1);
    tableDB.assignTable(2);
    tableDB.assignTable(3);
    assertEquals(3, tableDB.getAssignedTables(ID).size());
  }
  
  @Test
  void testGetMaxTableVal() throws ConnectionError, ExecutionError {
    assertEquals(12, tableDB.getMaxTableNum());
  }
  
  @Test
  void testGetNeededTables() throws ConnectionError, ExecutionError {
    testLogin = DatabaseFactory.getTestLoginDB();
    int ID = testLogin.getID(LoginDB.hash(8149));
    tableDB.assignTable(1);
    tableDB.assignTable(2);
    tableDB.assignTable(3);
    tableDB.setWaiterAlerted(1, true);
    tableDB.setWaiterAlerted(2, true);
    assertEquals(2, tableDB.getNeededTables(ID).size());
  }
  
  @Test
  void testAssignWaiter() throws ConnectionError, ExecutionError {
    testLogin = DatabaseFactory.getTestLoginDB();
    testLogin.getID(LoginDB.hash(8149));
    tableDB.assignTable(1);
    assertEquals(2, tableDB.getWaiter(1));
  }
  
  @Test
  void testAssignWaiterTwoWaiters() throws ConnectionError, ExecutionError {
    testLogin = DatabaseFactory.getTestLoginDB();
    testLogin.getID(LoginDB.hash(3435));
    testLogin.getID(LoginDB.hash(8149));
    tableDB.assignTable(1);
    assertEquals(1, tableDB.getWaiter(1));
  }
  
  @Test
  void testAssignWaiterDefaultLoggedIn() throws ConnectionError, ExecutionError {
    testLogin = DatabaseFactory.getTestLoginDB();
    testLogin.getID(LoginDB.hash(1234));
    testLogin.getID(LoginDB.hash(8149));
    tableDB.assignTable(1);
    assertEquals(2, tableDB.getWaiter(1));
  }
  
  @Test
  void testReassignTables() throws ConnectionError, ExecutionError {
    testLogin = DatabaseFactory.getTestLoginDB();
    testLogin.getID(LoginDB.hash(3435));
    testLogin.getID(LoginDB.hash(8149));
    tableDB.assignTable(1);
    testLogin.logOut(1);
    assertEquals(2, tableDB.getWaiter(1));
  }
  
  @Test
  void testAssignTableSetsTableAsSeated() throws ConnectionError, ExecutionError {
    testLogin = DatabaseFactory.getTestLoginDB();
    testLogin.getID(LoginDB.hash(8149));
    tableDB.assignTable(1);
    assertTrue(tableDB.getSeated(1));
  }
  
  @Test
  @Tag("Slow")
  void testAssignUnassignedTables() throws ConnectionError, ExecutionError, InterruptedException {
    testLogin = DatabaseFactory.getTestLoginDB();
    tableDB.assignTable(1);
    int ID = testLogin.getID(LoginDB.hash(8149));
    tableDB.assignUnassignedTables();
    Thread.sleep(33*1000);
    assertEquals(1, tableDB.getAssignedTables(ID).size());
  }
  
  @Test
  void testReassignTablesFromDefault() throws ConnectionError, ExecutionError {
    testLogin = DatabaseFactory.getTestLoginDB();
    tableDB.assignTable(1);
    int ID = testLogin.getID(LoginDB.hash(8149));
    tableDB.reassignTablesFromWaiter(-1);
    assertEquals(1, tableDB.getAssignedTables(ID).size());
  }
  
  @Test
  void testAssignInvalidTableTooHigh() {
    assertThrows(ExecutionError.class,() -> {tableDB.assignTable(tableDB.getMaxTableNum()+1);});
  }
  
  @Test
  void testAssignInvalidTableTooLow() {
    assertThrows(ExecutionError.class,() -> {tableDB.assignTable(0);});
  }
  
  @Test
  void assignDoesntAssignToManager() throws ConnectionError, ExecutionError {
    testLogin = DatabaseFactory.getTestLoginDB();
    testLogin.getID(LoginDB.hash(4578));
    int id = testLogin.getID(LoginDB.hash(8149));
    tableDB.assignTable(1);
    assertEquals(id, tableDB.getWaiter(1));
  }
}