package uk.ac.rhul.cs2810.database;

import org.junit.jupiter.api.*;
import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;
import uk.ac.rhul.cs2810.Exceptions.InvalidItemException;
import uk.ac.rhul.cs2810.containers.Item;
import uk.ac.rhul.cs2810.containers.ItemCategory;
import uk.ac.rhul.cs2810.containers.Order;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("MenuDB")
class MenuDBTest extends DatabaseTest{
  
  private static Item testItem;
  
  @BeforeAll
  static void setTables(){
    tables = new String[]{"allergens", "allergensInItems", "menu"};
    testItem = new Item(1, "Food1", "Desc1", 199, 0,
        ItemCategory.BURRITOS, false, false, false, 10, 100);
  }

  @Test
  void testMakeMenuDB() {
    try {
      MenuDB menu = DatabaseFactory.getTestMenu();
    } catch (ExecutionError | ConnectionError E) {
    }
  }
  
  @Test
  @Tag("Creation")
  void testMakesTables() throws SQLException, ConnectionError {
    if (!connection) {
      fail("Could not connect to run test");
    }
    
    DatabaseFactory.reset();
    try {
      DatabaseFactory.getTestMenu();
    } catch (ExecutionError EE) {
    }
    
    if (!Database.doTablesExist(st, tables)) {
      fail("Databases not made");
    }
  }
  
  @Test
  @Tag("Creation")
  void testPopulateTablesSeparately() throws SQLException, ConnectionError, ExecutionError {
    if (!connection) {
      fail("Could not connect to run test");
    }
  
    MenuDB menu = DatabaseFactory.getTestMenu();
    for (String table : new String[] {"allergens", "allergensInItems", "menu"}) {
      ResultSet rs = st.executeQuery("SELECT count(*) FROM " + table + ";");
      rs.next();
      if (rs.getInt(1) == 0) {
        fail("Table " + table + " not populated");
      }
      rs.close();
    }
  }
  
  @Test
  @Tag("Creation")
  void testPopulateTablesTogether() throws IOException, SQLException, ConnectionError, ExecutionError {
    if (!connection) {
      fail("Could not connect to run test");
    }
    changeConfigLine(3, "Menu Format = \"G\"");
    
    MenuDB menu = DatabaseFactory.getTestMenu();
    for (String table : new String[] {"allergens", "allergensInItems", "menu"}) {
      ResultSet rs = st.executeQuery("SELECT count(*) FROM " + table + ";");
      rs.next();
      if (rs.getInt(1) == 0) {
        fail("Table " + table + " not populated");
      }
      rs.close();
    }
  }
  
  @Test
  void testGetMenuItems() throws ExecutionError, ConnectionError {
    MenuDB menuDB = DatabaseFactory.getTestMenu();
    List<Item> menu = menuDB.getMenu();
    if (menu.get(0).getID() != 1 || menu.get(1).getID() != 2) {
      fail("Ids not correctly extracted from file");
    }
  }
  
  @Test
  void testGetMenuCategory() throws ConnectionError, ExecutionError {
    MenuDB menuDB = DatabaseFactory.getTestMenu();
    List<Item> menu = menuDB.getMenu();
    assertEquals(ItemCategory.BURRITOS, menu.get(0).getCategory());
  }
  
  @Test
  void testGetMenuMap() throws ConnectionError, ExecutionError {
    MenuDB menuDB = DatabaseFactory.getTestMenu();
    testItem.addAllergen("Wheat");
    testItem.addAllergen("Gluten");
    assertEquals(testItem.toString(), menuDB.getMap().get(1).toString());
  }
  
  @Test
  void testGetFilteredMenuSingleFilter() throws ConnectionError, ExecutionError {
    MenuDB menuDB = DatabaseFactory.getTestMenu();
    List<Item> filteredMenus = menuDB.getFilteredMenu(new Filter(new HashSet<>(), true,
        false, false));
    assertEquals(1, filteredMenus.size());
  }
  
  @Test
  void testGetFilteredMenuNoFilter() throws ConnectionError, ExecutionError, SQLException {
    MenuDB menuDB = DatabaseFactory.getTestMenu();
    List<Item> filteredMenus = menuDB.getFilteredMenu(new Filter(new HashSet<>(), false,
        false, false));
    assertEquals(2, filteredMenus.size());
  }
  
  @Test
  void testGetFilteredMenuAllergen() throws ConnectionError, ExecutionError {
    MenuDB menuDB = DatabaseFactory.getTestMenu();
    Set<String> allergens = new HashSet<>();
    allergens.add("Wheat");
    allergens.add("Milk");
    List<Item> filteredMenus = menuDB.getFilteredMenu(new Filter(allergens, false,
        false, false));
    assertEquals(1, filteredMenus.size());
  }
  
  @Test
  @Tag("Creation")
  void testGetAllergens() throws ConnectionError, ExecutionError {
    List<String> allergens = DatabaseFactory.getTestMenu().getAllergens();
    List<String> expected = new LinkedList<>();
    expected.add("Wheat");
    expected.add("Gluten");
    assertTrue(allergens.containsAll(expected));
  }
  
  @Test
  @Tag("OrderDB")
  void testDecreaseStock() throws ConnectionError, ExecutionError, SQLException {
    MenuDB menuDB = DatabaseFactory.getTestMenu();
    order = new Order(1);
    order.addItemToOrder(item);
    int id = item.getID();
    menuDB.decreaseStock(order);
    menuDB.setIgnoreOutOfStock(false);
    assertEquals(1, menuDB.getMenu().size());
  }
  
  @Test
  @Tag("Creation")
  void testGetItemsOutOfStock() throws ConnectionError, ExecutionError {
    MenuDB menuDB = DatabaseFactory.getTestMenu();
    menuDB.setIgnoreOutOfStock(true);
    assertEquals(2, menuDB.getMenu().size());
  }
  
  @Test
  @Tag("Prod")
  @Tag("Creation")
  void testGetProdMenu() throws ConnectionError, ExecutionError {
    DatabaseFactory.getMenuDB();
  }
  
  @Test
  void testAddItem() throws ConnectionError, ExecutionError, InvalidItemException {
    MenuDB menuDB = DatabaseFactory.getTestMenu();
    Item blankItem = new Item(-1, "blank", "desc", 1, 1, ItemCategory.BURRITOS,
        true, true, true, 10, 0);
    menuDB.addItem(blankItem);
    Map<Integer, Item> itemMap = menuDB.getMap();
    assertEquals("blank", itemMap.get(Collections.max(itemMap.keySet())).getName());
  }
  
  @Test
  void testRemoveItem() throws ConnectionError, ExecutionError {
    MenuDB menuDB = DatabaseFactory.getTestMenu();
    menuDB.removeItem(1);
    assertThat(menuDB.getMap().keySet(), not(contains(1)));
  }
  
  @Test
  void testChangeItem() throws ConnectionError, ExecutionError, InvalidItemException {
    MenuDB menuDB = DatabaseFactory.getTestMenu();
    int itemID = item.getID();
    menuDB.modifyItem(item);
    Item newItem = menuDB.getMap().get(itemID);
    assertEquals("taco", newItem.getName());
  }
  
  @Test
  void testAddItemTooLongName() throws ConnectionError, ExecutionError, InvalidItemException {
    MenuDB menuDB = DatabaseFactory.getTestMenu();
    Item newItem = new Item(1, "a".repeat(50), "a", 1, 1,
        ItemCategory.BURRITOS, true, true, true, 1, 0);
    try {
      menuDB.addItem(newItem);
    } catch (InvalidItemException IIE) {
      assertEquals("itemname", IIE.getBadInputColumn());
    }
  }
  
  @Test
  void testGetDefaultGetAverageTimeToCook() throws ConnectionError, ExecutionError {
    MenuDB menuDB = DatabaseFactory.getTestMenu();
    assertEquals(Float.NaN, menuDB.getAverageTimeToCook(1));
  }
  
  @Test
  void testAddOrderToData() throws ConnectionError, ExecutionError {
    MenuDB menuDB = DatabaseFactory.getTestMenu();
    menuDB.addOrderToData(order, 10);
    assertEquals(10, menuDB.getAverageTimeToCook(1));
  }
  
  @Test
  void modifyItemWithImages() throws ConnectionError, ExecutionError, InvalidItemException {
    MenuDB menuDB = DatabaseFactory.getTestMenu();
    ImageDB imageDB = DatabaseFactory.getTestImageDB();
    int itemID = item.getID();
    menuDB.modifyItem(item);
    Item newItem = menuDB.getMap().get(itemID);
    assertEquals("taco", newItem.getName());
  }
  
  @Test
  void removeItemWithImages() throws ConnectionError, ExecutionError {
    MenuDB menuDB = DatabaseFactory.getTestMenu();
    ImageDB imageDB = DatabaseFactory.getTestImageDB();
    menuDB.removeItem(1);
    assertThat(menuDB.getMap().keySet(), not(contains(1)));
  }
  
  @Test
  void testModifyOrderInvalidPrice() throws ConnectionError, ExecutionError {
    MenuDB menuDB = DatabaseFactory.getTestMenu();
    Item newItem = new Item(1, "", "", 1, 1, ItemCategory.BURRITOS,
        false, false, false, 1, 1);
    try {
      menuDB.modifyItem(newItem);
    } catch (InvalidItemException IIE){
      assertThat(IIE.getMessage().toLowerCase(), containsString("price too low"));
    }
  }
  
  @Test
  void testModifyItemAddAllergen() throws ConnectionError, ExecutionError, InvalidItemException {
    MenuDB menuDB = DatabaseFactory.getTestMenu();
    List<Item> menu = menuDB.getMenu();
    List<String> allergenList = menuDB.getAllergens();
    Item item = null;
    for (Item i: menu){
      if (i.getID() == 2){
        item = i;
        break;
      }
    }
    assert item != null;
    assertFalse(item.getAllergens().contains(allergenList.get(1)));
    item.addAllergen(allergenList.get(1));
    menuDB.modifyItem(item);
    DatabaseFactory.reset(); // Clears cache
    menuDB = DatabaseFactory.getTestMenu();
    menu = menuDB.getMenu();
    item = null;
    for (Item i: menu){
      if (i.getID() == 2){
        item = i;
        break;
      }
    }
    assert item != null;
    assertThat(item.getAllergens(), contains(allergenList.get(1)));
  }
}