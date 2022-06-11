package uk.ac.rhul.cs2810.containers;

/**
 * The data container for the notification table view in waiter.
 */
public class NotificationTableData {
  private int id;
  private int tableNumber;
  private String request;
  
  /**
   * Constructor to initialise the data to be stored.
   * 
   * @param id The Order ID for the specified order.
   * @param tableNum The Table Number for the specified order.
   * @param request The request related to the order.
   */
  public NotificationTableData(int id, int tableNum, String request) {
    this.id = id;
    this.tableNumber = tableNum;
    this.request = request;
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
   * Gets the request that has been set for the waiter to be notified.
   * 
   * @return Returns the request set on the order
   */
  public String getRequest() {
    return this.request;
  }
}
