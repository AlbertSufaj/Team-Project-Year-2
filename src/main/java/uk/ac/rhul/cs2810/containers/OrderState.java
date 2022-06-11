package uk.ac.rhul.cs2810.containers;

/**
 * Represents the state of a given order.
 */
public enum OrderState {
  SERVED("Not Paid"), 
  PREPARING("Preparing"), 
  CANCELLED("Cancelled"),
  UNCONFIRMED("Unconfirmed"), 
  CONFIRMED("Confirmed"), 
  READY("Ready");
  
  String strval;
  
  /**
   * Sets the string value of the order state.
   * 
   * @param str The order state as a string.
   */
  OrderState(String str) {
    this.strval = str;
  }
  
  /**
   * Returns the strong value of the order state.
   */
  public String toString() {
    return this.strval;
  }
}