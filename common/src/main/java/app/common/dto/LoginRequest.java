package app.common.dto;

/** LoginRequest. */
public record LoginRequest(String username, String password) implements Request {}
