package app.common.protocol;

import app.common.dto.Request;
import app.common.enums.PacketType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** PacketReq. */
public class PacketReq {
  private static final Gson GSON = new GsonBuilder().create();
  private PacketType type;
  private String data;

  /** PacketReq. */
  public PacketReq(PacketType type, String data) {
    this.type = type;
    this.data = data;
  }

  /** getData. */
  public <T extends Request> T getData(Class<T> clazz) {
    if (clazz == null || data == null) {
      return null;
    }
    return GSON.fromJson(data, clazz);
  }

  /** of. */
  public static PacketReq of(PacketType type, Request payload) {
    return new PacketReq(type, payload == null ? null : GSON.toJson(payload));
  }

  public PacketType getType() {
    return type;
  }
}
