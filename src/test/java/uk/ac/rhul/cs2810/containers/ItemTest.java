package uk.ac.rhul.cs2810.containers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("MenuDB")
class ItemTest {

  Item itemA;
  Item itemB;

  Item item;

  @BeforeEach
  void makeItem() {
    itemA = new Item(0, "name", "a", 0, 0, ItemCategory.BURRITOS, true, true, true, 1, 0);
    itemB = new Item(1, "name2", "ab", 1, 1, ItemCategory.ENCHILADAS, false, false, false, 0, 0);
    item = new Item(0, "name", "description", 112, 1002, ItemCategory.BURRITOS, true, false, false, 1, 80);
  }

  @Test
  void testGetID() {
    assertEquals(0, itemA.getID());
  }

  @Test
  void testGetDifferentID() {
    assertEquals(1, itemB.getID());
  }

  @Test
  void testGetName() {
    assertEquals("name", itemA.getName());
  }

  @Test
  void testGetDifferenceName() {
    assertEquals("name2", itemB.getName());
  }

  @Test
  void testAddAllergen() {
    itemA.addAllergen("allergen");
  }

  @Test
  void testCreation() {
    itemA = new Item(0, "", "", 0, 0, ItemCategory.BURRITOS, false, false,
        true, 1, 0);
  }

  @Test
  public void testPriceSplit() {
    Price p = new Price(112);
    assertEquals("£   1.12", p.toString());
  }

  @Test
  public void testCalFormat() {
    assertEquals("Calories: 1002 kcal", item.caloriesFormat());
  }

  @Test
  public void testAllergenFormat() {
    item.addAllergen("Gluten");
    item.addAllergen("Dairy");

    assertEquals("Allergens: Gluten, Dairy", item.allergenFormat());
  }

  @Test
  public void testToString() {
    String name = String.format("%-20s", "name");
    String description = String.format("%-100s", "description");
    String price = String.format("%s%4d.%02d", "£", 1, 12);

    assertEquals(
        name + "\t" + price + "\n" + description + "\nCalories: 1002 kcal\nAllergens: None",
        item.toString());
  }

  @Test
  void testGetCategory() {
    assertEquals(ItemCategory.BURRITOS, itemA.getCategory());
  }

  @Test
  void testGetDifferentCategory() {
    assertEquals(ItemCategory.ENCHILADAS, itemB.getCategory());
  }

  @Test
  void testGetVegi() {
    assertTrue(itemA.isVegi());
  }

  @Test
  void testGetDifferentVegi() {
    assertFalse(itemB.isVegi());
  }

  @Test
  void testGetVegan() {
    assertTrue(itemA.isVegan());
  }

  @Test
  void testGetDifferentVegan() {
    assertFalse(itemB.isVegan());
  }

  @Test
  void testGetGlutenFree() {
    assertTrue(itemA.isGlutenFree());
  }

  @Test
  void testGetDifferentGlutenFree() {
    assertFalse(itemB.isGlutenFree());
  }

  @Test
  void testCatNotNull() {
    assertNotEquals(null, itemA.getCategory());
  }
  
  @Test
  void testGetStock(){
    assertEquals(1, itemA.getStock());
  }
  
  @Test
  void testGetStockDiff(){
    assertEquals(0, itemB.getStock());
  }
  
  @Test
  void testGetDesc(){
    assertEquals("a", itemA.getDescription());
  }
  
  @Test
  void testGetDescDiff(){
    assertEquals("ab", itemB.getDescription());
  }
}
