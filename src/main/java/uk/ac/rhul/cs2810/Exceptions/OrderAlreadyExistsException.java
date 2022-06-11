package uk.ac.rhul.cs2810.Exceptions;

/**
 * Used to represent when an order already exists in the database and so can't be added.
 */
public class OrderAlreadyExistsException extends Throwable {
  private final int orderID;
  
  /**
   * Instantiates a new Order already exists exception with a message and the id of the order
   * that caused the issue.
   *
   * @param message the message explaining the error
   * @param id      the id of the order
   */
  public OrderAlreadyExistsException(String message, int id) {
    super(message);
    orderID = id;
  }
  
  /**
   * Gets the id of the order causing the issue.
   *
   * @return the order id
   */
  public int getOrderID() {
    return orderID;
  }
}
