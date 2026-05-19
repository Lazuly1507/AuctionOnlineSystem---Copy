package app.server.service;

import app.common.enums.AuctionStatus;
import app.common.enums.UserRole;
import app.common.exception.ServiceException;
import app.common.models.Item;
import app.server.dao.AuctionDAO;
import app.server.dao.ItemDAO;
import app.server.database.TransactionManager;
import java.util.List;
import java.util.Optional;

/** ItemService. */
public class ItemService {
  private final ItemDAO itemDAO;
  private final AuctionDAO auctionDAO;
  private final TransactionManager transactionManager;

  /** ItemService. */
  public ItemService(ItemDAO itemDAO, TransactionManager transactionManager) {
    this(itemDAO, null, transactionManager);
  }

  /** ItemService. */
  public ItemService(
      ItemDAO itemDAO, AuctionDAO auctionDAO, TransactionManager transactionManager) {
    this.itemDAO = itemDAO;
    this.auctionDAO = auctionDAO;
    this.transactionManager = transactionManager;
  }

  /** add. */
  public Item add(Item item) {
    return transactionManager.runInTransaction(conn -> itemDAO.save(conn, item));
  }

  /** getById. */
  public Optional<Item> getById(int id) {
    return itemDAO.findById(id);
  }

  /** update. */
  public void update(Item item) {
    transactionManager.runInTransaction(
        conn -> {
          itemDAO.update(conn, item);
          return null;
        });
  }

  /** delete. */
  public void delete(int id) {
    transactionManager.runWithoutResult(
        conn ->
            itemDAO
                .findById(conn, id)
                .ifPresent(
                    item -> {
                      item.setDeleted(true);
                      itemDAO.update(conn, item);
                    }));
  }

  /** getSellerItems. */
  public List<Item> getSellerItems(int requesterId, UserRole requesterRole, int requestedSellerId) {
    if (requesterRole != UserRole.ADMIN && requesterId != requestedSellerId) {
      throw new ServiceException("Bạn không có quyền xem danh sách sản phẩm này.");
    }
    return itemDAO.findBySeller(requestedSellerId);
  }

  /** updateManagedItem. */
  public Item updateManagedItem(Item item, int requesterId, UserRole requesterRole) {
    if (item == null) {
      throw new ServiceException("Thông tin sản phẩm không hợp lệ.");
    }
    validateManageRequest(item.getId(), requesterId, requesterRole);
    validateItemData(item);
    return transactionManager.runInTransaction(
        conn -> {
          Item stored =
              itemDAO
                  .findById(conn, item.getId())
                  .orElseThrow(() -> new ServiceException("Không tìm thấy sản phẩm."));
          ensureManagePermission(stored, requesterId, requesterRole);
          ensureItemNotRunning(conn, stored.getId());
          stored.setName(item.getName());
          stored.setDescription(item.getDescription());
          stored.setStartingPrice(item.getStartingPrice());
          stored.setStepPrice(item.getStepPrice());
          stored.setType(item.getType());
          itemDAO.update(conn, stored);
          return stored;
        });
  }

  /** softDeleteManagedItem. */
  public Item softDeleteManagedItem(int itemId, int requesterId, UserRole requesterRole) {
    validateManageRequest(itemId, requesterId, requesterRole);
    return transactionManager.runInTransaction(
        conn -> {
          Item stored =
              itemDAO
                  .findById(conn, itemId)
                  .orElseThrow(() -> new ServiceException("Không tìm thấy sản phẩm."));
          ensureManagePermission(stored, requesterId, requesterRole);
          ensureItemNotRunning(conn, itemId);
          stored.setDeleted(true);
          itemDAO.update(conn, stored);
          return stored;
        });
  }

  public List<Item> getAll() {
    return itemDAO.findAll();
  }

  private void validateManageRequest(int itemId, int requesterId, UserRole requesterRole) {
    if (itemId <= 0 || requesterId <= 0 || requesterRole == null) {
      throw new ServiceException("Dữ liệu sản phẩm không hợp lệ.");
    }
    if (requesterRole != UserRole.SELLER && requesterRole != UserRole.ADMIN) {
      throw new ServiceException("Bạn không có quyền quản lý sản phẩm.");
    }
  }

  private void validateItemData(Item item) {
    if (item == null
        || item.getName() == null
        || item.getName().isBlank()
        || item.getDescription() == null
        || item.getDescription().isBlank()
        || item.getStartingPrice() <= 0
        || item.getStepPrice() <= 0
        || item.getType() == null) {
      throw new ServiceException("Thông tin sản phẩm không hợp lệ.");
    }
  }

  private void ensureManagePermission(Item item, int requesterId, UserRole requesterRole) {
    if (requesterRole != UserRole.ADMIN && item.getSellerId() != requesterId) {
      throw new ServiceException("Bạn không có quyền quản lý sản phẩm này.");
    }
  }

  private void ensureItemNotRunning(java.sql.Connection conn, int itemId) {
    if (auctionDAO == null) {
      return;
    }
    boolean running =
        auctionDAO.findByItemId(conn, itemId).stream()
            .anyMatch(auction -> auction.getStatus() == AuctionStatus.RUNNING);
    if (running) {
      throw new ServiceException("Không thể sửa/xóa sản phẩm đang đấu giá.");
    }
  }
}
