package uk.ac.rhul.cs2810.database;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;

import java.sql.*;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.round;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;

@Tag("EmployeeDB")
class LoginDBTest extends DatabaseTest{
  
  @BeforeAll
  static void setTables() {
    tables = new String[] {"WaiterLogin"};
  }
  
  @BeforeEach
  void generateLoginDB() throws ConnectionError, ExecutionError {
    testLogin = DatabaseFactory.getTestLoginDB();
  }
  
  @Test
  void testCreation() throws ConnectionError, ExecutionError, SQLException {
    DatabaseFactory.getTestLoginDB();
  }
  
  @Test
  void testCreateTables() throws ConnectionError, ExecutionError, SQLException {
    assertTrue(Database.doTablesExist(st, new String[]{"WaiterLogin"}));
  }
  
  @Test
  void testDoubleCreation() throws ConnectionError, ExecutionError {
    DatabaseFactory.reset();
    DatabaseFactory.getTestLoginDB();
  }
  
  @Test
  void testLoadAdmin() throws ConnectionError, ExecutionError, SQLException {
    Statement st = getStatement();
    ResultSet rs = st.executeQuery("SELECT count(*) FROM WaiterLogin");
    rs.next();
    assertTrue(rs.getInt(1)>0);
  }
  
  @Test
  void testGetID() throws ConnectionError, ExecutionError {
    assertEquals(-1,testLogin.getID(LoginDB.hash(1234)));
  }
  
  @Test
  void testGetValidID() throws ConnectionError, ExecutionError {
    assertEquals(2,testLogin.getID(LoginDB.hash(8149)));
  }
  
  @Test
  void testGetIDNotInDatabase() throws ConnectionError, ExecutionError {
    assertEquals(-1,testLogin.getID(LoginDB.hash(0)));
  }
  
  @Test
  @Tag("Prod")
  void testGetIDProd() throws ConnectionError, ExecutionError, SQLException {
    Statement st = getStatement();
    st.execute("DROP TABLE IF EXISTS waiterlogin CASCADE");
    st.getConnection().close();
    DatabaseFactory.reset();
    testLogin = DatabaseFactory.getLoginDB();
    assertEquals(2, testLogin.getID(LoginDB.hash(8149)));
  }
  
  @Test
  void testGetName() throws ConnectionError, ExecutionError {
    assertEquals("Default", testLogin.getName(-1));
  }
  
  @Test
  void testHashReturnsSame() {
    assertEquals(LoginDB.hash(1), LoginDB.hash(1));
  }
  
  @Test
  void testLoginMarksUserAsLoggedin() throws ConnectionError, ExecutionError, SQLException {
    testLogin.getID(LoginDB.hash(8149));
    ResultSet rs = st.executeQuery("SELECT * FROM waiterlogin WHERE loggedIn = TRUE");
    if (!rs.next()) {
      fail("User Chris not marked as logged in");
    }
  }
  
  @Test
  void testLogOut() throws ConnectionError, ExecutionError, SQLException {
    int id = testLogin.getID(LoginDB.hash(8149));
    testLogin.logOut(id);
    ResultSet rs = st.executeQuery("SELECT * FROM waiterlogin WHERE loggedIn = TRUE");
    if (rs.next()) {
      fail("User Chris still marked as logged in");
    }
  }
  
  @Test
  @Tag("TableDB")
  void testGetLoggedInWaiters() throws ConnectionError, ExecutionError {
    testLogin.getID(LoginDB.hash(8149));
    assertEquals(1, testLogin.getLoggedInWaiters().size());
  }
  
  @Test
  void testLogOutReturnsHoursWorked() throws ConnectionError, SQLException, ExecutionError {
    Connection connection = getStatement().getConnection();
    PreparedStatement ps = connection.prepareStatement("UPDATE waiterlogin SET loggedIn = TRUE,  SignedInAt = ?" +
        "WHERE waiterid = 1 AND loggedin = FALSE");
    ps.setTime(1, Time.valueOf(LocalTime.of(0, 0)));
    ps.execute();
    long minutes = Duration.between(LocalTime.now(), LocalTime.of(0, 0)).toMinutes();
    assertTrue(abs(round((float) minutes/30f)) - testLogin.logOut(1) < 2);
    // Error bar to allow for test occuring across the second
  }
  
  @Test
  void testGetAllWaiters() throws ConnectionError, ExecutionError {
    List<Integer> waiters = testLogin.getAllWaiters();
    assertEquals(3, waiters.size());
    assertThat(waiters, not(contains(0)));
  }
  
  @Test
  @Tag("Prod")
  void testManagementLoginProd() throws ConnectionError, ExecutionError, SQLException {
    Statement st = getStatement();
    st.execute("DROP TABLE IF EXISTS waiterlogin CASCADE");
    st.getConnection().close();
    DatabaseFactory.reset();
    testLogin = DatabaseFactory.getLoginDB();
    assertEquals(-2, testLogin.getID(LoginDB.hash(4578)));
  }
}