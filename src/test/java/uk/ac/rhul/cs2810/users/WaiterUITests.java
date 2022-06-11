package uk.ac.rhul.cs2810.users;

import uk.ac.rhul.cs2810.Exceptions.*;
import uk.ac.rhul.cs2810.containers.Item;
import uk.ac.rhul.cs2810.containers.ItemCategory;
import uk.ac.rhul.cs2810.containers.Order;
import uk.ac.rhul.cs2810.containers.OrderState;
import uk.ac.rhul.cs2810.database.*;

import java.io.IOException;

public class WaiterUITests {
  
  public static void main(String[] args) throws IOException, ExecutionError, ConnectionError, MissingDataError, InterruptedException, OrderNotFoundException, OrderAlreadyExistsException {
    testDisplayOrdersForDelivery();
  }
  
  public static void testDisplayOrdersForDelivery() throws ConnectionError, ExecutionError, IOException, InterruptedException, MissingDataError, OrderNotFoundException, OrderAlreadyExistsException {
    OrderDB orderDB = DatabaseFactory.getOrderDB();
    LoginDB loginDB = DatabaseFactory.getLoginDB();
    TableDB tableDB = DatabaseFactory.getTableDB();
    Login login = new Login();
    
    new Thread(() -> {
      login.setUserType('w');
    }).start();
    while (loginDB.getLoggedInWaiters().size() < 1){
      Thread.sleep(1000);
    }
    tableDB.assignTable(1);
    tableDB.assignTable(12);
    Item item = new Item(1, "", "", 1, 1,
        ItemCategory.BURRITOS, false, false, false, 10,  0);
    Order order = new Order(1);
    order.addItemToOrder(item);
    orderDB.addOrder(order);
    orderDB.confirmOrder(order);
    orderDB.setOrderState(order, OrderState.READY);
    tableDB.setWaiterAlerted(12, true);
  }
}
