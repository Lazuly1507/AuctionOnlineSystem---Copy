package app.common.models;

import app.common.enums.AuctionStatus;
import java.time.LocalDateTime;
import java.util.Objects;

/** Auction. */
public class Auction {
  private int id;
  private final int itemId;
  private final int sellerId;
  private Integer winnerId;
  private AuctionStatus status;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private long highestBid;
  private int extendedCount;
  private int version;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String itemName;

  /** Auction. */
  public Auction(int itemId, int sellerId, LocalDateTime endTime, long currentPrice) {
    if (itemId <= 0) {
      throw new IllegalArgumentException("Item id không hợp lệ.");
    }
    if (sellerId <= 0) {
      throw new IllegalArgumentException("Seller id không hợp lệ.");
    }
    this.itemId = itemId;
    this.sellerId = sellerId;
    this.endTime = Objects.requireNonNull(endTime, "endTime");
    this.status = AuctionStatus.OPEN;
    this.highestBid = nonNegative(currentPrice, "highestBid");
    this.extendedCount = 0;
    this.version = 0;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = this.createdAt;
  }

  /** Auction. */
  public Auction(
      int id,
      int itemId,
      int sellerId,
      Integer winnerId,
      AuctionStatus status,
      LocalDateTime startTime,
      LocalDateTime endTime,
      long highestBid,
      int extendedCount,
      int version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = nonNegative(id, "id");
    this.itemId = nonNegative(itemId, "itemId");
    this.sellerId = nonNegative(sellerId, "sellerId");
    this.winnerId = positiveOrNull(winnerId, "winnerId");
    this.status = status;
    this.startTime = startTime;
    this.endTime = endTime;
    this.highestBid = nonNegative(highestBid, "highestBid");
    this.extendedCount = nonNegative(extendedCount, "extendedCount");
    this.version = nonNegative(version, "version");
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /** start. */
  public void start() {
    if (this.status != AuctionStatus.OPEN) {
      throw new IllegalStateException("Chỉ có thể bắt đầu phiên đang OPEN, hiện tại: " + status);
    }
    if (this.endTime == null) {
      throw new IllegalStateException("Không thể bắt đầu phiên chưa có thời gian kết thúc.");
    }
    this.status = AuctionStatus.RUNNING;
    this.startTime = LocalDateTime.now();
    touchUpdatedAt();
  }

  /** finish. */
  public void finish() {
    if (this.status != AuctionStatus.OPEN && this.status != AuctionStatus.RUNNING) {
      throw new IllegalStateException(
          "Chỉ có thể kết thúc phiên đang OPEN hoặc RUNNING, hiện tại: " + status);
    }
    this.status = AuctionStatus.FINISHED;
    touchUpdatedAt();
  }

  /** finish. */
  public void finish(Integer winnerId) {
    if (winnerId != null && winnerId <= 0) {
      throw new IllegalArgumentException("Winner id không hợp lệ.");
    }
    finish();
    if (winnerId != null) {
      this.winnerId = winnerId;
    }
  }

  /** markPaid. */
  public void markPaid() {
    if (this.status != AuctionStatus.FINISHED) {
      throw new IllegalStateException("Chỉ có thể PAID khi FINISHED, hiện tại: " + status);
    }
    this.status = AuctionStatus.PAID;
    touchUpdatedAt();
  }

  /** cancel. */
  public void cancel() {
    if (this.status == AuctionStatus.FINISHED || this.status == AuctionStatus.PAID) {
      throw new IllegalStateException("Không thể huỷ phiên đã " + status);
    }
    if (this.status == null) {
      throw new IllegalStateException("Không thể huỷ phiên chưa có trạng thái.");
    }
    this.status = AuctionStatus.CANCELED;
    touchUpdatedAt();
  }

  /** updateHighestBid. */
  public void updateHighestBid(long newBid, int bidderId) {
    if (newBid <= this.highestBid) {
      throw new IllegalArgumentException("Giá mới phải cao hơn giá hiện tại: " + highestBid);
    }
    if (bidderId <= 0) {
      throw new IllegalArgumentException("Bidder id không hợp lệ.");
    }
    this.highestBid = newBid;
    this.winnerId = bidderId;
    touchUpdatedAt();
  }

  /** Anti-sniping: gia hạn thêm extraSeconds nếu bid trong X giây cuối. */
  public void extend(int extraSeconds) {
    if (extraSeconds <= 0) {
      throw new IllegalArgumentException("Thời gian gia hạn phải là số dương.");
    }
    if (this.status != AuctionStatus.RUNNING) {
      throw new IllegalStateException("Chỉ gia hạn khi đang RUNNING");
    }
    if (this.endTime == null) {
      throw new IllegalStateException("Không thể gia hạn phiên chưa có thời gian kết thúc.");
    }
    this.endTime = this.endTime.plusSeconds(extraSeconds);
    this.extendedCount++;
    touchUpdatedAt();
  }

  public boolean isExpired() {
    return endTime != null && LocalDateTime.now().isAfter(this.endTime);
  }

  public boolean isRunning() {
    return this.status == AuctionStatus.RUNNING;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = nonNegative(id, "id");
  }

  public int getItemId() {
    return itemId;
  }

  public int getSellerId() {
    return sellerId;
  }

  public Integer getWinnerId() {
    return winnerId;
  }

  public void setWinnerId(Integer winnerId) {
    this.winnerId = positiveOrNull(winnerId, "winnerId");
    touchUpdatedAt();
  }

  public AuctionStatus getStatus() {
    return status;
  }

  public void setStatus(AuctionStatus status) {
    this.status = Objects.requireNonNull(status, "status");
    touchUpdatedAt();
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
    touchUpdatedAt();
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = Objects.requireNonNull(endTime, "endTime");
    touchUpdatedAt();
  }

  public long getHighestBid() {
    return highestBid;
  }

  public void setHighestBid(long highestBid) {
    this.highestBid = nonNegative(highestBid, "highestBid");
    touchUpdatedAt();
  }

  public int getExtendedCount() {
    return extendedCount;
  }

  public void setExtendedCount(int extendedCount) {
    this.extendedCount = nonNegative(extendedCount, "extendedCount");
    touchUpdatedAt();
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = nonNegative(version, "version");
  }

  /** incrementVersion. */
  public void incrementVersion() {
    this.version++;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getItemName() {
    return itemName;
  }

  public void setItemName(String itemName) {
    this.itemName = itemName;
  }

  private void touchUpdatedAt() {
    this.updatedAt = LocalDateTime.now();
  }

  private static int nonNegative(int value, String field) {
    if (value < 0) {
      throw new IllegalArgumentException(field + " không được âm.");
    }
    return value;
  }

  private static long nonNegative(long value, String field) {
    if (value < 0) {
      throw new IllegalArgumentException(field + " không được âm.");
    }
    return value;
  }

  private static Integer positiveOrNull(Integer value, String field) {
    if (value != null && value <= 0) {
      throw new IllegalArgumentException(field + " không hợp lệ.");
    }
    return value;
  }

  @Override
  public String toString() {
    return "Auction{"
        + "id="
        + id
        + ", itemId="
        + itemId
        + ", sellerId="
        + sellerId
        + ", winnerId="
        + winnerId
        + ", status="
        + status
        + ", startTime="
        + startTime
        + ", endTime="
        + endTime
        + ", highestBid="
        + highestBid
        + ", extendedCount="
        + extendedCount
        + ", version="
        + version
        + '}';
  }
}
