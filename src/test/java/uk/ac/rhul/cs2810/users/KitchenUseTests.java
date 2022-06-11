package uk.ac.rhul.cs2810.users;

import uk.ac.rhul.cs2810.Exceptions.*;
import uk.ac.rhul.cs2810.containers.Item;
import uk.ac.rhul.cs2810.containers.Order;
import uk.ac.rhul.cs2810.containers.OrderState;
import uk.ac.rhul.cs2810.database.*;

import java.util.List;
import java.util.Random;

public class KitchenUseTests {
  
  public static void main(String[] args)
      throws ConnectionError, ExecutionError, InterruptedException,
      OrderAlreadyExistsException, MissingDataError, OrderNotFoundException {
    
    TableDB tableDB = DatabaseFactory.getTableDB();
    OrderDB orderDB = DatabaseFactory.getOrderDB();
    MenuDB menuDB = DatabaseFactory.getMenuDB();
    Random rand = new Random();
    Login login = new Login();
    
    orderDB.clearOrders();
    
    new Thread(() -> {
      login.setUserType('k');
    }).start();
    while (true) {
      Thread.sleep((rand.nextInt(3) + 1) * 1000);
      int tableNum = rand.nextInt(11) + 1;
      tableDB.assignTable(tableNum);
      Order order = new Order(tableNum);
      List<Item> menu = menuDB.getMenu();
      
      for (int i = 0; i < rand.nextInt(5) + 1; i++) {
        order.addItemToOrder(menu.get(rand.nextInt(menu.size())));
      }
      
      orderDB.addOrder(order);
      orderDB.setOrderState(order, OrderState.CONFIRMED);
      System.out.println("Confirmed orders = " +
          orderDB.getOrdersAssignedToWaiterFromState(OrderState.CONFIRMED, 0).size());
      System.out.println("Unconfirmed orders = " +
          orderDB.getOrdersAssignedToWaiterFromState(OrderState.UNCONFIRMED, 0).size());
    }
  }
}
