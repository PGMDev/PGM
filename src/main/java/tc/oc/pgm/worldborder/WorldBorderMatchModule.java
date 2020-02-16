package tc.oc.pgm.worldborder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.joda.time.Duration;
import tc.oc.pgm.api.event.CoarsePlayerMoveEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.filters.query.IQuery;
import tc.oc.pgm.filters.query.MatchQuery;
import tc.oc.pgm.goals.events.GoalEvent;
import tc.oc.util.collection.DefaultMapAdapter;
import tc.oc.world.WorldBorders;

@ListenerScope(MatchScope.LOADED)
public class WorldBorderMatchModule implements MatchModule, Listener {

  private final Match match;
  private final List<WorldBorder> borders;
  private final Map<WorldBorder, Boolean> results = new DefaultMapAdapter<>(false);
  private @Nullable WorldBorder appliedBorder;
  private @Nullable Duration appliedAt;

  public WorldBorderMatchModule(Match match, List<WorldBorder> borders) {
    this.match = match;
    checkNotNull(borders);
    checkArgument(!borders.isEmpty());
    this.borders = borders;
  }

  @Override
  public void load() {
    WorldBorder initial = null;
    for (WorldBorder border : borders) {
      if (!border.isConditional()) initial = border;
    }

    if (initial != null) {
      match.getLogger().fine("Initializing with " + initial);
      apply(initial);
    } else {
      reset();
    }
  }

  @Override
  public void enable() {
    match
        .getScheduler(MatchScope.RUNNING)
        .runTaskTimer(
            Duration.ZERO,
            Duration.standardSeconds(1),
            new Runnable() {
              @Override
              public void run() {
                if (!update(null)) {
                  refresh();
                }
              }
            });
  }

  @Override
  public void disable() {
    freeze();
  }

  private void apply(WorldBorder border) {
    match.getLogger().fine("Applying " + border);

    border.apply(match.getWorld().getWorldBorder(), appliedBorder != null);
    appliedBorder = border;
    appliedAt = match.getDuration();
  }

  private void reset() {
    match.getLogger().fine("Clearing border");

    appliedBorder = null;
    appliedAt = null;
    match.getWorld().getWorldBorder().reset();
  }

  /**
   * Query the filters of all borders and apply them as needed.
   *
   * <p>A border is applied when its filter goes from false to true, or when it becomes active
   * because of another border further down the list going from true to false.
   *
   * <p>If multiple borders become active simultaneously, they are applied in order. This allows a
   * border to serve as the starting point for another moving border.
   *
   * @param event to use for the filter query
   */
  private boolean update(@Nullable Event event) {
    IQuery query = event == null ? match.getQuery() : new MatchQuery(event, match);
    WorldBorder lastMatched = null;
    boolean applied = false;

    for (WorldBorder border : borders) {
      boolean newResult = border.filter.query(query).isAllowed();
      boolean oldResult = results.put(border, newResult);
      if (newResult) lastMatched = border;

      if (!oldResult && newResult) {
        // On the filter's rising edge, apply the border
        applied = true;
        apply(border);
      } else if (oldResult && !newResult) {
        if (lastMatched == null) {
          // On the filter's falling edge, apply the last border in the list with a passing filter
          reset();
        } else {
          // If no borders have passing filters, clear the border
          apply(lastMatched);
        }
      }
    }

    return applied;
  }

  /**
   * If the current border is moving, refresh its size/duration on all clients (to keep them in
   * sync)
   */
  private void refresh() {
    if (appliedBorder != null) {
      appliedBorder.refresh(
          match.getWorld().getWorldBorder(), match.getDuration().minus(appliedAt));
    }
  }

  /** If the current border is moving, stop it in-place */
  private void freeze() {
    if (appliedBorder != null && appliedBorder.isMoving()) {
      match.getLogger().fine("Freezing border " + appliedBorder);
      match.getWorld().getWorldBorder().setSize(match.getWorld().getWorldBorder().getSize(), 0);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onGoalComplete(GoalEvent event) {
    update(event);
  }

  /** Prevent teleporting outside the border */
  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerTeleport(final PlayerTeleportEvent event) {
    if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
      if (WorldBorders.isInsideBorder(event.getFrom())
          && !WorldBorders.isInsideBorder(event.getTo())) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerMove(final CoarsePlayerMoveEvent event) {
    MatchPlayer player = match.getPlayer(event.getPlayer());
    if (player != null && player.isObserving()) {
      Location location = event.getTo();
      if (WorldBorders.clampToBorder(location)) {
        event.setTo(location);
      }
    }
  }
}
