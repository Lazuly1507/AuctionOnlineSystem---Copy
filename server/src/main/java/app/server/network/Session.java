package app.server.network;

import app.common.models.User;
import java.time.LocalDateTime;
import java.util.UUID;

/** Session. */
public class Session {
  private final String sessionId;
  private User user;
  private final LocalDateTime createdAt;
  private volatile LocalDateTime lastAccessTime;
  private volatile boolean authenticated;
  private volatile Integer viewingAuctionId;

  /** Session. */
  public Session() {
    this.sessionId = UUID.randomUUID().toString();
    this.createdAt = LocalDateTime.now();
    this.lastAccessTime = createdAt;
    this.authenticated = false;
    this.viewingAuctionId = null;
  }

  /** authenticate. */
  public void authenticate(User user) {
    this.user = user;
    this.authenticated = true;
    touch();
  }

  /** logout. */
  public void logout() {
    this.user = null;
    this.authenticated = false;
    this.viewingAuctionId = null;
    touch();
  }

  /** touch. */
  public void touch() {
    this.lastAccessTime = LocalDateTime.now();
  }

  public boolean isAuthenticated() {
    return authenticated && user != null;
  }

  public String getSessionId() {
    return sessionId;
  }

  public User getUser() {
    return user;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getLastAccessTime() {
    return lastAccessTime;
  }

  public Integer getViewingAuctionId() {
    return viewingAuctionId;
  }

  public void setViewingAuctionId(Integer viewingAuctionId) {
    this.viewingAuctionId = viewingAuctionId;
    touch();
  }
}
