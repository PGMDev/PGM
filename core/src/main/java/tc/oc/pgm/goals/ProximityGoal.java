package tc.oc.pgm.goals;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.party.event.CompetitorRemoveEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.pgm.goals.events.GoalCompleteEvent;
import tc.oc.pgm.goals.events.GoalProximityChangeEvent;
import tc.oc.pgm.goals.events.GoalTouchEvent;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;
import tc.oc.pgm.util.LegacyFormatUtils;
import tc.oc.pgm.util.block.BlockVectors;
import tc.oc.pgm.util.collection.DefaultMapAdapter;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

public abstract class ProximityGoal<T extends ProximityGoalDefinition> extends OwnedGoal<T>
    implements Listener {

  private final Map<Competitor, Integer> proximity = new DefaultMapAdapter<>(Integer.MAX_VALUE);

  public ProximityGoal(T definition, Match match) {
    super(definition, match);
  }

  /**
   * Get the locations from which proximity can be measured relative to. The shortest measurement
   * will be used.
   */
  public abstract Iterable<Location> getProximityLocations(ParticipantState player);

  public @Nullable ProximityMetric getProximityMetric(Competitor team) {
    return getDefinition().getPreTouchMetric();
  }

  public @Nullable ProximityMetric.Type getProximityMetricType(Competitor team) {
    ProximityMetric metric = getProximityMetric(team);
    return metric == null ? null : metric.type;
  }

  /**
   * Is proximity relevant at the present moment for the given team? That is, can it be measured and
   * affect the outcome of te match?
   */
  public boolean isProximityRelevant(Competitor team) {
    return canComplete(team) && !isCompleted() && getProximityMetric(team) != null;
  }

  protected boolean canPlayerUpdateProximity(ParticipantState player) {
    return canComplete(player.getParty());
  }

  protected boolean canBlockUpdateProximity(BlockState oldState, BlockState newState) {
    return true;
  }

  private static double distanceFromDistanceSquared(int squared) {
    return squared == Integer.MAX_VALUE ? Double.POSITIVE_INFINITY : Math.sqrt(squared);
  }

  public int getProximity(Competitor team) {
    return this.proximity.get(team);
  }

  /**
   * Get the minimum distance the given team has been from the objective at any time during the
   * match (which is +Inf at the start of the match). The given metric determines exactly how this
   * is measured.
   */
  public double getMinimumDistance(Competitor team) {
    return distanceFromDistanceSquared(this.getProximity(team));
  }

  public void resetProximity(Competitor team) {
    Integer oldProximity = proximity.remove(team);
    if (oldProximity != null) {
      getMatch()
          .callEvent(new GoalProximityChangeEvent(
              this,
              team,
              null,
              distanceFromDistanceSquared(oldProximity),
              Double.POSITIVE_INFINITY));
    }
  }

  public void resetProximity() {
    for (Competitor team : proximity.keySet()) {
      resetProximity(team);
    }
  }

  public int getProximityFrom(ParticipantState player, Location location) {
    if (Double.isInfinite(location.lengthSquared())) return Integer.MAX_VALUE;

    ProximityMetric metric = getProximityMetric(player.getParty());
    if (metric == null) return Integer.MAX_VALUE;

    int minimumDistance = Integer.MAX_VALUE;
    for (Location v : getProximityLocations(player)) {
      // If either point is at infinity, the distance is infinite
      if (Double.isInfinite(v.lengthSquared())) continue;

      int dx = location.getBlockX() - v.getBlockX();
      int dy = location.getBlockY() - v.getBlockY();
      int dz = location.getBlockZ() - v.getBlockZ();

      // Note: distances stay squared as long as possible
      int distance;
      if (metric.horizontal) {
        distance = dx * dx + dz * dz;
      } else {
        distance = dx * dx + dy * dy + dz * dz;
      }

      if (distance < minimumDistance) {
        minimumDistance = distance;
      }
    }

    return minimumDistance;
  }

  public boolean updateProximity(ParticipantState player, Location location) {
    if (isProximityRelevant(player.getParty()) && canPlayerUpdateProximity(player)) {
      int oldProximity = proximity.get(player.getParty());
      int newProximity = getProximityFrom(player, location);
      if (newProximity < oldProximity) {
        proximity.put(player.getParty(), newProximity);
        getMatch()
            .callEvent(new GoalProximityChangeEvent(
                this,
                player.getParty(),
                location,
                distanceFromDistanceSquared(oldProximity),
                distanceFromDistanceSquared(newProximity)));
        return true;
      }
    }
    return false;
  }

  public boolean shouldShowProximity(@Nullable Competitor team, Party viewer) {
    return team != null
        && PGM.get()
            .getConfiguration()
            .showProximity(match.moduleRequire(TimeLimitMatchModule.class).willUseProximity())
        && isProximityRelevant(team)
        && (viewer == team || viewer.isObserving());
  }

  public TextColor renderProximityColor(Competitor team, Party viewer) {
    return NamedTextColor.GRAY;
  }

  public Component renderProximity(@Nullable Competitor team, Party viewer) {
    if (!shouldShowProximity(team, viewer)) {
      return empty();
    }

    String text;
    double distance = this.getMinimumDistance(team);
    if (distance == Double.POSITIVE_INFINITY) {
      text = "\u221e"; // ∞
    } else {
      text = LegacyFormatUtils.tiny(String.format("%.1f", distance));
    }

    return text(text, renderProximityColor(team, viewer));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerMove(PlayerCoarseMoveEvent event) {
    MatchPlayer player = getMatch().getParticipant(event.getPlayer());
    if (player != null
        && getProximityMetricType(player.getCompetitor()) == ProximityMetric.Type.CLOSEST_PLAYER) {

      updateProximity(player.getParticipantState(), event.getBlockTo());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerPlaceBlock(ParticipantBlockTransformEvent event) {
    if (getProximityMetricType(event.getPlayerState().getParty())
            == ProximityMetric.Type.CLOSEST_BLOCK
        && canBlockUpdateProximity(event.getOldState(), event.getNewState())) {

      updateProximity(event.getPlayerState(), BlockVectors.center(event.getNewState()));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerKill(MatchPlayerDeathEvent event) {
    if (event.getKiller() != null
        && event.isChallengeKill()
        && getProximityMetricType(event.getKiller().getParty())
            == ProximityMetric.Type.CLOSEST_KILL) {

      updateProximity(event.getKiller(), event.getKiller().getLocation());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onTouch(GoalTouchEvent event) {
    if (this == event.getGoal() && event.isFirstForCompetitor()) {
      resetProximity(event.getCompetitor());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onComplete(GoalCompleteEvent event) {
    if (this == event.getGoal()) {
      resetProximity();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onCompetitorRemove(CompetitorRemoveEvent event) {
    resetProximity(event.getCompetitor());
  }
}
