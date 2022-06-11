package uk.ac.rhul.cs2810.containers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents an item on the menu.
 */
public class Item {
  private final int id;
  private final String name;
  private final String desc;
  private final Price price;
  private final int calories;
  private final ItemCategory category;
  private final boolean vegi;
  private final boolean vegan;
  private final boolean glutenFree;
  private final List<String> allergens = new ArrayList<>();
  private final int stock;
  private final Price costPrice;
  
  /**
   * Instantiates a new Item.
   * 
   * @param id         the id of the item
   * @param name       the name of the item
   * @param desc       the item desc
   * @param price      the items price
   * @param calories   the number of calories in the item
   * @param category   the category the items in
   * @param vegi       Is the item vegetarian
   * @param vegan      Is the item vegan
   * @param glutenFree Is the item gluten free
   * @param stock      the item stock level
   * @param costPrice  the items cost price
   */
  public Item(int id, String name, String desc, int price, int calories, ItemCategory category,
              boolean vegi, boolean vegan, boolean glutenFree, int stock, int costPrice) {
    this.id = id;
    this.name = name;
    this.desc = desc;
    this.price = new Price(price);
    this.calories = calories;
    this.category = category;
    this.vegi = vegi;
    this.vegan = vegan;
    this.glutenFree = glutenFree;
    this.stock = stock;
    this.costPrice = new Price(costPrice);
  }
  
  /**
   * Adds an allergen to the list of allergens in the item.
   *
   * @param allergen the allergen to add
   */
  public void addAllergen(String allergen) {
    if (!allergens.contains(allergen)) {
      allergens.add(allergen);
    }
  }
  
  /**
   * Gets a list of the allergens contained in the Item.
   *
   * @return the list of allergens
   */
  public List<String> getAllergens() {
    return allergens;
  }
  
  /**
   * Gets the items id.
   *
   * @return the id
   */
  public int getID() {
    return id;
  }
  
  /**
   * Gets the items name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }
  
  /**
   * Gets the items category.
   *
   * @return the category
   */
  public ItemCategory getCategory() {
    return category;
  }
  
  /**
   * Gets if the item is vegetarian
   *
   * @return Is the item vegetarian
   */
  public boolean isVegi() {
    return vegi;
  }
  
  /**
   * Gets if the item is vegan.
   *
   * @return Is the item vegan
   */
  public boolean isVegan() {
    return vegan;
  }
  
  /**
   * Is the item gluten free?
   *
   * @return Is the item gluten free
   */
  public boolean isGlutenFree() {
    return glutenFree;
  }
  
  /**
   * Returns a formatted string of the calories.
   *
   * @return the formatted string
   */
  public String caloriesFormat() {
    return String.format("%s%4d%s", "Calories: ", this.calories, " kcal");
  }
  
  /**
   * Returns the price of the item as type price.
   * 
   * @return the price of the item
   */
  public Price getPrice() {
    return this.price;
  }
  
  /**
   * Returns a formatted string of the allergens.
   *
   * @return the formatted string
   */
  public String allergenFormat() {
    if (this.allergens.isEmpty()) {
      return "Allergens: None";
    } else {
      String allAllergens = "";
      for (String allergens : this.allergens) {
        allAllergens += allergens + ", ";
      }
      
      return "Allergens: " + allAllergens.subSequence(0, allAllergens.length() - 2);
    }
  }
  
  @Override
  public String toString() {
    return String.format("%-20s\t%s\n%-100s\n%s\n%s", this.name, price.toString(),
        this.desc, this.caloriesFormat(), this.allergenFormat());
  }
  
  /**
   * Gets the item description.
   * 
   * @return the item description
   */
  public String getDescription() {
    return desc;
  }
  
  /**
   * Gets the stock level of the item.
   * 
   * @return the item stock level.
   */
  public int getStock() {
    return stock;
  }
  
  /**
   * Gets the number of calories in an item.
   * 
   * @return the item calories.
   */
  public int getCalories() {
    return calories;
  }
  
  /**
   * Gets the item cost price, as a price type.
   * 
   * @return the item cost price
   */
  public Price getCostPrice() {
    return costPrice;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Item item = (Item) o;
    return id == item.id;
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
  
}
