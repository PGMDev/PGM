package tc.oc.pgm.join;

import com.google.common.collect.Iterables;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.match.ObservingParty;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;
import tc.oc.util.components.PeriodFormats;

@ListenerScope(MatchScope.LOADED)
public class JoinMatchModule implements MatchModule, Listener, JoinHandler {

  private final Set<JoinGuard> guards = new LinkedHashSet<>();
  private final Set<JoinHandler> handlers = new LinkedHashSet<>();

  // Players who have requested to join before match start
  private final QueuedParticipants queuedParticipants;
  private final Match match;

  public JoinMatchModule(Match match) {
    this.match = match;
    queuedParticipants = new QueuedParticipants(match);
  }

  @Override
  public void load() {
    match.addParty(queuedParticipants);
  }

  public void registerHandler(JoinGuard guard) {
    if (guard instanceof JoinHandler) {
      handlers.add((JoinHandler) guard);
    } else {
      guards.add(guard);
    }
  }

  public boolean canJoinFull(MatchPlayer joining) {
    return !Config.Join.capacity()
        || (Config.Join.overfill() && joining.getBukkit().hasPermission(Permissions.JOIN_FULL));
  }

  public boolean canPriorityKick(MatchPlayer joining) {
    return Config.Join.priorityKick()
        && joining.getBukkit().hasPermission(Permissions.JOIN_FULL)
        && !match.isRunning();
  }

  private Iterable<JoinGuard> allGuards() {
    return Iterables.concat(guards, handlers);
  }

  @Override
  public @Nullable JoinResult queryJoin(MatchPlayer joining, @Nullable Competitor chosenParty) {
    // Player does not have permission to voluntarily join
    if (!joining.getBukkit().hasPermission(Permissions.JOIN)) {
      return GenericJoinResult.Status.NO_PERMISSION.toResult();
    }

    // Can't join if the match is over
    if (match.isFinished()) {
      return GenericJoinResult.Status.MATCH_FINISHED.toResult();
    }

    // If mid-match join is disabled, player cannot join for the first time after the match has
    // started
    if (match.isRunning() && !Config.Join.midMatch()) {
      return GenericJoinResult.Status.MATCH_STARTED.toResult();
    }

    for (JoinGuard guard : allGuards()) {
      JoinResult result = guard.queryJoin(joining, chosenParty);
      if (result != null) return result;
    }

    return null;
  }

  @Override
  public boolean join(MatchPlayer joining, @Nullable Competitor chosenParty, JoinResult result) {
    if (result instanceof GenericJoinResult) {
      GenericJoinResult genericResult = (GenericJoinResult) result;
      if (genericResult.getStatus() == GenericJoinResult.Status.QUEUED) {
        queueToJoin(joining);
        return true;
      }

      switch (genericResult.getStatus()) {
        case MATCH_STARTED:
          joining.sendWarning(
              new PersonalizedTranslatable("command.gameplay.join.matchStarted"), false);
          return true;

        case MATCH_FINISHED:
          joining.sendWarning(
              new PersonalizedTranslatable("command.gameplay.join.matchFinished"), false);
          return true;

        case NO_PERMISSION:
          joining.sendWarning(
              new PersonalizedTranslatable("command.gameplay.join.joinDenied"), false);
          return true;
      }
    }

    for (JoinGuard guard : allGuards()) {
      if (guard.join(joining, chosenParty, result)) return true;
    }

    return false;
  }

  public boolean join(MatchPlayer joining, @Nullable Competitor chosenParty) {
    return join(joining, chosenParty, queryJoin(joining, chosenParty));
  }

  @Override
  public boolean forceJoin(MatchPlayer joining, @Nullable Competitor forcedParty) {
    for (JoinHandler handler : handlers) {
      if (handler.forceJoin(joining, forcedParty)) return true;
    }
    return false;
  }

  public boolean leave(MatchPlayer leaving) {
    if (cancelQueuedJoin(leaving)) return true;

    if (leaving.getParty() instanceof ObservingParty) {
      leaving.sendWarning(
          new PersonalizedTranslatable("command.gameplay.leave.alreadyOnObservers"), false);
      return false;
    }

    if (!leaving.getBukkit().hasPermission(Permissions.LEAVE)) {
      leaving.sendWarning(
          new PersonalizedTranslatable("command.gameplay.leave.leaveDenied"), false);
      return false;
    }

    Party observers = match.getDefaultParty();
    leaving.sendMessage(new PersonalizedTranslatable("team.join", observers.getComponentName()));
    return match.setParty(leaving, observers);
  }

  public QueuedParticipants getQueuedParticipants() {
    return queuedParticipants;
  }

  public boolean isQueuedToJoin(MatchPlayer joining) {
    return queuedParticipants.equals(joining.getParty());
  }

  public boolean queueToJoin(MatchPlayer joining) {
    boolean joined = match.setParty(joining, queuedParticipants);
    if (joined) {
      joining.sendMessage(new PersonalizedTranslatable("ffa.join"));
    }

    joining.sendMessage(
        new PersonalizedText(
            new PersonalizedTranslatable("team.join.deferred.request"),
            ChatColor.YELLOW)); // Always show this message

    if (match.hasModule(TeamMatchModule.class)) {
      // If they are joining a team, show them a scary warning about leaving the match
      joining.sendMessage(
          new PersonalizedText(
              new PersonalizedTranslatable(
                  "team.join.forfeitWarning",
                  new PersonalizedText(
                      new PersonalizedTranslatable("team.join.forfeitWarning.emphasis.warning"),
                      ChatColor.RED),
                  new PersonalizedText(
                      new PersonalizedTranslatable(
                          "team.join.forfeitWarning.emphasis.playUntilTheEnd"),
                      ChatColor.RED),
                  new PersonalizedText(
                      new PersonalizedTranslatable("team.join.forfeitWarning.emphasis.doubleLoss"),
                      ChatColor.RED),
                  new PersonalizedText(
                      new PersonalizedTranslatable("team.join.forfeitWarning.emphasis.suspension"),
                      ChatColor.RED)),
              ChatColor.DARK_RED));

      TimeLimitMatchModule tlmm = match.getModule(TimeLimitMatchModule.class);
      if (tlmm != null && tlmm.getTimeLimit() != null) {
        joining.sendMessage(
            new PersonalizedText(
                new PersonalizedTranslatable(
                    "team.join.forfeitWarning.timeLimit",
                    new PersonalizedText(
                        PeriodFormats.briefNaturalPrecise(tlmm.getTimeLimit().getDuration()),
                        ChatColor.AQUA),
                    new PersonalizedText("/leave", ChatColor.GOLD)),
                ChatColor.DARK_RED,
                ChatColor.BOLD));
      } else {
        joining.sendMessage(
            new PersonalizedText(
                new PersonalizedTranslatable(
                    "team.join.forfeitWarning.noTimeLimit",
                    new PersonalizedText("/leave", ChatColor.GOLD)),
                ChatColor.DARK_RED,
                ChatColor.BOLD));
      }
    }

    return joined;
  }

  public boolean cancelQueuedJoin(MatchPlayer joining) {
    if (!isQueuedToJoin(joining)) return false;
    if (match.setParty(joining, match.getDefaultParty())) {
      joining.sendMessage(
          new PersonalizedText(
              new PersonalizedTranslatable("team.join.deferred.cancel"), ChatColor.YELLOW));
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void queuedJoin(QueuedParticipants queue) {
    // Give all handlers a chance to bulk join
    for (JoinHandler handler : handlers) {
      if (queue.getPlayers().isEmpty()) break;
      handler.queuedJoin(queue);
    }

    // Send any leftover players to obs
    for (MatchPlayer joining : queue.getOrderedPlayers()) {
      match.setParty(joining, match.getDefaultParty());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchCommit(MatchStartEvent event) {
    queuedJoin(queuedParticipants);
  }
}
