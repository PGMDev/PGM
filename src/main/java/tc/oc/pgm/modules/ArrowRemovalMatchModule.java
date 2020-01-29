package tc.oc.pgm.modules;

import org.bukkit.entity.Arrow;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.world.NMSHacks;

/** Reduce the lifespan of infinity arrows */
public class ArrowRemovalMatchModule implements MatchModule {

  private final Match match;
  private final long maxTicks;

  public ArrowRemovalMatchModule(Match match) {
    this.match = match;
    this.maxTicks = Config.ArrowRemoval.delay() * 20;
  }

  @Override
  public void enable() {
    match
        .getScheduler(MatchScope.RUNNING)
        .runTaskTimer(
            0,
            20,
            () -> {
              for (Arrow arrow : match.getWorld().getEntitiesByClass(Arrow.class)) {
                if (arrow.getTicksLived() >= maxTicks && NMSHacks.hasInfinityEnchanment(arrow))
                  arrow.remove();
              }
            });
  }
}
