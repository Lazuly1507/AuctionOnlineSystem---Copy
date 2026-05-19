package app.server.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.TestFixtures;
import app.common.enums.ItemType;
import app.common.enums.UserRole;
import app.common.models.Item;
import app.common.models.User;
import app.server.dao.impl.MySqlItemDAO;
import app.server.dao.impl.MySqlUserDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MySqlItemDAOTest extends BaseDAOTest {
  private UserDAO userDAO;
  private ItemDAO itemDAO;
  private User seller;

  @BeforeEach
  void setUp() {
    userDAO = new MySqlUserDAO();
    itemDAO = new MySqlItemDAO();
    seller = userDAO.save(TestFixtures.user(TestFixtures.unique("seller"), UserRole.SELLER));
  }

  @Test
  void save_shouldPersistNotDeletedItem() {
    Item saved = itemDAO.save(TestFixtures.item(seller.getId(), "Laptop", ItemType.ELECTRONICS));

    assertTrue(saved.getId() > 0);
    Item found = itemDAO.findById(saved.getId()).orElseThrow();
    assertEquals("Laptop", found.getName());
    assertEquals(seller.getId(), found.getSellerId());
    assertEquals(ItemType.ELECTRONICS, found.getType());
    assertFalse(found.isDeleted());
  }

  @Test
  void findBySeller_shouldReturnOnlySellerItems() {
    User otherSeller =
        userDAO.save(TestFixtures.user(TestFixtures.unique("other_seller"), UserRole.SELLER));
    Item sellerItem =
        itemDAO.save(TestFixtures.item(seller.getId(), "Camera", ItemType.ELECTRONICS));
    itemDAO.save(TestFixtures.item(otherSeller.getId(), "Painting", ItemType.ART));

    var items = itemDAO.findBySeller(seller.getId());

    assertEquals(1, items.size());
    assertEquals(sellerItem.getId(), items.getFirst().getId());
  }

  @Test
  void findByCategoryAndAvailable_shouldFilterItems() {
    Item phone = itemDAO.save(TestFixtures.item(seller.getId(), "Phone", ItemType.ELECTRONICS));
    Item painting = itemDAO.save(TestFixtures.item(seller.getId(), "Painting", ItemType.ART));
    painting.setDeleted(true);
    itemDAO.update(painting);

    var electronics = itemDAO.findByCategory(ItemType.ELECTRONICS);
    var available = itemDAO.findAvailable();

    assertEquals(1, electronics.size());
    assertEquals(phone.getId(), electronics.getFirst().getId());
    assertEquals(1, available.size());
    assertEquals(phone.getId(), available.getFirst().getId());
  }

  @Test
  void update_shouldPersistEditableFieldsAndDeletedFlag() {
    Item saved = itemDAO.save(TestFixtures.item(seller.getId(), "Old name", ItemType.ELECTRONICS));
    saved.setName("New name");
    saved.setDescription("Updated description");
    saved.setStartingPrice(2000L);
    saved.setStepPrice(200L);
    saved.setType(ItemType.VEHICLE);
    saved.setDeleted(true);

    itemDAO.update(saved);

    Item found = itemDAO.findById(saved.getId()).orElseThrow();
    assertEquals("New name", found.getName());
    assertEquals("Updated description", found.getDescription());
    assertEquals(2000L, found.getStartingPrice());
    assertEquals(200L, found.getStepPrice());
    assertEquals(ItemType.VEHICLE, found.getType());
    assertTrue(found.isDeleted());
  }
}
