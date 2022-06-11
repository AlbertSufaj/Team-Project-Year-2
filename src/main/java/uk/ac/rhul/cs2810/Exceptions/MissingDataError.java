package uk.ac.rhul.cs2810.Exceptions;

/**
 * An error thrown when there is missing data in the database
 */
public class MissingDataError extends Exception {
  
  /**
   * Instantiates a new missing data error.
   * 
   * @param message the message contained in the error.
   */
  public MissingDataError (String message) {
    super(message);
  }
}
