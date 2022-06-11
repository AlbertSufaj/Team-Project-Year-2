package uk.ac.rhul.cs2810.containers;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Represents an order to be processed by staff.
 */
public class Order {

  private LocalTime timeOfOrder = LocalTime.now();
  private LocalTime timeStatusChanged = LocalTime.now();
  private final int tableNumber;
  private List<Item> itemList = new LinkedList<>(); // Used a linked list as it is more efficient
  // for addition of data and access will mainly be iteration
  private OrderState state = OrderState.UNCONFIRMED;
  private int id = -1;

  /**
   * Create order with a list of items and a description.
   *
   * @param id the id
   * @param items list of items in the order.
   * @param tableNumber table number of order.
   * @param timeOfOrder the time of order
   * @param timeStatusChanged the time status changed
   */
  public Order(int id, List<Item> items, int tableNumber, LocalTime timeOfOrder,
      LocalTime timeStatusChanged) {
    this.id = id;
    this.itemList = items;
    this.tableNumber = tableNumber;
    this.timeOfOrder = timeOfOrder;
    this.timeStatusChanged = timeStatusChanged;
  }

  /**
   * Instantiates a new Order.
   *
   * @param tableNumber the table number
   */
  public Order(int tableNumber) {
    this.tableNumber = tableNumber;
  }

  /**
   * Instantiates a new Order.
   */
  public Order() {
    tableNumber = -1;
  }

  /**
   * Get the time at which the order was made.
   *
   * @return time of order
   */
  public LocalTime getTime() {
    return timeOfOrder;
  }

  /**
   * Convert the time an order was made to a printable format.
   *
   * @return a readable string
   */
  public String formatTime() {
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
    return getTime().format(dtf);
  }

  /**
   * Get the state of the order.
   *
   * @return order state to get
   */
  public OrderState getState() {
    return this.state;
  }

  /**
   * Set the state of the order.
   *
   * @param state the state to set
   */
  public void setState(OrderState state) {
    this.state = state;
  }

  /**
   * Set the time of the order.
   *
   * @param time the time to set
   */
  public void setTime(LocalTime time) {
    timeOfOrder = time;
  }

  /**
   * Adds the item passed from the current order.
   *
   * @param itemToAdd The item which needs to be added.
   */
  public void addItemToOrder(Item itemToAdd) {
    this.itemList.add(itemToAdd);
  }

  /**
   * Gets the current items in the order.
   *
   * @return Returns the current items list
   */
  public List<Item> getItemsInOrder() {
    return this.itemList;
  }

  /**
   * Gets a formatted list of items in the order.
   *
   * @return a formatted list
   */
  public String getItemsFormatted() {
    String formatted = "";
    String separator = "";
    int size = itemList.size();
    int iter = 0;
    if (size > 1) {
      separator = "\n\n";
    }
    while (iter < size) {
      formatted += itemList.get(iter).getName() + separator;
      iter++;
    }
    return formatted;
  }

  /**
   * Removes the item passed from the current order.
   *
   * @param itemToRemove The item which needs to be removed.
   */
  public void removeItemFromOrder(Item itemToRemove) {
    this.itemList.remove(itemToRemove);
  }

  /**
   * Get the ID of the order.
   *
   * @return the id
   */
  public int getID() {
    return this.id;
  }

  /**
   * Get the table number.
   *
   * @return the table number
   */
  public int getTableNumber() {
    return tableNumber;
  }

  /**
   * Get number of items in list.
   *
   * @return the item count
   */
  public int getItemCount() {
    return itemList.size();
  }


  /**
   * Gets a map of item to how many times there in the order.
   *
   * @return the map
   */
  public Map<Item, Integer> getOccuranceMap() {
    Set<Item> seenItems = new HashSet<>();
    Map<Item, Integer> occuranceMap = new HashMap<>();
    List<Item> items = getItemsInOrder();
    for (Item item : items) {
      if (seenItems.contains(item)) {
        continue;
      }
      seenItems.add(item);
      occuranceMap.put(item, Collections.frequency(items, item));
    }
    return occuranceMap;
  }

  /**
   * Check if two orders are equal.
   *
   * @param ord the other order
   * @return whether or not it is equal to the other order
   */
  public boolean equals(Order ord) {
    return this.getID() == ord.getID();
  }

  @Override
  public boolean equals(Object o) {
    if (o.getClass() != this.getClass()) {
      return false;
    }
    Order order = (Order) o;
    return order.equals(this);
  }

  /**
   * Sets id.
   *
   * @param id the id
   */
  public void setID(int id) {
    this.id = id;
  }

  /**
   * Gets time status changed.
   *
   * @return the time status changed
   */
  public LocalTime getTimeStatusChanged() {
    return timeStatusChanged;
  }

  /**
   * Sets time status changed.
   *
   * @param timeStatusChanged the time status changed
   */
  public void setTimeStatusChanged(LocalTime timeStatusChanged) {
    this.timeStatusChanged = timeStatusChanged;
  }

  /**
   * Get price price.
   *
   * @return the price
   */
  public Price getPrice() {
    Price price = new Price(0);
    for (Item item : itemList) {
      price = price.add(item.getPrice());
    }
    return price;
  }

  /**
   * Formats the id to display it.
   * 
   * @return The formatted id
   */
  private String formatID() {
    return String.format("%s%-4s", "ID: ", this.id);
  }

  /**
   * Formats the table number to display it.
   * 
   * @return The formatted table number
   */
  private String formatTableNum() {
    return String.format("%s%-2s", "Table Number: ", this.tableNumber);
  }

  @Override
  public String toString() {
    return (formatID() + " " + formatTableNum() + " " + formatTime());
  }
}
