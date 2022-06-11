package uk.ac.rhul.cs2810.containers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StockTableDataTest {

  StockTableData std;
  
  @BeforeEach
  public void createData() {
    Price price = new Price(100);
    std = new StockTableData(1, "name", price, 0, (float) 0.0);
  }
  
  @Test
  void testGetID() {
    assertEquals(1, std.getID());
  }
  
  @Test
  void testGetName() {
    assertEquals("name", std.getName());
  }
  
  @Test
  void testGetPrice() {
    Price newPrice = new Price(100);
    assertEquals(newPrice, std.getPrice());
  }
  
  @Test
  void testGetStock() {
    assertEquals(0, std.getStock());
  }
  
  @Test
  void testGetAvgTime() {
    String time = String.format("%02d", 0) + ":" + String.format("%02d", 0) + " Minutes";
    assertEquals(time, std.getAvgTime());
  }

}
