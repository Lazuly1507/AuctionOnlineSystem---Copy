package app.common.dto;

import java.util.List;

/** UserListResponse. */
public record UserListResponse(List<UserData> users) implements Response {}
