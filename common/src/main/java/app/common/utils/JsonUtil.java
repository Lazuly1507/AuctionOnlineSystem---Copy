package app.common.utils;

import app.common.exception.AppException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/** JsonUtil. */
public class JsonUtil {
  private static final Gson GSON = new GsonBuilder().create();

  /** toJson. */
  public static String toJson(Object obj) {
    return GSON.toJson(obj);
  }

  /** fromJson. */
  public static <T> T fromJson(String json, Class<T> clazz) {
    try {
      return GSON.fromJson(json, clazz);
    } catch (JsonSyntaxException e) {
      System.err.println("Lỗi parse JSON từ String: " + e.getMessage());
      throw new AppException("Dữ liệu không hợp lệ");
    }
  }
}
