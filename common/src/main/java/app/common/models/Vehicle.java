package app.common.models;

import app.common.enums.ItemType;

/** Vehicle. */
public class Vehicle extends Item {
  /** Vehicle. */
  public Vehicle(
      String name, int sellerId, String description, Long startingPrice, Long stepPrice) {
    this(0, name, sellerId, description, startingPrice, stepPrice);
  }

  /** Vehicle. */
  public Vehicle(
      int id, String name, int sellerId, String description, Long startingPrice, Long stepPrice) {
    super(id, name, sellerId, description, startingPrice, stepPrice, ItemType.VEHICLE);
  }
}
