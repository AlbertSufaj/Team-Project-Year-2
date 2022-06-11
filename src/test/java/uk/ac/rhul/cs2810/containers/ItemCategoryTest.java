package uk.ac.rhul.cs2810.containers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemCategoryTest {
  @Test
  void testStringToCategory() {
    assertEquals(ItemCategory.BURRITOS, ItemCategory.toCategory("burritos"));
  }
}