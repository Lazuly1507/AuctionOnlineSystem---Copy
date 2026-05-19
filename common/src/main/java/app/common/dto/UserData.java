package app.common.dto;

import app.common.enums.UserRole;
import java.math.BigDecimal;
import java.util.Map;

/** UserData. */
public record UserData(
    int id,
    String name,
    String username,
    BigDecimal availableBalance,
    Map<String, BigDecimal> frozenFunds,
    UserRole role) {}
