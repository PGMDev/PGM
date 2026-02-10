package tc.oc.pgm.shield;

import java.time.Duration;

public record ShieldParameters(double maxHealth, Duration rechargeDelay) {
  public static final double DEFAULT_HEALTH = 4d;
  public static final Duration DEFAULT_DELAY = Duration.ofSeconds(4);
}
