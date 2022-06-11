package uk.ac.rhul.cs2810.containers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderTableDataTest {
  
  OrderTableData otd;

  @BeforeEach
  public void createOrderTableData() {
    otd = new OrderTableData(1, 12, OrderState.UNCONFIRMED, "12:00:00");
  }
  
  @Test
  void testGetID() {
    assertEquals(1, otd.getID());
  }
  
  @Test
  void testGetTableNumber() {
    assertEquals(12, otd.getTableNumber());
  }
  
  @Test
  void testGetOrderState() {
    assertEquals(OrderState.UNCONFIRMED, otd.getOrderState());
  }
  
  @Test
  void testGetTimePlaced() {
    assertEquals("12:00:00", otd.getTimePlaced());
  }

}