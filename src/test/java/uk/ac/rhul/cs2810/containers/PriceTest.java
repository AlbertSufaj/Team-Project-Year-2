package uk.ac.rhul.cs2810.containers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PriceTest {

  Price price;
  Price newPrice;

  @BeforeEach
  public void createPrice() {
    this.price = new Price(1200);
  }
  
  // Tests the price is printed formatted correctly
  @Test
  void testToString() {
    //assertEquals("£  12.00", price.toString());
  }
  
  // Tests getting the value of the price
  @Test
  void testGetPriceValue() {
    assertEquals(1200, price.getPriceValue());
  }
  
  // Tests adding prices together
  @Test
  void testAdd() {
    newPrice = new Price(1600);
    assertEquals(newPrice.toString(), price.add(400).toString());
  }
  
  // Tests subtracting prices from each other
  @Test
  void testSubtract() {
    newPrice = new Price(800);
    assertEquals(newPrice.toString(), price.subtract(400).toString());
  }

}
