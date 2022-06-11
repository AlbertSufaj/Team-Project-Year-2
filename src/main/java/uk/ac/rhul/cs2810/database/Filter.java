package uk.ac.rhul.cs2810.database;

import java.util.Objects;
import java.util.Set;

/**
 * Represents the filter used when getting the menu
 */
public class Filter {
  private final boolean isVegi;
  private final boolean isVegan;
  private final boolean isGlutenFree;
  private Set<String> allergens;
  
  /**
   * Creates a new filter.
   *
   * @param allergens    the set of allergens to not allow
   * @param isVegi       only allow vegetarian items
   * @param isVegan      only allow vegan items
   * @param isGlutenFree only allow gluten free items
   */
  public Filter(Set<String> allergens, boolean isVegi, boolean isVegan, boolean isGlutenFree) {
    this.isVegi = isVegi;
    this.isVegan = isVegan;
    this.isGlutenFree = isGlutenFree;
    
    this.allergens = allergens;
  }
  
  /**
   * Does the filter only allow vegetarian items.
   *
   * @return the boolean
   */
  public boolean isVegi() {
    return isVegi;
  }
  
  /**
   * Does the filter only allow vegan items.
   *
   * @return the boolean
   */
  public boolean isVegan() {
    return isVegan;
  }
  
  /**
   * Does the filter only allow gluten free items.
   *
   * @return the boolean
   */
  public boolean isGlutenFree() {
    return isGlutenFree;
  }
  
  /**
   * Gets the allergens to filter out
   *
   * @return the allergens
   */
  public Set<String> getAllergens() {
    return allergens;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Filter filter = (Filter) o;
    return isVegi == filter.isVegi && isVegan == filter.isVegan &&
        isGlutenFree == filter.isGlutenFree && allergens.equals(filter.allergens);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(isVegi, isVegan, isGlutenFree, allergens);
  }
}
