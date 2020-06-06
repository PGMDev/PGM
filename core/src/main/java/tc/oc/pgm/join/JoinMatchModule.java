package tc.oc.pgm.join;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.match.ObservingParty;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;

@ListenerScope(MatchScope.LOADED)
public class JoinMatchModule implements MatchModule, Listener, JoinHandler {

  public static class Factory implements MatchModuleFactory<JoinMatchModule> {
    @Override
    public Collection<Class<? extends MatchModule>> getWeakDependencies() {
      return ImmutableList.of(TimeLimitMatchModule.class);
    }

    @Override
    public JoinMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new JoinMatchModule(match);
    }
  }

  private final Set<JoinGuard> guards = new LinkedHashSet<>();
  private final Set<JoinHandler> handlers = new LinkedHashSet<>();

  // Players who have requested to join before match start
  private final QueuedParticipants queuedParticipants;
  private final Match match;

  private JoinMatchModule(Match match) {
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

  private Config getConfig() {
    return PGM.get().getConfiguration();
  }

  public boolean canJoinFull(MatchPlayer joining) {
    return !getConfig().shouldLimitJoin()
        || joining.getBukkit().hasPermission(Permissions.JOIN_FULL);
  }

  public boolean canPriorityKick(MatchPlayer joining) {
    return getConfig().canPriorityKick()
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

    // Don't allow vanished players to join
    if (joining.isVanished()) {
      return GenericJoinResult.Status.VANISHED.toResult();
    }

    // If mid-match join is disabled, player cannot join for the first time after the match has
    // started
    if (match.isRunning() && !getConfig().canAnytimeJoin()) {
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
          joining.sendWarning(TranslatableComponent.of("join.err.afterStart"));
          return true;

        case MATCH_FINISHED:
          joining.sendWarning(TranslatableComponent.of("join.err.afterFinish"));
          return true;

        case NO_PERMISSION:
          joining.sendWarning(TranslatableComponent.of("join.err.noPermission"));
          return true;

        case VANISHED:
          joining.sendWarning(TranslatableComponent.of("join.err.vanish"));
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
    if (joining.isVanished()) return join(joining, forcedParty);

    for (JoinHandler handler : handlers) {
      if (handler.forceJoin(joining, forcedParty)) return true;
    }
    return false;
  }

  public boolean leave(MatchPlayer leaving) {
    if (cancelQueuedJoin(leaving)) return true;

    if (leaving.getParty() instanceof ObservingParty) {
      leaving.sendWarning(
          TranslatableComponent.of("join.err.alreadyJoined.team", leaving.getParty().getName()));
      return false;
    }

    if (!leaving.getBukkit().hasPermission(Permissions.LEAVE)) {
      leaving.sendWarning(TranslatableComponent.of("leave.err.noPermission"));
      return false;
    }

    return match.setParty(leaving, match.getDefaultParty());
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
      joining.sendMessage(TranslatableComponent.of("join.ok"));
    } else {
      joining.sendMessage(TranslatableComponent.of("join.ok.queue", TextColor.YELLOW));
    }

    return joined;
  }

  public boolean cancelQueuedJoin(MatchPlayer joining) {
    if (!isQueuedToJoin(joining)) return false;
    if (match.setParty(joining, match.getDefaultParty())) {
      joining.sendMessage(TranslatableComponent.of("join.ok.dequeue", TextColor.YELLOW));
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
