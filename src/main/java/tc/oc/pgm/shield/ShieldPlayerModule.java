package tc.oc.pgm.shield;

import java.util.logging.Logger;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PotionEffectRemoveEvent;
import org.bukkit.potion.PotionEffectType;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.util.ClassLogger;
import tc.oc.util.TimeUtils;
import tc.oc.world.NMSHacks;

public class ShieldPlayerModule implements Tickable {

  private final Logger logger;
  private final MatchPlayer player;
  private final Player bukkit;
  private final ShieldParameters parameters;

  // Current shield strength, which may be less than the player's absorption
  // if they are getting some from a potion effect.
  private double shieldHealth;

  // Ticks until next shield recharge.
  private long rechargeTicks;

  public ShieldPlayerModule(Logger logger, MatchPlayer player, ShieldParameters parameters) {
    this.logger = ClassLogger.get(logger, getClass(), player.getBukkit().getName());
    this.player = player;
    this.bukkit = player.getBukkit();
    this.parameters = parameters;
    this.shieldHealth = parameters.maxHealth;
  }

  double getAbsorption() {
    return NMSHacks.getAbsorption(bukkit);
  }

  void setAbsorption(double absorption) {
    NMSHacks.setAbsorption(bukkit, absorption);
  }

  void addAbsorption(double delta) {
    if (delta != 0d) setAbsorption(getAbsorption() + delta);
  }

  public void apply() {
    addAbsorption(shieldHealth);
  }

  public void remove() {
    addAbsorption(-shieldHealth);
  }

  /**
   * Recharge the shield to its maximum health. If the player has more absorption than the current
   * shield strength, the excess is preserved.
   */
  void recharge() {
    if (shieldHealth < parameters.maxHealth) {
      double delta = parameters.maxHealth - shieldHealth;
      logger.fine("Recharging shield: shield=" + shieldHealth + " delta=" + delta);
      shieldHealth = parameters.maxHealth;
      addAbsorption(delta);
      bukkit.playSound(bukkit.getLocation(), Sound.ORB_PICKUP, 1, 2);
    }
  }

  void damage() {
    rechargeTicks = TimeUtils.toTicks(parameters.rechargeDelay);
  }

  @Override
  public void tick(Match match, Tick tick) {
    if (rechargeTicks > 0) {
      if (--rechargeTicks == 0) {
        recharge();
      }
    } else {
      // Detect shield damage, in case it somehow happens without firing an event.
      double absorption = getAbsorption();
      if (shieldHealth > absorption) {
        logger.fine(
            "Detected unexpected shield damage: shield="
                + shieldHealth
                + " absorption="
                + absorption);
        shieldHealth = absorption;
        damage();
      }
    }
  }

  void onEvent(EntityDamageEvent event) {
    if (event.getFinalDamage() > 0) {
      // Absorbed damage is applied to the shield before any potion effect
      double shieldDamage = -event.getDamage(EntityDamageEvent.DamageModifier.ABSORPTION);
      logger.fine(
          "Absorbing damage with shield: shield=" + shieldHealth + " shieldDamage=" + shieldDamage);
      shieldHealth = Math.max(0, shieldHealth - shieldDamage);

      damage();
    }
  }

  void onEvent(PotionEffectRemoveEvent event) {
    // The NMS code assumes that a potion effect is the only way to get
    // absorption hearts, so when the effect is removed, it simply removes
    // the same amount of absorption that it added initially. If any of that
    // eats into the shield, we refund the difference.
    if (PotionEffectType.ABSORPTION.equals(event.getEffect().getType())) {
      double newAbsorption =
          Math.max(0, getAbsorption() - 4 * (1 + event.getEffect().getAmplifier()));
      if (newAbsorption < shieldHealth) {
        logger.fine(
            "Compensating for removal of absorption "
                + event.getEffect().getAmplifier()
                + " effect, which will reduce absorption hearts to "
                + newAbsorption
                + ", which is below shield health of "
                + shieldHealth);
        addAbsorption(shieldHealth - newAbsorption);
      }
    }
  }
}
