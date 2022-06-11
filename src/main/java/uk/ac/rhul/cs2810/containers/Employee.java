package uk.ac.rhul.cs2810.containers;

import uk.ac.rhul.cs2810.Exceptions.ConnectionError;
import uk.ac.rhul.cs2810.Exceptions.ExecutionError;
import uk.ac.rhul.cs2810.database.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents the user using the system to allow management to see there details.
 */
public class Employee {
  private final int id;
  private final String name;
  private final LocalDate dateOfBirth;
  private final LocalDate dateOfHire;
  private int numOrders;
  private LocalTime timeOrdersLastUpdated;
  private int halfHoursWorked;

  private final int dataTimeToLive = 2000;
  private OrderDB orderDB;

  /**
   * Instantiates a new Employee.
   *
   * @param id the employees id
   * @param name the employees name
   * @param dateOfBirth the employees date of birth
   * @param dateOfHire the date the employee was hired
   */
  public Employee(int id, String name, LocalDate dateOfBirth, LocalDate dateOfHire, int numOrders,
      int halfHoursWorked) {
    this.id = id;
    this.name = name;
    this.dateOfBirth = dateOfBirth;
    this.dateOfHire = dateOfHire;
    this.numOrders = numOrders;
    this.halfHoursWorked = halfHoursWorked;

    this.timeOrdersLastUpdated = LocalTime.now();
  }

  /**
   * Gets the employees id.
   *
   * @return the id
   */
  public int getId() {
    return id;
  }

  /**
   * Gets the employees name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets employees date of birth.
   *
   * @return the date of birth
   */
  public LocalDate getDateOfBirth() {
    return dateOfBirth;
  }

  /**
   * Gets the date the employee was hired.
   *
   * @return the date of hire
   */
  public LocalDate getDateOfHire() {
    return dateOfHire;
  }

  /**
   * Gets the number of orders which have been assigned to the waiter.
   * 
   * @return The number of orders assigned.
   * @throws ConnectionError Thrown if it's not possible to connect to the database.
   * @throws ExecutionError Thrown if there is an error processing the data in the database.
   */
  public int getNumOrdersAssigned() throws ConnectionError, ExecutionError {
    if (orderDB == null) {
      orderDB = DatabaseFactory.getOrderDB();
    }

    // Keeps orderNumbers relatively upto data without constantly checking
    if (Duration.between(LocalTime.now(), timeOrdersLastUpdated).toMillis() > dataTimeToLive) {
      numOrders = orderDB.getOrdersAssignedToWaiter(id).size();
      timeOrdersLastUpdated = LocalTime.now();
    }
    return numOrders;
  }

  /**
   * The total number of half hours worked by the waiter.
   * 
   * @return The half hours worked by the employee.
   */
  public int getHalfHoursWorked() {
    return halfHoursWorked;
  }
}
