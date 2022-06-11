package uk.ac.rhul.cs2810.Exceptions;

/**
 * The type Invalid item exception.
 */
public class InvalidItemException extends Exception{
  private String badInput;
  private int maxLength;
  
  /**
   * Instantiates a new Invalid item exception.
   *
   * @param message   the message explaining the issue
   * @param cause     the cause of the exception
   * @param column    the column causing the issue
   * @param maxLength the max length of the column
   */
  public InvalidItemException(String message, Exception cause, String column, int maxLength){
    super(message, cause);
    this.badInput = column;
    this.maxLength = maxLength;
  }
  
  /**
   * Gets the name of the column with the bad input.
   *
   * @return the name of the column with the bad input
   */
  public String getBadInputColumn() {
    return badInput;
  }
  
  /**
   * Gets the max length of the invalid input.
   *
   * @return the max length
   */
  public int getMaxLength() {
    return maxLength;
  }
}
