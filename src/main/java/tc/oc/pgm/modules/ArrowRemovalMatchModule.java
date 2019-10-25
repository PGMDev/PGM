package tc.oc.pgm.modules;

import org.bukkit.entity.Arrow;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import tc.oc.pgm.Config;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.match.MatchScope;
import tc.oc.world.NMSHacks;

/** Reduce the lifespan of infinity arrows */
@ListenerScope(MatchScope.RUNNING)
public class ArrowRemovalMatchModule extends MatchModule implements Listener, Runnable {

  private final long maxTicks;
  private BukkitTask task;

  public ArrowRemovalMatchModule(Match match) {
    super(match);
    this.maxTicks = Config.ArrowRemoval.delay() * 20;
  }

  @Override
  public void enable() {
    super.enable();
    this.task = getMatch().getScheduler(MatchScope.RUNNING).runTaskTimer(0, 20, this);
  }

  @Override
  public void disable() {
    if (this.task != null) {
      this.task.cancel();
      this.task = null;
    }
    super.disable();
  }

  @Override
  public void run() {
    for (Arrow arrow : getMatch().getWorld().getEntitiesByClass(Arrow.class)) {
      if (arrow.getTicksLived() >= this.maxTicks && NMSHacks.hasInfinityEnchanment(arrow))
        arrow.remove();
    }
  }
}
