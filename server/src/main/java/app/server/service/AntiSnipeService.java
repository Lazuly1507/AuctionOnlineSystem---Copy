package app.server.service;

import app.common.models.Auction;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/** AntiSnipeService. */
public class AntiSnipeService {
  private static final int THRESHOLD_SECONDS = 30;
  private static final int EXTENSION_SECONDS = 60;
  private final Clock clock;

  /** AntiSnipeService. */
  public AntiSnipeService() {
    this.clock = Clock.systemDefaultZone();
  }

  /** apply. */
  public void apply(Auction auction) {
    if (auction.getEndTime() == null) {
      return;
    }
    long secondsLeft =
        Duration.between(LocalDateTime.now(clock), auction.getEndTime()).getSeconds();
    if (secondsLeft > 0 && secondsLeft <= THRESHOLD_SECONDS) {
      auction.extend(EXTENSION_SECONDS);
    }
  }
}
