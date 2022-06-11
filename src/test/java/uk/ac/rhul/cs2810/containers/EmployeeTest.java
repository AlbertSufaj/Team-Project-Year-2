package uk.ac.rhul.cs2810.containers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.sql.SQLException;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;
import uk.ac.rhul.cs2810.database.DatabaseTest;

@Tag("Employee")
class EmployeeTest {
  private Employee employeeA;
  
  @BeforeEach
  void makeUser() throws ConnectionError, ExecutionError {
    employeeA = new Employee(1, "test", LocalDate.of(2000, 12, 30),
        LocalDate.of(2000, 10, 30), 0, 1);
  }
  
  @Test
  void testGetId() {
    assertEquals(1, employeeA.getId());
  }
  
  @Test
  void testGetName() {
    assertEquals("test", employeeA.getName());
  }
  
  @Test
  void testGetDateOfBirth() {
    assertEquals(LocalDate.of(2000, 12, 30), employeeA.getDateOfBirth());
  }
  
  @Test
  void testGetDateOfHire() {
    assertEquals(LocalDate.of(2000, 10, 30), employeeA.getDateOfHire());
  }
  
  @Test
  void testGetNumOrdersAssigned() throws ConnectionError, ExecutionError {
    assertEquals(0, employeeA.getNumOrdersAssigned());
  }
  
  @Test
  void testGetHalfHoursWorked() {
    assertEquals(1, employeeA.getHalfHoursWorked());
  }
  
  @AfterAll
  static void clearDB() throws SQLException, ConnectionError, ExecutionError {
    DatabaseTest.clearTables();
  }
}