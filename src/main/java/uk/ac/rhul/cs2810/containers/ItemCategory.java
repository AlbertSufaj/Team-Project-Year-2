package uk.ac.rhul.cs2810.containers;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the categories of items in the menu.
 */
public enum ItemCategory {
  
  SIDES,
  TACOS,
  QUESADILLAS,
  BURRITOS,
  ENCHILADAS,
  SALADS,
  EXTRAS,
  SOFTDRINKS,
  MOCKTAILS,
  BEERS,
  COCKTAILS,
  TEQUILAS,
  SPIRITS;
  
  private static final Map<String, ItemCategory> strToCategory = new HashMap<>();
  
  static {
    for (ItemCategory category : ItemCategory.values()) {
      strToCategory.put(category.toString().toLowerCase(), category);
    }
  }
  
  /**
   * Converts a string to the respective category.
   *
   * @param category the string to convert
   * @return the item category
   */
  public static ItemCategory toCategory(String category) {
    return strToCategory.get(category.toLowerCase());
  }
}
