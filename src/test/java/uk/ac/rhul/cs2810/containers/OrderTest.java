package uk.ac.rhul.cs2810.containers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTest {
  
  private Order blankOrder;
  private Order fullOrder;
  private Order basicOrder;
  private Item itemA;
  private Item itemB;
  private List<Item> items;
  
  @BeforeEach
  void createOrder() {
    items = new LinkedList<>();
    itemA = new Item(0, "name", "a", 0, 0, ItemCategory.BURRITOS, true,
        true, true, 10, 0);
    itemB = new Item(1, "name1", "b", 1, 1, ItemCategory.BURRITOS, false,
        false, false, 10, 0);
    items.add(itemA);
    items.add(itemB);
    blankOrder = new Order();
    basicOrder = new Order(0);
    fullOrder = new Order(0, items, 1, LocalTime.now(), LocalTime.now());
  }
  
  @Test
  void testBlankConstructor() {
    blankOrder = new Order();
  }
  
  @Test
  void testBasicConstructor() {
    basicOrder = new Order(0);
  }
  
  @Test
  @Tag("OrderDB")
  void testGetTime() {
    assertTrue(Duration.between(blankOrder.getTime(), LocalTime.now()).toMillis() < 5);
  }
  
  @Test
  void testGetState() {
    assertEquals(OrderState.UNCONFIRMED, blankOrder.getState());
  }
  
  @Test
  void testSetState() {
    blankOrder.setState(OrderState.PREPARING);
    assertEquals(OrderState.PREPARING, blankOrder.getState());
  }
  
  @Test
  void testAddSingleItemToOrder() {
    blankOrder.addItemToOrder(itemA);
    assertEquals(1, blankOrder.getItemsInOrder().size());
  }
  
  @Test
  void testAddManyItemsToOrder() {
    int count = 100;
    for (int i = 0; i < count; i++) {
      blankOrder.addItemToOrder(itemA);
    }
    assertEquals(count, blankOrder.getItemsInOrder().size());
  }
  
  @Test
  @Tag("OrderDB")
  void testGetItemsInOrderOneItem() {
    blankOrder.addItemToOrder(itemA);
    assertEquals(itemA, blankOrder.getItemsInOrder().get(0));
  }
  
  @Test
  @Tag("OrderDB")
  void testGetItemsInOrderTwoItems() {
    assertEquals(items, fullOrder.getItemsInOrder());
  }
  
  @Test
  void testRemoveItemFromOrderOnBlankList() {
    blankOrder.removeItemFromOrder(itemA);
  }
  
  @Test
  void testRemoveItemFromOrderSingleItem() {
    blankOrder.addItemToOrder(itemA);
    blankOrder.removeItemFromOrder(itemA);
  }
  
  @Test
  void testRemoveItemFromDuplicateItems() {
    blankOrder.addItemToOrder(itemA);
    blankOrder.addItemToOrder(itemA);
    blankOrder.removeItemFromOrder(itemA);
  }
  
  @Test
  void testRemoveItemFromOrderDifferentItems() {
    fullOrder.removeItemFromOrder(itemA);
  }
  
  @Test
  @Tag("OrderDB")
  void testSetID() {
    blankOrder.setID(1);
    assertEquals(1, blankOrder.getID());
  }
  
  @Test
  void testGetID() {
    assertEquals(-1, basicOrder.getID());
  }
  
  @Test
  void testGetPrice(){
    assertEquals(1, fullOrder.getPrice().getPriceValue());
  }
}
