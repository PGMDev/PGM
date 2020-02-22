package tc.oc.pgm.shield;

import org.joda.time.Duration;

public class ShieldParameters {

  public static final double DEFAULT_HEALTH = 4d;
  public static final Duration DEFAULT_DELAY = Duration.standardSeconds(4);

  public final double maxHealth;
  public final Duration rechargeDelay;

  public ShieldParameters(double maxHealth, Duration rechargeDelay) {
    this.maxHealth = maxHealth;
    this.rechargeDelay = rechargeDelay;
  }
}
