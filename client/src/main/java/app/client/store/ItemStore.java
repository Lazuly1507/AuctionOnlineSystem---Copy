package app.client.store;

import app.common.models.Item;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemStore {
  private static volatile ItemStore instance;

  private final Map<Integer, Item> itemMap = new ConcurrentHashMap<>();

  private ItemStore() {}

  /** getInstance. */
  public static ItemStore getInstance() {
    if (instance == null) {
      synchronized (ItemStore.class) {
        if (instance == null) {
          instance = new ItemStore();
        }
      }
    }
    return instance;
  }

  public void addItem(Item item) {
    if (item == null) {
      return;
    }
    this.itemMap.put(item.getId(), item);
  }

  public Item getItem(int itemId) {
    return this.itemMap.get(itemId);
  }
}
