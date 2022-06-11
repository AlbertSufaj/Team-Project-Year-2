package uk.ac.rhul.cs2810.containers;

/**
 * The data container for the orders which are to be paid table view on customer login.
 */
public class OrdersToPayData {
  private int id;
  private int tableNumber;
  private Price price;
  
  /**
   * Constructor to initialise the data to be stored.
   * 
   * @param id The id for the orders selected.
   * @param tableNum The table number for the orders selected.
   * @param price The price of the order.
   */
  public OrdersToPayData(int id, int tableNum, Price price) {
    this.id = id;
    this.tableNumber = tableNum;
    this.price = price;
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
   * Gets the price of the order from table data.
   * 
   * @return Returns the price of the order
   */
  public Price getPrice() {
    return this.price;
  }
}
