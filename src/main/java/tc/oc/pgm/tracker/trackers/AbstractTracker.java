package tc.oc.pgm.tracker.trackers;

import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.damage.TrackerInfo;
import tc.oc.util.ClassLogger;

/**
 * Base class with a few convenience methods that are useful to trackers.
 *
 * <p>Subclasses specify the type of {@link TrackerInfo} they use and the resolve methods provided
 * by the base class will filter out results of the wrong type. If subclasses don't want results to
 * be filtered, they should call the resolve methods directly on the block/entity tracker.
 */
public abstract class AbstractTracker<Info extends TrackerInfo> implements Listener {

  protected final Logger logger;
  protected final Match match;
  protected final TrackerMatchModule tmm;
  private final Class<Info> infoClass;

  protected AbstractTracker(Class<Info> infoClass, TrackerMatchModule tmm, Match match) {
    this.infoClass = infoClass;
    this.tmm = tmm;
    this.logger = ClassLogger.get(match.getLogger(), getClass());
    this.match = match;
  }

  protected EntityTracker entities() {
    return tmm.getEntityTracker();
  }

  protected BlockTracker blocks() {
    return tmm.getBlockTracker();
  }

  protected @Nullable Info resolveBlock(Block block) {
    return blocks().resolveInfo(block, infoClass);
  }

  protected @Nullable Info resolveEntity(Entity entity) {
    return entities().resolveInfo(entity, infoClass);
  }
}
