package tc.oc.pgm.goals;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.party.event.CompetitorRemoveEvent;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.channels.ChatManager;
import tc.oc.pgm.goals.events.GoalCompleteEvent;
import tc.oc.pgm.goals.events.GoalTouchEvent;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
import tc.oc.pgm.util.Audience;

/**
 * A {@link Goal} that may be 'touched' by players, meaning the player has made some tangible
 * progress in completing the goal.
 */
public abstract class TouchableGoal<T extends ProximityGoalDefinition> extends ProximityGoal<T>
    implements Listener {

  public static final TextColor COLOR_TOUCHED = NamedTextColor.YELLOW;
  public static final Component SYMBOL_TOUCHED = text("\u2733"); // ✳

  protected boolean touched;
  protected final Set<Competitor> touchingCompetitors = new HashSet<>();
  protected final Set<ParticipantState> touchingPlayers = new HashSet<>();
  protected final Set<ParticipantState> recentTouchingPlayers = new HashSet<>();

  public TouchableGoal(T definition, Match match) {
    super(definition, match);
    match.addListener(this, MatchScope.RUNNING);
  }

  /** Should touches NOT be credited until the goal is completed? */
  public boolean getDeferTouches() {
    return false;
  }

  /**
   * Gets a formatted message designed to be broadcast when a player touches the goal.
   *
   * @param toucher The player
   * @param self is the message for the toucher?
   */
  public abstract Component getTouchMessage(@Nullable ParticipantState toucher, boolean self);

  @Override
  public TextColor renderProximityColor(Competitor team, Party viewer) {
    return hasTouched(team) ? NamedTextColor.YELLOW : super.renderProximityColor(team, viewer);
  }

  @Override
  public TextColor renderSidebarStatusColor(@Nullable Competitor competitor, Party viewer) {
    return shouldShowTouched(competitor, viewer)
        ? COLOR_TOUCHED
        : super.renderSidebarStatusColor(competitor, viewer);
  }

  @Override
  public Component renderSidebarStatusText(@Nullable Competitor competitor, Party viewer) {
    return shouldShowTouched(competitor, viewer)
        ? SYMBOL_TOUCHED
        : super.renderSidebarStatusText(competitor, viewer);
  }

  public boolean isTouched() {
    return touched;
  }

  /** Gets whether or not the specified team has touched the goal since the last reset. */
  public boolean hasTouched(Competitor team) {
    return touchingCompetitors.contains(team);
  }

  public boolean hasTouched(ParticipantState player) {
    return touchingPlayers.contains(player);
  }

  public ImmutableSet<ParticipantState> getTouchingPlayers() {
    return ImmutableSet.copyOf(touchingPlayers);
  }

  /**
   * Gets whether or not the specified player has recently (in their current lifetime) touched the
   * goal.
   */
  public boolean hasTouchedRecently(final ParticipantState player) {
    return recentTouchingPlayers.contains(player);
  }

  /**
   * Gets whether or not the specified player touching the goal has any significance at this moment.
   */
  public boolean canTouch(final ParticipantState player) {
    return canComplete(player.getParty())
        && !isCompleted(player.getParty())
        && !hasTouchedRecently(player);
  }

  public void touch(final @Nullable ParticipantState toucher) {
    // TODO: support playerless touches (deduce which team to give the touch to based on objective
    // owner etc)
    if (toucher == null) return;

    touched = true;

    GoalTouchEvent event;
    if (toucher == null) {
      event = new GoalTouchEvent(this, getMatch().getTick().instant);
    } else {
      if (!canTouch(toucher)) return;

      boolean firstForCompetitor = touchingCompetitors.add(toucher.getParty());
      boolean firstForPlayer = touchingPlayers.add(toucher);
      boolean firstForPlayerLife = recentTouchingPlayers.add(toucher);

      event = new GoalTouchEvent(
          this,
          toucher.getParty(),
          firstForCompetitor,
          toucher,
          firstForPlayer,
          firstForPlayerLife,
          getMatch().getTick().instant);
    }

    getMatch().callEvent(event);
    sendTouchMessage(toucher, !event.getCancelToucherMessage());
  }

  public void resetTouches() {
    touched = false;
    touchingCompetitors.clear();
    touchingPlayers.clear();
    recentTouchingPlayers.clear();
  }

  public void resetTouches(Competitor team) {
    if (touchingCompetitors.remove(team)) {
      for (Iterator<ParticipantState> iterator = touchingPlayers.iterator(); iterator.hasNext(); ) {
        if (iterator.next().getParty() == team) iterator.remove();
        ;
      }
      for (Iterator<ParticipantState> iterator = recentTouchingPlayers.iterator();
          iterator.hasNext(); ) {
        if (iterator.next().getParty() == team) iterator.remove();
        ;
      }
    }
  }

  @Override
  public @Nullable ProximityMetric getProximityMetric(Competitor team) {
    if (hasTouched(team)) {
      return getDefinition().getPostTouchMetric();
    } else {
      return super.getProximityMetric(team);
    }
  }

  public boolean showEnemyTouches() {
    return false;
  }

  public boolean shouldShowTouched(@Nullable Competitor team) {
    return team != null && !isCompleted(team) && hasTouched(team);
  }

  public boolean shouldShowTouched(@Nullable Competitor team, Party viewer) {
    return shouldShowTouched(team)
        && (team == viewer || showEnemyTouches() || viewer.isObserving());
  }

  protected void sendTouchMessage(@Nullable ParticipantState toucher, boolean includeToucher) {
    if (!hasShowOption(ShowOption.SHOW_MESSAGES)) return;

    Component message = getTouchMessage(toucher, false);
    Audience.console().sendMessage(message);

    if (shouldShowTouched(toucher.getParty())) {
      if (showEnemyTouches())
        ChatManager.broadcastMessage(
            message, viewer -> toucher == null || !toucher.isPlayer(viewer));
      else
        ChatManager.broadcastPartyMessage(
            message, toucher.getParty(), viewer -> toucher == null || !toucher.isPlayer(viewer));
    }

    if (toucher != null) {
      if (includeToucher) {
        toucher.sendMessage(getTouchMessage(toucher, true));
      }

      if (getDeferTouches()) {
        toucher.sendMessage(translatable("objective.credit.future", text(this.getName())));
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerDeath(ParticipantDespawnEvent event) {
    ParticipantState victim = event.getPlayer().getParticipantState();
    if (victim != null) recentTouchingPlayers.remove(victim);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onCompetitorRemove(CompetitorRemoveEvent event) {
    resetTouches(event.getCompetitor());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onComplete(GoalCompleteEvent event) {
    if (this == event.getGoal()) {
      resetTouches();
    }
  }
}
