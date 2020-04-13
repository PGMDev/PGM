package tc.oc.pgm.shield;

import java.time.Duration;

public class ShieldParameters {

  public static final double DEFAULT_HEALTH = 4d;
  public static final Duration DEFAULT_DELAY = Duration.ofSeconds(4);

  public final double maxHealth;
  public final Duration rechargeDelay;

  public ShieldParameters(double maxHealth, Duration rechargeDelay) {
    this.maxHealth = maxHealth;
    this.rechargeDelay = rechargeDelay;
  }
}
