package tc.oc.pgm.modules;

import java.util.concurrent.TimeUnit;
import org.bukkit.entity.Arrow;
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
    for (Arrow arrow : match.getWorld().getEntitiesByClass(Arrow.class)) {
      if (arrow.getTicksLived() >= maxTicks) arrow.remove();
    }
  }
}
