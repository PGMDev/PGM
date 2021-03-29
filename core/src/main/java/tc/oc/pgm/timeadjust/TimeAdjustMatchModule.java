package tc.oc.pgm.timeadjust;

import static com.google.common.base.Preconditions.checkNotNull;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TemporalComponent.duration;

import com.google.common.collect.Maps;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.GoalDefinition;
import tc.oc.pgm.goals.events.GoalCompleteEvent;

@ListenerScope(MatchScope.RUNNING)
public class TimeAdjustMatchModule implements MatchModule, Listener {

  private final Match match;
  private final Map<GoalDefinition, TimeAdjust> adjustmentDefs;
  private Map<Goal, TimeAdjust> adjustments;

  public TimeAdjustMatchModule(Match match, Map<GoalDefinition, TimeAdjust> adjustments) {
    this.match = match;
    this.adjustmentDefs = checkNotNull(adjustments);
  }

  @Override
  public void load() {
    Map<Goal, TimeAdjust> adjusts = Maps.newHashMap();
    adjustmentDefs.forEach(
        (def, time) -> {
          Goal goal = def.getGoal(match);
          if (goal != null) {
            adjusts.put(goal, time);
          }
        });
    this.adjustments = adjusts;
  }

  @EventHandler
  public void onGoalComplete(GoalCompleteEvent event) {
    TimeAdjust time = adjustments.get(event.getGoal());
    if (time != null && time.adjustTime(match) && match.isRunning()) {
      match.sendMessage(createBroadcast(time));
    }
  }

  private Component createBroadcast(TimeAdjust adjust) {
    boolean decrease = adjust.getTime().isNegative();
    String key = "misc." + (decrease ? "decrease" : "increase");
    NamedTextColor color = decrease ? NamedTextColor.RED : NamedTextColor.GREEN;
    return translatable(
        "match.timeLimit.increase",
        NamedTextColor.YELLOW,
        translatable(key, color),
        duration(adjust.getTime(), color));
  }
}
