package app.common.models;

import app.common.enums.ItemType;

/** Electronics. */
public class Electronics extends Item {
  /** Electronics. */
  public Electronics(
      String name, int sellerId, String description, Long startingPrice, Long stepPrice) {
    this(0, name, sellerId, description, startingPrice, stepPrice);
  }

  /** Electronics. */
  public Electronics(
      int id, String name, int sellerId, String description, Long startingPrice, Long stepPrice) {
    super(id, name, sellerId, description, startingPrice, stepPrice, ItemType.ELECTRONICS);
  }
}
