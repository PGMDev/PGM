package tc.oc.pgm.modes;

import com.google.common.base.Preconditions;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchEvent;
import tc.oc.pgm.core.Core;
import tc.oc.pgm.destroyable.Destroyable;
import tc.oc.pgm.goals.GoalMatchModule;

public class ObjectiveModeChangeEvent extends MatchEvent {

  private final Mode mode;
  private String name;
  private static final HandlerList handlers = new HandlerList();

  public ObjectiveModeChangeEvent(Match match, final Mode mode) {
    super(match);
    this.mode = mode;

    if (this.mode.getName() != null) {
      this.name = this.mode.getName();
    } else {
      GoalMatchModule wins = getMatch().needModule(GoalMatchModule.class);
      Collection<Core> cores = wins.getGoals(Core.class).values();
      Collection<Destroyable> destroyables = wins.getGoals(Destroyable.class).values();

      if (cores.size() > destroyables.size() && cores.size() > 0) {
        this.name =
            cores.iterator().next().getModeChangeMessage(this.mode.getMaterialData().getItemType());
      } else if (destroyables.size() >= cores.size() && destroyables.size() > 0) {
        this.name =
            destroyables
                .iterator()
                .next()
                .getModeChangeMessage(this.mode.getMaterialData().getItemType());
      } else {
        this.name = "Unknown Mode";
      }
    }
  }

  public final Mode getMode() {
    return this.mode;
  }

  public void setName(@Nonnull String name) {
    this.name = Preconditions.checkNotNull(name, "name");
  }

  public String getName() {
    return this.name;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
