package uk.ac.rhul.cs2810.containers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmployeeTableDataTest {

  private EmployeeTableData etd;
  
  @BeforeEach
  public void createData() {
    LocalDate dob = LocalDate.of(2000, 1, 8);
    LocalDate doh = LocalDate.of(2020, 1, 8);
    etd = new EmployeeTableData(1, "name", dob, doh, 0, 0);
  }
  
  @Test
  void testGetID() {
    assertEquals(1, etd.getID());
  }
  
  @Test
  void testGetName() {
    assertEquals("name", etd.getName());
  }
  
  @Test
  void testGetDateOB() {
    LocalDate dob = LocalDate.of(2000, 1, 8);
    assertEquals(dob, etd.getDateOB());
  }
  
  @Test
  void testGetDateOH() {
    LocalDate doh = LocalDate.of(2020, 1, 8);
    assertEquals(doh, etd.getDateOH());
  }
  
  @Test
  void testGetNumOrders() {
    assertEquals(0, etd.getNumOrders());
  }
  
  @Test
  void testGetHoursWorked() {
    assertEquals("0.0 Hours", etd.getHoursWorked());
  }

}
