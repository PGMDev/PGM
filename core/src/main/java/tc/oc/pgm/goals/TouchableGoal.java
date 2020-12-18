package tc.oc.pgm.goals;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.party.event.CompetitorRemoveEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.goals.events.GoalCompleteEvent;
import tc.oc.pgm.goals.events.GoalTouchEvent;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
import tc.oc.pgm.util.chat.Audience;

/**
 * A {@link Goal} that may be 'touched' by players, meaning the player has made some tangible
 * progress in completing the goal.
 */
public abstract class TouchableGoal<T extends ProximityGoalDefinition> extends ProximityGoal<T>
    implements Listener {

  public static final ChatColor COLOR_TOUCHED = ChatColor.YELLOW;
  public static final String SYMBOL_TOUCHED = "\u2733"; // ✳

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
  public net.md_5.bungee.api.ChatColor renderProximityColor(Competitor team, Party viewer) {
    return hasTouched(team)
        ? net.md_5.bungee.api.ChatColor.YELLOW
        : super.renderProximityColor(team, viewer);
  }

  @Override
  public ChatColor renderSidebarStatusColor(@Nullable Competitor competitor, Party viewer) {
    return shouldShowTouched(competitor, viewer)
        ? COLOR_TOUCHED
        : super.renderSidebarStatusColor(competitor, viewer);
  }

  @Override
  public String renderSidebarStatusText(@Nullable Competitor competitor, Party viewer) {
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

      event =
          new GoalTouchEvent(
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
    playTouchEffects(toucher);
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

  public boolean shouldShowTouched(@Nullable Competitor team, Party viewer) {
    return team != null
        && !isCompleted(team)
        && hasTouched(team)
        && (team == viewer || showEnemyTouches() || viewer.isObserving());
  }

  protected void sendTouchMessage(@Nullable ParticipantState toucher, boolean includeToucher) {
    if (!isVisible()) return;

    Component message = getTouchMessage(toucher, false);
    Audience.get(Bukkit.getConsoleSender()).sendMessage(message);

    if (!showEnemyTouches()) {
      message = Component.text().append(toucher.getParty().getChatPrefix()).append(message).build();
    }

    for (MatchPlayer viewer : getMatch().getPlayers()) {
      if (shouldShowTouched(toucher.getParty(), viewer.getParty())
          && (toucher == null || !toucher.isPlayer(viewer))) {
        viewer.sendMessage(message);
      }
    }

    if (toucher != null) {
      if (includeToucher) {
        toucher.sendMessage(getTouchMessage(toucher, true));
      }

      if (getDeferTouches()) {
        toucher.sendMessage(
            Component.translatable("objective.credit.future", Component.text(this.getName())));
      }
    }
  }

  protected void playTouchEffects(@Nullable ParticipantState toucher) {
    if (toucher == null || !isVisible()) return;

    MatchPlayer onlineToucher = toucher.getPlayer().orElse(null);
    if (onlineToucher == null) return;
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
