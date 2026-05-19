package app.common.dto;

import app.common.enums.UserRole;

/** RegisterRequest. */
public record RegisterRequest(String name, String account, String password, UserRole role)
    implements Request {}
