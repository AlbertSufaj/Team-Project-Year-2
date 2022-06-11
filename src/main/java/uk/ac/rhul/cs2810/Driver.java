package uk.ac.rhul.cs2810;

import uk.ac.rhul.cs2810.users.JointStartMenu;

/**
 * Main driver class to launch the application.
 */
public class Driver {

  private static volatile JointStartMenu jsm;
  
  /**
   * Checks that the program isn't already running and starts it.
   * 
   * @return The running instance of the start menu.
   */
  private static JointStartMenu getInstance() {
    if (jsm == null) {
      jsm = new JointStartMenu(true);
    }
    return jsm;
  }
  
  /**
   * Opens the start menu.
   * 
   * @param args Args from launch
   */
  public static void main(String[] args) {
    getInstance();
  }
}