package uk.ac.rhul.cs2810.Exceptions;

/**
 * An error thrown when theres an issue connecting to the database.
 */
public class ConnectionError extends Exception {
  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the message contained in the error
   */
  public ConnectionError(String message) {
    super(message);
  }
  
  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message the message contained in the error
   * @param cause   the cause of the error
   */
  public ConnectionError(String message, Throwable cause) {
    super(message, cause);
  }
}
