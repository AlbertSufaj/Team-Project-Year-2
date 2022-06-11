package uk.ac.rhul.cs2810.Exceptions;

/**
 * Contains errors when executing postgres commands
 */
public class ExecutionError extends Exception {
  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the message to show the user.
   */
  public ExecutionError(String message) {
    super(message);
  }
  
  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message the message to show the user.
   * @param cause   the cause of the error
   */
  public ExecutionError(String message, Throwable cause) {
    super(message, cause);
  }
  
}
