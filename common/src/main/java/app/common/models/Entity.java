package app.common.models;

import java.time.LocalDateTime;
import java.util.Objects;

/** Entity. */
public abstract class Entity {
  protected int id;
  protected LocalDateTime createdAt;
  protected LocalDateTime updatedAt;

  /** Entity. */
  public Entity(int id) {
    this.id = id;
    this.createdAt = LocalDateTime.now();
  }

  /** Entity. */
  public Entity() {
    this.createdAt = LocalDateTime.now();
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Entity entity = (Entity) o;
    return id > 0 && id == entity.id;
  }

  @Override
  public int hashCode() {
    if (id <= 0) {
      return System.identityHashCode(this);
    }
    return Objects.hash(getClass(), id);
  }
}
