package uk.ac.rhul.cs2810.containers;

import java.time.LocalDate;

/**
 * The data container for the employee data table view in manager
 */
public class EmployeeTableData {
  private int id;
  private String name;
  private LocalDate dateOB;
  private LocalDate dateOH;
  private int numOrders;
  private String hoursWorked;

  /**
   * Constructor to initialise data stored.
   * 
   * @param id The waiter ID
   * @param name The waiter Name
   * @param dateOB The waiter date of birth
   * @param dateOH The waiter date of hire
   * @param numOrders The number of orders the waiter has completed.
   * @param hoursWorked The number of hours the waiter has worked.
   */
  public EmployeeTableData(int id, String name, LocalDate dateOB, LocalDate dateOH, int numOrders,
      int hoursWorked) {
    this.id = id;
    this.name = name;
    this.dateOB = dateOB;
    this.dateOH = dateOH;
    this.numOrders = numOrders;

    // Formats time worked
    int wholeHours = (int) hoursWorked / 2;
    int halfHours = hoursWorked % 2;
    this.hoursWorked = wholeHours + "." + halfHours + " Hours";
  }
  
  /**
   * Gets the employee id from table data.
   * 
   * @return Returns the ID of the employee
   */
  public int getID() {
    return this.id;
  }
  
  /**
   * Gets the employee name from table data.
   * 
   * @return Returns the employee name
   */
  public String getName() {
    return this.name;
  }
  
  /**
   * Gets the employee date of birth from table data.
   * 
   * @return Returns the employee DOB
   */
  public LocalDate getDateOB() {
    return this.dateOB;
  }
  
  /**
   * Gets the employee date of hire from table data.
   * 
   * @return Returns the employee DOH
   */
  public LocalDate getDateOH() {
    return this.dateOH;
  }
  
  /**
   * Gets the number of orders placed by the employee.
   * 
   * @return Returns the number of orders placed by the employee.
   */
  public int getNumOrders() {
    return this.numOrders;
  }
  
  /**
   * Returns the number of hours worked formatted as a string of hours.
   * 
   * @return Returns the number of hours worked as a formatted string.
   */
  public String getHoursWorked() {
    return this.hoursWorked;
  }

}
