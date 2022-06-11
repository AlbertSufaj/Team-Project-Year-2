package uk.ac.rhul.cs2810.containers;

/**
 * The data container for the stock level table view in manager.
 */
public class StockTableData {
  private int id;
  private String name;
  private Price price;
  private int stock;
  private String avgTime;

  /**
   * Constructor to initialise the data to be stored.
   * 
   * @param id The item id
   * @param name The item name
   * @param price The item price
   * @param stock The current stock level of the item
   * @param avgTime The average time taken for the item to be prepared
   */
  public StockTableData(int id, String name, Price price, int stock, float avgTime) {
    this.id = id;
    this.name = name;
    this.price = price;
    this.stock = stock;

    // Converts the average time in minutes to a formatted string
    int wholeMins = (int) Math.floor(avgTime);
    avgTime -= wholeMins;
    int seconds = (int) (60 * avgTime);

    this.avgTime =
        (String.format("%02d", wholeMins) + ":" + String.format("%02d", seconds) + " Minutes");
  }

  /**
   * Gets the item id from table data.
   * 
   * @return Returns the ID of the item
   */
  public int getID() {
    return this.id;
  }

  /**
   * Gets the item name from table data.
   * 
   * @return Returns the name of the item.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Gets the item price from table data.
   * 
   * @return Returns the price of the item.
   */
  public Price getPrice() {
    return this.price;
  }

  /**
   * Gets the current stock level of the item.
   * 
   * @return Returns the stock level of the item.
   */
  public int getStock() {
    return this.stock;
  }

  /**
   * Gets the average time taken for an individual item to be prepared by the kitchen
   * 
   * @return The average time, as a formatted string.
   */
  public String getAvgTime() {
    return this.avgTime;
  }
}
