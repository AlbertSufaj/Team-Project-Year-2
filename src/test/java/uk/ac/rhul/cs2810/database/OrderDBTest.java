package uk.ac.rhul.cs2810.database;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import uk.ac.rhul.cs2810.Exceptions.*;
import uk.ac.rhul.cs2810.containers.Item;
import uk.ac.rhul.cs2810.containers.ItemCategory;
import uk.ac.rhul.cs2810.containers.Order;
import uk.ac.rhul.cs2810.containers.OrderState;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import static java.lang.Math.abs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("OrderDB")
public class OrderDBTest extends DatabaseTest {
  
  @BeforeAll
  static void generateTables() {
    tables = new String[]{"orders", "itemsInOrder"};
  }
  
  @BeforeEach
  void generateOrderDB() throws ConnectionError, ExecutionError {
    orderDB = DatabaseFactory.getTestOrderDB();
  }
  
  //1 connection excess
  @Test
  void testGetOrderDB() throws ConnectionError, ExecutionError {
    orderDB = DatabaseFactory.getTestOrderDB();
  }
  
  //1 connection excess
  @Test
  void testMakeTables() throws ConnectionError, ExecutionError, SQLException {
    Statement st = getStatement();
    
    DatabaseFactory.getTestOrderDB();
    
    if (!Database.doTablesExist(st, tables)) {
      fail("Databases not made");
    }
    Database.closeConnection(st.getConnection());
  }
  
  //0 connection excess
  @Test
  void testAddOrder()
      throws ConnectionError, ExecutionError, SQLException, MissingDataError, OrderAlreadyExistsException {
    orderDB.addOrder(order);
    
    Statement st = getStatement();
    ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM orders");
    rs.next();
    if (rs.getInt(1) != 1) {
      fail("Item not added");
    }
    
    Database.closeConnection(st.getConnection());
    rs.close();
  }
  
  //0 connection excess
  @Test
  void testAddDuplicateOrder() throws MissingDataError, ExecutionError, ConnectionError, OrderAlreadyExistsException {
    orderDB.addOrder(order);
    assertThrows(OrderAlreadyExistsException.class, () -> {
      orderDB.addOrder(order);
    });
  }
  
  //2 connection excess
  @Test
  void testGetOrders() throws ConnectionError, ExecutionError, MissingDataError, OrderAlreadyExistsException {
    orderDB.addOrder(order);
    
    Order fetchedOrder = orderDB.getOrders().get(0);
    assertTrue(order.equals(fetchedOrder));
  }
  
  //1 connection excess
  @Test
  void testGetUnconfirmedOrders()
      throws MissingDataError, ExecutionError, ConnectionError, OrderAlreadyExistsException {
    Order orderA = new Order(1);
    orderA.addItemToOrder(item);
    orderA.setState(OrderState.PREPARING);
    orderDB.addOrder(orderA);
    
    Order orderB = new Order(1);
    orderB.addItemToOrder(item);
    orderDB.addOrder(orderB);
    
    List<Order> fetchedOrders = orderDB.getOrdersAssignedToWaiterFromState(OrderState.UNCONFIRMED, 0);
    assertEquals(1, fetchedOrders.size());
  }
  
  //1 connection excess
  @Test
  void testErrorEmptyOrder() throws ConnectionError, ExecutionError, OrderAlreadyExistsException {
    Order order = new Order();
    try {
      orderDB.addOrder(order);
      fail("Expected MissingDataError but was not thrown");
    } catch (MissingDataError MDE) {
      assertThat(MDE.getMessage(), containsString("contain items"));
    }
  }
  
  //1 connection excess
  @Test
  void testGetConfirmOrder() throws MissingDataError, ExecutionError, ConnectionError, OrderAlreadyExistsException {
    Order orderA = new Order(1);
    orderA.addItemToOrder(item);
    orderA.setState(OrderState.CONFIRMED);
    
    Order orderB = new Order(2);
    orderB.addItemToOrder(item);
    orderB.setState(OrderState.UNCONFIRMED);
    
    orderDB.addOrder(orderA);
    orderDB.addOrder(orderB);
    
    List<Order> fetchedOrders = orderDB.getOrdersAssignedToWaiterFromState(OrderState.CONFIRMED, -1);
    assertEquals(1, fetchedOrders.size());
  }
  
  //10 connections
  @Test
  void testConfirmOrder()
      throws MissingDataError, ExecutionError, ConnectionError, OrderNotFoundException, OrderAlreadyExistsException {
    orderDB.addOrder(order);
    orderDB.confirmOrder(order);
    
    List<Order> fetchedOrders = orderDB.getOrdersAssignedToWaiterFromState(OrderState.CONFIRMED, 0);
    assertEquals(1, fetchedOrders.size());
  }
  
  //0 connections
  @Test
  void testRemoveOrder()
      throws ConnectionError, ExecutionError, SQLException, MissingDataError, OrderAlreadyExistsException {
    
    orderDB = DatabaseFactory.getTestOrderDB();
    orderDB.addOrder(order);
    orderDB.removeOrder(order);
    
    Statement st = getStatement();
    ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM orders");
    rs.next();
    if (rs.getInt(1) != 0) {
      fail("Item not removed");
    }
    
    Database.closeConnection(st.getConnection());
    rs.close();
  }
  
  // We just need to check it doesn't throw an error as we know it already removes an item that does exist
  //1 connection excess
  @Test
  void testRemoveOrderNotInDB() throws ConnectionError, ExecutionError {
    Order order = new Order(1);
    order.addItemToOrder(item);
    orderDB.removeOrder(order);
  }
  
  //1 connection
  @Test
  void testClearOrdersEmpty() throws ConnectionError, ExecutionError {
    orderDB.clearOrders();
  }
  
  //0 connection excess
  @Test
  void testClearOrders() throws MissingDataError, ExecutionError, ConnectionError, OrderAlreadyExistsException {
    orderDB.addOrder(order);
    orderDB.clearOrders();
  }
  
  //0 connection excess
  @Test
  void testSetOrderStateConfirmed()
      throws MissingDataError, ExecutionError, ConnectionError, OrderNotFoundException, OrderAlreadyExistsException {
    orderDB.addOrder(order);
    orderDB.setOrderState(order, OrderState.CONFIRMED);
    assertEquals(OrderState.CONFIRMED, orderDB.getOrderFromID(order.getID()).getState());
  }
  
  //0 connection excess
  @Test
  void testSetOrderStateReady()
      throws MissingDataError, ExecutionError, ConnectionError, OrderNotFoundException, OrderAlreadyExistsException {
    orderDB.addOrder(order);
    orderDB.setOrderState(order, OrderState.READY);
    assertEquals(OrderState.READY, orderDB.getOrderFromID(order.getID()).getState());
  }
  
  //1 connection excess
  @Test
  void testSetOrderStateNotInDB() {
    assertThrows(OrderNotFoundException.class, () -> {
      orderDB.setOrderState(order, OrderState.READY);
    });
  }
  
  //1 connection excess
  @Test
  void testSetStateCancelled()
      throws ConnectionError, ExecutionError, SQLException, MissingDataError, OrderNotFoundException, OrderAlreadyExistsException {
    
    orderDB = DatabaseFactory.getTestOrderDB();
    orderDB.addOrder(order);
    orderDB.setOrderState(order, OrderState.CANCELLED);
    
    Statement st = getStatement();
    ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM orders");
    rs.next();
    if (rs.getInt(1) != 0) {
      fail("Item not removed");
    }
    
    Database.closeConnection(st.getConnection());
    rs.close();
  }
  
  //2 connection excess
  @Test
  void testGetOrderStatus()
      throws MissingDataError, ExecutionError, ConnectionError, OrderNotFoundException, OrderAlreadyExistsException {
    order.setState(OrderState.UNCONFIRMED);
    orderDB.addOrder(order);
    assertEquals(OrderState.UNCONFIRMED, orderDB.getOrderState(order));
  }
  
  @Test
  void testGetOrdersAssignedToWaiter()
      throws MissingDataError, ExecutionError, ConnectionError, OrderAlreadyExistsException {
    orderDB.addOrder(order); // Order is assigned to table 1
    order.setID(-1);
    
    Order orderA = new Order(2);
    orderA.addItemToOrder(item);
    orderDB.addOrder(orderA);
    
    TableDB tableDB = DatabaseFactory.getTestTableDB();
    LoginDB loginDB = DatabaseFactory.getTestLoginDB();
    int ID = loginDB.getID(LoginDB.hash(8149));
    tableDB.assignTable(1);
    
    assertEquals(1, orderDB.getOrdersAssignedToWaiter(ID).size());
  }
  
  @Test
  @Tag("Slow")
  void testGetOrderTime()
      throws InterruptedException, MissingDataError, ExecutionError, ConnectionError, OrderAlreadyExistsException {
    orderDB.addOrder(order);
    Thread.sleep(600, 0);
    long delay = Duration.between(orderDB.getOrders().get(0).getTime(), LocalTime.now()).toMillis();
    assertThat(delay, greaterThan(1L));
  }
  
  @Test
  @Tag("Slow")
  void testOrderSorting()
      throws MissingDataError, ExecutionError, ConnectionError, InterruptedException, OrderAlreadyExistsException {
    orderDB.addOrder(order);
    Thread.sleep(1000, 0);
    order.setID(-1);
    orderDB.addOrder(order);
    List<Order> orders = orderDB.getOrders();
    assertThat(orders.get(0).getTime(), lessThanOrEqualTo(orders.get(1).getTime()));
  }
  
  @Test
  void testGetOrderFromID() throws MissingDataError, ExecutionError, ConnectionError, OrderAlreadyExistsException {
    orderDB.addOrder(order);
    assertEquals(order, orderDB.getOrderFromID(order.getID()));
  }
  
  @Test
  void testGetNonExistingOrderFromID() throws ExecutionError, ConnectionError {
    assertNull(orderDB.getOrderFromID(order.getID()));
  }
  
  @Test
  void testDecrementStockOnConfirmOrder()
      throws MissingDataError, ExecutionError, ConnectionError, OrderNotFoundException, OrderAlreadyExistsException {
    
    orderDB.addOrder(order);
    orderDB.confirmOrder(order);
    MenuDB menuDB = DatabaseFactory.getTestMenu();
    menuDB.setIgnoreOutOfStock(false);
    assertEquals(1, menuDB.getMenu().size());
  }
  
  @Test
  void testGetOrdersAwaitingDelivery()
      throws ConnectionError, ExecutionError, MissingDataError, OrderNotFoundException, OrderAlreadyExistsException {
    tableDB = DatabaseFactory.getTestTableDB();
    
    LoginDB loginDB = DatabaseFactory.getTestLoginDB();
    int ID = loginDB.getID(LoginDB.hash(8149));
    tableDB.assignTable(1);
    
    orderDB.addOrder(order);
    orderDB.confirmOrder(order);
    orderDB.setOrderState(order, OrderState.READY);
    assertEquals(1, orderDB.getOrdersAssignedToWaiterFromState(OrderState.READY, ID).size());
  }
  
  @Test
  void testGetOrderStatusDoesntExist() throws ConnectionError, ExecutionError {
    assertThrows(OrderNotFoundException.class, () -> {
      orderDB.getOrderState(order);
    });
  }
  
  @Test
  void testAddOrderTableNumTooLow() {
    Order badOrder = new Order(0);
    badOrder.addItemToOrder(item);
    assertThrows(ExecutionError.class, () -> {
      orderDB.addOrder(badOrder);
    });
  }
  
  @Test
  void testAddOrderTableNumTooHigh() {
    Order badOrder = new Order(13);
    badOrder.addItemToOrder(item);
    assertThrows(ExecutionError.class, () -> {
      orderDB.addOrder(badOrder);
    });
  }
  
  @Test
  void testGetPayedDefault()
      throws ConnectionError, ExecutionError, MissingDataError, OrderNotFoundException,
      OrderAlreadyExistsException {
    orderDB.addOrder(order);
    assertFalse(orderDB.getPayed(order));
  }
  
  @Test
  void testSetPayed()
      throws ConnectionError, ExecutionError, MissingDataError, OrderNotFoundException,
      OrderAlreadyExistsException {
    orderDB.addOrder(order);
    orderDB.setOrderPayed(order);
    assertTrue(orderDB.getPayed(order));
  }
  
  @Test
  void addOrderSetID() throws MissingDataError, ExecutionError, ConnectionError,
      OrderAlreadyExistsException {
    order.setID(2);
    orderDB.addOrder(order);
    assertNotNull(orderDB.getOrderFromID(2));
  }
  
  @Test
  void testModifyOrderTableNum()
      throws MissingDataError, ExecutionError, ConnectionError, OrderNotFoundException,
      OrderAlreadyExistsException {
    orderDB.addOrder(order);
    Order newOrder = new Order(9);
    newOrder.setID(order.getID());
    newOrder.addItemToOrder(order.getItemsInOrder().get(0));
    orderDB.modifyOrder(newOrder);
    assertEquals(9, orderDB.getOrderFromID(order.getID()).getTableNumber());
  }
  
  @Test
  void testModifyOrderAddItem()
      throws MissingDataError, ExecutionError, ConnectionError, OrderNotFoundException,
      OrderAlreadyExistsException {
    Item newItem = new Item(2, "a", "a", 1, 1,
        ItemCategory.BURRITOS, false, false, false, 10, 0);
    orderDB.addOrder(order);
    order.addItemToOrder(newItem);
    orderDB.modifyOrder(order);
    Order fetchedOrder = orderDB.getOrderFromID(order.getID());
    assertEquals(2, fetchedOrder.getItemsInOrder().size());
    assertTrue(fetchedOrder.getItemsInOrder().contains(newItem));
  }
  
  @Test
  void testModifyOrderChangeItemCount()
      throws MissingDataError, ExecutionError, ConnectionError, OrderNotFoundException,
      OrderAlreadyExistsException {
    orderDB.addOrder(order);
    order.addItemToOrder(item);
    orderDB.modifyOrder(order);
    Order fetchedOrder = orderDB.getOrderFromID(order.getID());
    assertEquals(2, fetchedOrder.getItemsInOrder().size());
  }
  
  @Test
  void testModifyOrderRemoveItem()
      throws MissingDataError, ExecutionError, ConnectionError, OrderNotFoundException,
      OrderAlreadyExistsException {
    Item newItem = new Item(2, "a", "a", 1, 1,
        ItemCategory.BURRITOS, false, false, false, 10, 0);
    order.addItemToOrder(newItem);
    orderDB.addOrder(order);
    order.removeItemFromOrder(newItem);
    orderDB.modifyOrder(order);
    Order fetchedOrder = orderDB.getOrderFromID(order.getID());
    assertEquals(1, fetchedOrder.getItemsInOrder().size());
  }
  
  @Test
  void testModifyOrderDoesntExist() {
    order.setID(5);
    assertThrows(OrderNotFoundException.class, () -> {
      orderDB.modifyOrder(order);
    });
  }
  
  @Test
  void testConfirmOrderOrderDoesntExist() {
    order.setID(1);
    assertThrows(OrderNotFoundException.class, () -> {
      orderDB.confirmOrder(order);
    });
  }
  
  @Test
  void testGetOrdersAssignedToTable() throws ConnectionError, MissingDataError, ExecutionError,
      OrderAlreadyExistsException {
    orderDB.addOrder(order);
    Order newOrder = new Order(9);
    newOrder.addItemToOrder(item);
    orderDB.addOrder(newOrder);
    assertEquals(1, orderDB.getOrdersFromTable(order.getTableNumber()).size());
  }
  
  @Test
  void testSetOrderStateUpdatesTime() throws ConnectionError, MissingDataError, ExecutionError,
      OrderAlreadyExistsException, OrderNotFoundException {
    orderDB.addOrder(order);
    Order oldOrder = orderDB.getOrderFromID(order.getID());
    orderDB.setOrderState(order, OrderState.SERVED);
    int millis = (int) abs(Duration.between(oldOrder.getTimeStatusChanged(),
        order.getTimeStatusChanged()).toMillis());
    assertThat(millis, greaterThan(10));
  }
  
  @Test
  void testSetOrderStatusReturnsDelay()
      throws OrderNotFoundException, ExecutionError, ConnectionError, OrderAlreadyExistsException,
      MissingDataError {
    orderDB.addOrder(order);
    LocalTime pretendTime = LocalTime.now();
    pretendTime = pretendTime.minusMinutes(10);
    order.setTimeStatusChanged(pretendTime);
    assertThat(orderDB.setOrderState(order, OrderState.CONFIRMED), greaterThanOrEqualTo(10));
  }
  
  @Test
  void testCacheNotUsedWhenDataShouldBeEmpty()
      throws ConnectionError, ExecutionError, OrderAlreadyExistsException, MissingDataError,
      OrderNotFoundException {
    orderDB.addOrder(order);
    orderDB.getOrdersAssignedToWaiterFromState(OrderState.UNCONFIRMED, 0);
    orderDB.setOrderState(order, OrderState.CONFIRMED);
    assertEquals(0, orderDB.getOrdersAssignedToWaiterFromState(OrderState.UNCONFIRMED,
        0).size());
  }
  
  @Test
  void testGetOrdersFromStateNotReturningNull() throws ConnectionError, ExecutionError {
    assertNotNull(orderDB.getOrdersAssignedToWaiterFromState(OrderState.SERVED, 0));
  }
  
  @Test
  void testGetOrderFromStateReturnsCorrectValues()
      throws ConnectionError, ExecutionError, OrderAlreadyExistsException, MissingDataError,
      OrderNotFoundException {
    
    orderDB.addOrder(order);
    orderDB.setOrderState(order, OrderState.CONFIRMED);
    orderDB.getOrdersAssignedToWaiterFromState(OrderState.CONFIRMED, 0);
    assertEquals(1, orderDB.getOrdersAssignedToWaiterFromState(OrderState.CONFIRMED,
        0).size());
  }
  
  @Test
  void testChangeOrderStateThenFetchReturnsUpdatedList()
      throws ConnectionError, MissingDataError, ExecutionError, OrderAlreadyExistsException,
      OrderNotFoundException {
    orderDB.addOrder(order);
    orderDB.getOrdersAssignedToWaiterFromState(OrderState.CONFIRMED, -1);
    orderDB.setOrderState(order, OrderState.CONFIRMED);
    assertEquals(1, orderDB.getOrdersAssignedToWaiterFromState(OrderState.CONFIRMED,
        -1).size());
  }
  
  @Test
  void testGetOrderAssignedToWaiterFromState()
      throws ConnectionError, MissingDataError, ExecutionError, OrderAlreadyExistsException,
      OrderNotFoundException {
    
    orderDB.addOrder(order);
    order.setID(-1);
    orderDB.addOrder(order);
    orderDB.setOrderState(order, OrderState.CONFIRMED);
    
    LoginDB loginDB = DatabaseFactory.getTestLoginDB();
    int ID = loginDB.getID(LoginDB.hash(8149));
    tableDB.assignTable(1);
    
    assertEquals(1, orderDB.getOrdersAssignedToWaiterFromState(OrderState.CONFIRMED,
        ID).size());
  }
  
  @Test
  void testGetOrdersAfterNewAdded()
      throws ConnectionError, ExecutionError, OrderAlreadyExistsException, MissingDataError {
    
    LoginDB loginDB = DatabaseFactory.getTestLoginDB();
    tableDB = DatabaseFactory.getTestTableDB();
    int ID = loginDB.getID(LoginDB.hash(8149));
    tableDB.assignTable(1);
    
    assert orderDB.getOrdersAssignedToWaiterFromState(OrderState.UNCONFIRMED, ID).size() == 0;
    
    orderDB.addOrder(order);
    
    assertEquals(1, orderDB.getOrdersAssignedToWaiterFromState(OrderState.UNCONFIRMED,
        ID).size());
  }
  
  @Test
  void testGetOrdersAfterOrderRemoved()
      throws ConnectionError, ExecutionError, OrderAlreadyExistsException, MissingDataError {
    
    LoginDB loginDB = DatabaseFactory.getTestLoginDB();
    tableDB = DatabaseFactory.getTestTableDB();
    int ID = loginDB.getID(LoginDB.hash(8149));
    tableDB.assignTable(1);
    
    orderDB.addOrder(order);
    order.setID(-1);
    orderDB.addOrder(order);
    
    assertEquals(2, orderDB.getOrdersAssignedToWaiterFromState(OrderState.UNCONFIRMED,
        ID).size(),
        "Setup failed");
    
    orderDB.removeOrder(order);
    
    assertEquals(1, orderDB.getOrdersAssignedToWaiterFromState(OrderState.UNCONFIRMED,
        ID).size());
  }
  
  @Test
  void testGetOrdersAfterOrderCanceled()
      throws ConnectionError, ExecutionError, OrderAlreadyExistsException, MissingDataError,
      OrderNotFoundException {
    
    LoginDB loginDB = DatabaseFactory.getTestLoginDB();
    tableDB = DatabaseFactory.getTestTableDB();
    int ID = loginDB.getID(LoginDB.hash(8149));
    tableDB.assignTable(1);
    
    orderDB.addOrder(order);
    order.setID(-1);
    orderDB.addOrder(order);
    
    assertEquals(2, orderDB.getOrdersAssignedToWaiterFromState(OrderState.UNCONFIRMED,
        ID).size(),
        "Setup failed");
    
    orderDB.setOrderState(order, OrderState.CANCELLED);
    
    assertEquals(1, orderDB.getOrdersAssignedToWaiterFromState(OrderState.UNCONFIRMED,
        ID).size());
  }
  
  @Test
  void testConfirmOrderNotInStock()
      throws ConnectionError, MissingDataError, ExecutionError,
      OrderAlreadyExistsException, OrderNotFoundException {
    orderDB.addOrder(order);
    int idA = order.getID();
    order.setID(-1);
    orderDB.addOrder(order);
    orderDB.confirmOrder(order);
    order.setID(idA);
    try {
      orderDB.confirmOrder(order);
    } catch (ExecutionError EE) {
      testExecutionError(EE, "out of stock");
    }
  }
  
  @Test
  void testChangingStateWithCaching()
      throws ConnectionError, MissingDataError, ExecutionError,
      OrderAlreadyExistsException, OrderNotFoundException {
    
    orderDB.addOrder(order);
    orderDB.setOrderState(order, OrderState.CONFIRMED);
    int idA = order.getID();
    order.setID(-1);
    orderDB.addOrder(order);
    orderDB.setOrderState(order, OrderState.CONFIRMED);
    assertEquals(2,
        orderDB.getOrdersAssignedToWaiterFromState(OrderState.CONFIRMED, 0).size());
    orderDB.setOrderState(order, OrderState.PREPARING);
    assertEquals(1,
        orderDB.getOrdersAssignedToWaiterFromState(OrderState.CONFIRMED, 0).size());
    assertEquals(1,
        orderDB.getOrdersAssignedToWaiterFromState(OrderState.PREPARING, 0).size());
  }
  
  @Test
  void testAddingConfirmedOrderWithCaching()
      throws ConnectionError, MissingDataError, ExecutionError,
      OrderAlreadyExistsException, OrderNotFoundException {
    orderDB.addOrder(order);
    orderDB.setOrderState(order, OrderState.CONFIRMED);
    int idA = order.getID();
    order.setID(-1);
    assertEquals(1,
        orderDB.getOrdersAssignedToWaiterFromState(OrderState.CONFIRMED, 0).size());
    orderDB.addOrder(order);
    orderDB.setOrderState(order, OrderState.CONFIRMED);
    assertEquals(2,
        orderDB.getOrdersAssignedToWaiterFromState(OrderState.CONFIRMED, 0).size());
  }
}

