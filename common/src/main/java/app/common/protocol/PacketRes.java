package app.common.protocol;

import app.common.dto.Response;
import app.common.enums.PacketType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** PacketRes. */
public class PacketRes {
  private static final Gson GSON = new GsonBuilder().create();
  private final boolean success;
  private final PacketType type;
  private final String message;
  private final String data;

  /** PacketRes. */
  public PacketRes(boolean success, PacketType type, String message, String data) {
    this.success = success;
    this.type = type;
    this.message = message;
    this.data = data;
  }

  public static PacketRes of(PacketType type, String message, Response payload) {
    return new PacketRes(true, type, message, toJson(payload));
  }

  /** error. */
  public static PacketRes error(PacketType type, String message) {
    return new PacketRes(false, type, message, null);
  }

  private static String toJson(Response payload) {
    if (payload == null) {
      return null;
    }
    return GSON.toJson(payload);
  }

  /** getData. */
  public <T extends Response> T getData(Class<T> clazz) {
    if (clazz == null || data == null) {
      return null;
    }
    return GSON.fromJson(data, clazz);
  }

  public boolean isSuccess() {
    return success;
  }

  public PacketType getType() {
    return type;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return GSON.toJson(this);
  }
}
