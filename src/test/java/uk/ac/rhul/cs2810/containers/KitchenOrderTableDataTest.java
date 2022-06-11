package uk.ac.rhul.cs2810.containers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KitchenOrderTableDataTest {

  private KitchenOrderTableData kotd;

  @BeforeEach
  public void createData() {
    kotd = new KitchenOrderTableData(0, "20:00", OrderState.CANCELLED, "Items");
  }

  @Test
  void testGetID() {
    assertEquals(0, kotd.getId());
  }

  @Test
  void testGetTime() {
    assertEquals("20:00", kotd.getTime());
  }

  @Test
  void testGetStatus() {
    assertEquals(OrderState.CANCELLED, kotd.getStatus());
  }

  @Test
  void testGetItems() {
    assertEquals("Items", kotd.getItems());
  }

  @Test
  void testEquals() {
    KitchenOrderTableData newData =
        new KitchenOrderTableData(0, "20:00", OrderState.CANCELLED, "Items");
    assertTrue(kotd.equals(newData));
  }

}
