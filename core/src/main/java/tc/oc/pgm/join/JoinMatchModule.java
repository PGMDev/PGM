package tc.oc.pgm.join;

import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.match.ObserverParty;
import tc.oc.pgm.match.QueuedParty;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;
import tc.oc.pgm.util.bukkit.OnlinePlayerMapAdapter;

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

  // Players who have requested to join before match start
  private final QueuedParty queuedParticipants;
  private final Match match;
  private final OnlinePlayerMapAdapter<JoinRequest> requests;

  private JoinHandler handler;

  private JoinMatchModule(Match match) {
    this.match = match;
    this.queuedParticipants = new QueuedParty(match);
    this.requests = new OnlinePlayerMapAdapter<>(PGM.get());
  }

  @Override
  public void load() {
    match.addParty(queuedParticipants);
  }

  @Override
  public void unload() {
    requests.disable();
  }

  public void setJoinHandler(JoinHandler handler) {
    this.handler = handler;
  }

  private Config getConfig() {
    return PGM.get().getConfiguration();
  }

  public boolean canJoinFull(JoinRequest request) {
    return !getConfig().shouldLimitJoin() || request.isForcedOr(JoinRequest.Flag.JOIN_FULL);
  }

  public boolean priorityKickAllowed() {
    return getConfig().canPriorityKick() && !match.isRunning();
  }

  public boolean canPriorityKick(MatchPlayer player) {
    JoinRequest request = requests.get(player.getBukkit());
    return request != null && canPriorityKick(request);
  }

  public boolean canPriorityKick(JoinRequest request) {
    return priorityKickAllowed() && request.isForcedOr(JoinRequest.Flag.JOIN_FULL);
  }

  public boolean canBePriorityKicked(MatchPlayer player) {
    JoinRequest request = requests.get(player.getBukkit());
    if (request == null) return false;

    return priorityKickAllowed()
        && !request.isForcedOr(JoinRequest.Flag.JOIN_FULL)
        && !request.has(JoinRequest.Flag.SQUAD);
  }

  public boolean isAutoJoin(MatchPlayer player) {
    JoinRequest request = requests.get(player.getBukkit());
    return request != null && request.getTeam() == null;
  }

  @Override
  public JoinResult queryJoin(MatchPlayer joining, JoinRequest request) {
    // Player does not have permission to voluntarily join
    if (!request.isForcedOr(JoinRequest.Flag.JOIN)) {
      return JoinResultOption.NO_PERMISSION;
    }

    // Can't join if the match is over
    if (match.isFinished()) {
      return JoinResultOption.MATCH_FINISHED;
    }

    // Don't allow vanished players to join
    if (Integration.isVanished(joining.getBukkit())) {
      return JoinResultOption.VANISHED;
    }

    // If mid-match join is disabled, player cannot join for the first time after the match has
    // started
    if (match.isRunning() && !getConfig().canAnytimeJoin()) {
      return JoinResultOption.MATCH_STARTED;
    }

    return handler.queryJoin(joining, request);
  }

  @Override
  public boolean join(MatchPlayer joining, JoinRequest request, @NotNull JoinResult result) {
    if (result.isSuccess()) {
      requests.put(joining.getBukkit(), request);
    }

    switch (result.getOption()) {
      case QUEUED:
        queueToJoin(joining);
        return true;

      case MATCH_STARTED:
        joining.sendWarning(translatable("join.err.afterStart"));
        return true;

      case MATCH_FINISHED:
        joining.sendWarning(translatable("join.err.afterFinish"));
        return true;

      case NO_PERMISSION:
        joining.sendWarning(translatable("join.err.noPermission"));
        return true;

      case VANISHED:
        joining.sendWarning(translatable("join.err.vanish"));
        return true;
    }

    return handler.join(joining, request, result);
  }

  public boolean leave(MatchPlayer leaving, JoinRequest request) {
    if (cancelQueuedJoin(leaving)) return true;

    if (leaving.getParty() instanceof ObserverParty) {
      leaving.sendWarning(
          translatable("join.err.alreadyJoined.team", leaving.getParty().getName()));
      return false;
    }

    if (!leaving.getBukkit().hasPermission(Permissions.LEAVE)) {
      leaving.sendWarning(translatable("leave.err.noPermission"));
      return false;
    }

    return match.setParty(leaving, match.getDefaultParty(), request);
  }

  public QueuedParty getQueuedParticipants() {
    return queuedParticipants;
  }

  public boolean isQueuedToJoin(MatchPlayer joining) {
    return queuedParticipants.equals(joining.getParty());
  }

  public boolean queueToJoin(MatchPlayer joining) {
    boolean joined = match.setParty(joining, queuedParticipants);
    if (joined) {
      joining.sendMessage(translatable("join.ok"));
    } else {
      joining.sendMessage(translatable("join.ok.queue", NamedTextColor.YELLOW));
    }

    return joined;
  }

  public boolean cancelQueuedJoin(MatchPlayer joining) {
    if (!isQueuedToJoin(joining)) return false;
    if (match.setParty(joining, match.getDefaultParty())) {
      joining.sendMessage(translatable("join.ok.dequeue", NamedTextColor.YELLOW));
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void queuedJoin(QueuedParty queue) {
    // Give handler a chance to bulk join
    handler.queuedJoin(queue);

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
