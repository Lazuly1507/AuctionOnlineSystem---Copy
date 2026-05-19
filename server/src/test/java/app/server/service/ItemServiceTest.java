package app.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.TestFixtures;
import app.common.enums.ItemType;
import app.common.enums.UserRole;
import app.common.exception.ServiceException;
import app.common.models.Item;
import app.common.models.User;
import app.server.dao.AuctionDAO;
import app.server.dao.BaseDAOTest;
import app.server.dao.ItemDAO;
import app.server.dao.UserDAO;
import app.server.dao.impl.MySqlAuctionDAO;
import app.server.dao.impl.MySqlItemDAO;
import app.server.dao.impl.MySqlUserDAO;
import app.server.database.TransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemServiceTest extends BaseDAOTest {
  private ItemDAO itemDAO;
  private ItemService itemService;
  private User seller;

  @BeforeEach
  void setUp() {
    UserDAO userDAO = new MySqlUserDAO();
    itemDAO = new MySqlItemDAO();
    AuctionDAO auctionDAO = new MySqlAuctionDAO();
    itemService = new ItemService(itemDAO, auctionDAO, new TransactionManager());
    seller = userDAO.save(TestFixtures.user(TestFixtures.unique("seller"), UserRole.SELLER));
  }

  @Test
  void add_shouldPersistItem() {
    Item saved = itemService.add(TestFixtures.item(seller.getId(), "Laptop", ItemType.ELECTRONICS));

    assertTrue(saved.getId() > 0);
    Item found = itemService.getById(saved.getId()).orElseThrow();
    assertEquals("Laptop", found.getName());
    assertFalse(found.isDeleted());
  }

  @Test
  void update_shouldPersistItemChanges() {
    Item saved = itemService.add(TestFixtures.item(seller.getId(), "Old name", ItemType.ART));
    saved.setName("New name");
    saved.setDescription("New description");
    saved.setStartingPrice(2000L);
    saved.setStepPrice(250L);

    itemService.update(saved);

    Item found = itemService.getById(saved.getId()).orElseThrow();
    assertEquals("New name", found.getName());
    assertEquals("New description", found.getDescription());
    assertEquals(2000L, found.getStartingPrice());
    assertEquals(250L, found.getStepPrice());
  }

  @Test
  void updateManagedItem_shouldRejectNullItem() {
    assertThrows(
        ServiceException.class,
        () -> itemService.updateManagedItem(null, seller.getId(), seller.getRole()));
  }

  @Test
  void delete_shouldMarkItemAsDeleted() {
    Item saved = itemService.add(TestFixtures.item(seller.getId(), "Phone", ItemType.ELECTRONICS));

    itemService.delete(saved.getId());

    Item found = itemDAO.findById(saved.getId()).orElseThrow();
    assertTrue(found.isDeleted());
  }

  @Test
  void getAll_shouldReturnPersistedItems() {
    itemService.add(TestFixtures.item(seller.getId(), "Phone", ItemType.ELECTRONICS));
    itemService.add(TestFixtures.item(seller.getId(), "Bike", ItemType.VEHICLE));

    assertEquals(2, itemService.getAll().size());
  }
}
