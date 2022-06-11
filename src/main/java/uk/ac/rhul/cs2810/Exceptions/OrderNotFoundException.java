package uk.ac.rhul.cs2810.Exceptions;

/**
 * Used to represent when an order isn't in the database and so cannot be updated.
 */
public class OrderNotFoundException extends Exception {
  private final int id;
  
  /**
   * Instantiates a new Order not found exception with a message and the id of the order causing
   * the issue.
   *
   * @param message the message explaining the error
   * @param id      the id of the order
   */
  public OrderNotFoundException(String message, int id) {
    super(message);
    this.id = id;
  }
  
  /**
   * Gets the id of the order causing the issue.
   *
   * @return the order id
   */
  public int getOrderId() {
    return id;
  }
}
