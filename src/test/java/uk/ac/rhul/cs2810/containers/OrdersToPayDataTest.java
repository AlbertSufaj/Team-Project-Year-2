package uk.ac.rhul.cs2810.containers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrdersToPayDataTest {
  
  private OrdersToPayData otpd;
  
  @BeforeEach
  public void createData() {
    Price price = new Price(100);
    otpd = new OrdersToPayData(0, 0, price);
  }
  
  @Test
  void testGetID() {
    assertEquals(0, otpd.getID());
  }
  
  @Test
  void testGetTableNumber() {
    assertEquals(0, otpd.getTableNumber());
  }
  
  @Test
  void testGetPrice() {
    Price newPrice = new Price(100);
    assertEquals(newPrice, otpd.getPrice());
  }
  

}
