package uk.ac.rhul.cs2810.containers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationTableDataTest {
  
  NotificationTableData ntd;
  
  @BeforeEach
  public void createNotificationTable() {
    ntd = new NotificationTableData(1, 12, "Customer Needs Help");
  }
  
  @Test
  void testGetID() {
    assertEquals(1, ntd.getID());
  }
  
  @Test
  void testGetTableNumber() {
    assertEquals(12, ntd.getTableNumber());
  }
  
  @Test
  void testGetRequest() {
    assertEquals("Customer Needs Help", ntd.getRequest());
  }

}
