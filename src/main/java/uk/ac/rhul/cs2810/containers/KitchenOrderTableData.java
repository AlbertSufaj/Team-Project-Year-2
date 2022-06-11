package uk.ac.rhul.cs2810.containers;

/**
 * The data container for the kitchen table view.
 */
public class KitchenOrderTableData {
  private int id;
  private String time;
  private OrderState status;
  private String items;
  
  /**
   * Instantiate a new order item to add to the UI.
   * 
   * @param id the order id
   * @param time the time the order was placed
   * @param state the order state
   * @param items the items contained within the order
   */
  public KitchenOrderTableData(int id, String time, OrderState state, String items) {
    this.id = id;
    this.time = time;
    this.status = state;
    this.items = items;
  }

  /**
   * Gets the order id from table data.
   * 
   * @return Returns the ID of the order
   */
  public int getId() {
    return this.id;
  }

  /**
   * Gets the time formatted for the order from table data.
   * 
   * @return Returns the formatted time
   */
  public String getTime() {
    return this.time;
  }

  /**
   * Gets the state of the order from table data.
   * 
   * @return Returns the status
   */
  public OrderState getStatus() {
    return this.status;
  }

  /**
   * Gets the items contained within the order from table data.
   * 
   * @return Returns the items as a formatted string
   */
  public String getItems() {
    return this.items;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (o.getClass() != this.getClass()) {
      return false;
    }

    KitchenOrderTableData kotd = (KitchenOrderTableData) o;
    return kotd.getId() == this.getId();
  }
}
