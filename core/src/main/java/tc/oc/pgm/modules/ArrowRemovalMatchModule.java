package tc.oc.pgm.modules;

import static tc.oc.pgm.util.bukkit.MiscUtils.MISC_UTILS;

import java.util.concurrent.TimeUnit;
import org.bukkit.entity.Projectile;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.util.TimeUtils;

/** Reduce the lifespan of infinity arrows */
public class ArrowRemovalMatchModule implements MatchModule {

  private final Match match;
  private final long maxTicks;

  public ArrowRemovalMatchModule(Match match) {
    this.match = match;
    this.maxTicks = TimeUtils.toTicks(30, TimeUnit.SECONDS);
  }

  @Override
  public void enable() {
    match
        .getExecutor(MatchScope.RUNNING)
        .scheduleWithFixedDelay(this::removeOldArrows, 0, 30, TimeUnit.SECONDS);
  }

  private void removeOldArrows() {
    for (Projectile projectile : match.getWorld().getEntitiesByClass(Projectile.class)) {
      if (MISC_UTILS.isArrow(projectile.getClass()) && projectile.getTicksLived() >= maxTicks) {
        projectile.remove();
      }
    }
  }
}
