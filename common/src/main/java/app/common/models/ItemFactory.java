package app.common.models;

import app.common.enums.ItemType;

/** ItemFactory. */
public class ItemFactory {
  /** createItem. */
  public static Item createItem(
      int id,
      String name,
      int sellerId,
      String description,
      Long startingPrice,
      Long stepPrice,
      ItemType type) {
    return switch (type) {
      case ELECTRONICS ->
          new Electronics(id, name, sellerId, description, startingPrice, stepPrice);
      case ART -> new Art(id, name, sellerId, description, startingPrice, stepPrice);
      case VEHICLE -> new Vehicle(id, name, sellerId, description, startingPrice, stepPrice);
    };
  }

  /** createItem. */
  public static Item createItem(
      String name,
      int sellerId,
      String description,
      Long startingPrice,
      Long stepPrice,
      ItemType type) {
    return switch (type) {
      case ELECTRONICS -> new Electronics(name, sellerId, description, startingPrice, stepPrice);
      case ART -> new Art(name, sellerId, description, startingPrice, stepPrice);
      case VEHICLE -> new Vehicle(name, sellerId, description, startingPrice, stepPrice);
    };
  }
}
