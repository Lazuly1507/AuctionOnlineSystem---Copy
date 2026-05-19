package app.common.models;

import app.common.enums.ItemType;

/** Art. */
public class Art extends Item {
  /** Art. */
  public Art(String name, int sellerId, String description, Long startingPrice, Long stepPrice) {
    this(0, name, sellerId, description, startingPrice, stepPrice);
  }

  /** Art. */
  public Art(
      int id, String name, int sellerId, String description, Long startingPrice, Long stepPrice) {
    super(id, name, sellerId, description, startingPrice, stepPrice, ItemType.ART);
  }
}
