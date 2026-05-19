package app.common.dto;

import java.time.LocalDateTime;

/** ChatResponse. */
public record ChatResponse(int senderId, String sender, String content, LocalDateTime timestamp)
    implements Response {}
