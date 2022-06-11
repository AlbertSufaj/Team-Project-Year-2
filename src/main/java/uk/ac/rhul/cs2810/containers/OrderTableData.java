package uk.ac.rhul.cs2810.containers;

/**
 * The data container for the order table view in waiter.
 */
public class OrderTableData {
  private int id;
  private int tableNumber;
  private OrderState state;
  private String time;
  
  /**
   * Constructor to initialise the data to be stored.
   * 
   * @param id The Order ID for the specified order.
   * @param tableNum The Table Number for the specified order.
   * @param st The Order State for the specified order.
   * @param time The time the order was placed.
   */
  public OrderTableData(int id, int tableNum, OrderState st, String time) {
    this.id = id;
    this.tableNumber = tableNum;
    this.state = st;
    this.time = time;
  }
  
  /**
   * Gets the order id from table data.
   * 
   * @return Returns the ID of the order
   */
  public int getID() {
    return this.id;
  }
  
  /**
   * Gets the table number from table data.
   * 
   * @return Returns the table number of the order
   */
  public int getTableNumber() {
    return this.tableNumber;
  }
  
  /**
   * Gets the order state from table data.
   * 
   * @return Returns the order state.
   */
  public OrderState getOrderState() {
    return this.state;
  }
  
  /**
   * Gets the time the order was placed from table data.
   * 
   * @return Returns the time placed.
   */
  public String getTimePlaced() {
    return this.time;
  }
}
