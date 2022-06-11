package uk.ac.rhul.cs2810.containers;

/**
 * A container for item prices which formats the integer prices as strings.
 */
public class Price {

  private int pricePence;
  private int pricePounds;
  private int price;

  /**
   * Takes the price and converts it into two integers, one for pounds one for pence.
   * 
   * @param price The price passed in pence.
   */
  public Price(int price) {
    this.price = price;
    this.pricePence = price % 100;
    this.pricePounds = (price - this.pricePence) / 100;
  }

  @Override
  public String toString() {
    return String.format("%s%4d.%02d", "£", this.pricePounds, this.pricePence);
  }

  /**
   * Gets the price of the item as an integer value.
   * 
   * @return The price as an integer value.
   */
  public int getPriceValue() {
    return this.price;
  }

  /**
   * Adds the price stored to the number passed.
   * 
   * @param priceToBeAdded The price to be added.
   * @return The new total.
   */
  public Price add(int priceToBeAdded) {
    int newPrice = this.price + priceToBeAdded;
    Price returnPrice = new Price(newPrice);
    return returnPrice;
  }

  /**
   * Adds the price stored to the number passed.
   *
   * @param priceToBeAdded The price to be added.
   * @return The new total.
   */
  public Price add(Price priceToBeAdded) {
    int newPrice = this.price + priceToBeAdded.getPriceValue();
    Price returnPrice = new Price(newPrice);
    return returnPrice;
  }

  /**
   * Calls the add method with a negative version of the price to be added.
   * 
   * @param priceToBeSubtracted The price to be subtracted.
   * @return The new total.
   */
  public Price subtract(int priceToBeSubtracted) {
    return this.add(-1 * priceToBeSubtracted);
  }
  
  @Override
  public boolean equals(Object o) {
    Price price = (Price) o;
    return price.getPriceValue() == this.getPriceValue();
  }
}
