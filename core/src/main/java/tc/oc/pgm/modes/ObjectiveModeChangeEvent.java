package tc.oc.pgm.modes;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchEvent;
import tc.oc.pgm.core.Core;
import tc.oc.pgm.destroyable.Destroyable;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.goals.ModeChangeGoal;

public class ObjectiveModeChangeEvent extends MatchEvent {

  private final Mode mode;
  private String name;
  private boolean visible;
  private static final HandlerList handlers = new HandlerList();

  public ObjectiveModeChangeEvent(Match match, final Mode mode) {
    super(match);
    this.mode = mode;
    this.visible = false;
    this.name = this.mode.getName(() -> {
      ModeChangeGoal<?> objective = getMainObjective();
      return objective == null || mode.getMaterialData() == null
          ? mode.getLegacyName()
          : objective.getModeChangeMessage(mode.getMaterialData().getItemType());
    });
  }

  private ModeChangeGoal<?> getMainObjective() {
    GoalMatchModule wins = getMatch().needModule(GoalMatchModule.class);
    Destroyable lastDestroyable = null;
    int affectedDestroyables = 0;
    for (Destroyable destroyable : wins.getGoals(Destroyable.class).values()) {
      if (!destroyable.isAffectedBy(mode)) continue;
      affectedDestroyables++;
      lastDestroyable = destroyable;
    }

    int affectedCores = 0;
    for (Core core : wins.getGoals(Core.class).values()) {
      if (!core.isAffectedBy(mode)) continue;
      if (++affectedCores > affectedDestroyables) return core;
    }
    return lastDestroyable;
  }

  public boolean isVisible() {
    return this.visible;
  }

  public final Mode getMode() {
    return this.mode;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  public void setName(@NotNull String name) {
    this.name = assertNotNull(name, "name");
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
