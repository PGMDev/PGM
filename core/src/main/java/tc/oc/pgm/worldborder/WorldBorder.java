package tc.oc.pgm.worldborder;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.time.Duration;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.filters.matcher.StaticFilter;

/** Parameters for the world border. */
public class WorldBorder {
  final Filter filter; // State should only be applied when a MatchQuery passes this filter
  final Vector center; // Center of the border
  final double size; // Diameter of the border
  final Duration duration; // Time taken to transition into this state from any previous state
  final double
      damage; // Damage per second dealt to players for each meter outside of the border the are
  // located
  final double buffer; // Distance from the edge of the border where the damage to players begins
  final double warningDistance; // Show red vignette to players closer than this to border
  final Duration
      warningTime; // Show red vignette to players when the border is moving and will reach them
  // within this time

  public WorldBorder(
      Filter filter,
      Vector center,
      double size,
      Duration duration,
      double damage,
      double buffer,
      double warningDistance,
      Duration warningTime) {
    this.filter = assertNotNull(filter);
    this.center = assertNotNull(center);
    this.size = size;
    this.duration = assertNotNull(duration);
    this.damage = damage;
    this.buffer = buffer;
    this.warningDistance = warningDistance;
    this.warningTime = assertNotNull(warningTime);
  }

  public boolean isMoving() {
    return !Duration.ZERO.equals(duration);
  }

  public boolean isConditional() {
    return filter != StaticFilter.ALLOW;
  }

  public void apply(org.bukkit.WorldBorder bukkit, boolean transition) {
    bukkit.setDamageAmount(damage);
    bukkit.setDamageBuffer(buffer);
    bukkit.setWarningDistance((int) Math.round(warningDistance));
    bukkit.setWarningTime((int) warningTime.getSeconds());
    bukkit.setCenter(center.getX(), center.getZ());

    if (transition && isMoving()) {
      bukkit.setSize(size, Math.max(0, duration.getSeconds()));
    } else {
      bukkit.setSize(size);
    }
  }

  public void refresh(org.bukkit.WorldBorder bukkit, Duration elapsed) {
    if (isMoving()) {
      bukkit.setSize(size, Math.max(0, duration.minus(elapsed).getSeconds()));
    }
  }
}
