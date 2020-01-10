package tc.oc.pgm.start;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.joda.time.Duration;
import tc.oc.bossbar.DynamicBossBar;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.bossbar.BossBarMatchModule;
import tc.oc.pgm.countdowns.MatchCountdown;
import tc.oc.pgm.countdowns.SingleCountdownContext;
import tc.oc.pgm.cycle.CycleMatchModule;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerLeaveMatchEvent;

@ListenerScope(MatchScope.LOADED)
public class StartMatchModule implements MatchModule, Listener {

  class UnreadyBar extends DynamicBossBar {
    @Override
    public boolean isVisible(Player viewer) {
      return !match.isRunning() && !unreadyReasons.isEmpty();
    }

    @Override
    public Component getText(Player viewer) {
      return formatUnreadyReason();
    }

    @Override
    public float getMeter(Player viewer) {
      return 1f;
    }
  }

  class UnreadyTimeout extends MatchCountdown {
    public UnreadyTimeout(Match match) {
      super(match);
    }

    @Override
    protected boolean showChat() {
      return false;
    }

    @Override
    protected Component formatText() {
      return formatUnreadyReason();
    }

    @Override
    public void onEnd(Duration total) {
      super.onEnd(total);
      match.needMatchModule(CycleMatchModule.class).cycleNow();
    }
  }

  protected final Match match;
  protected final UnreadyBar unreadyBar;
  protected final Set<UnreadyReason> unreadyReasons = new HashSet<>();
  protected final BossBarMatchModule bbmm;
  protected final StartConfig config;
  protected boolean autoStart; // Initialized from config, but is mutable

  public StartMatchModule(Match match) {
    this.match = match;
    this.unreadyBar = new UnreadyBar();
    this.config = new StartConfig(PGM.get().getConfig());
    this.autoStart = config.autoStart();
    this.bbmm = match.needMatchModule(BossBarMatchModule.class);
  }

  @Override
  public void load() {
    bbmm.pushBossBar(unreadyBar);
    update();
  }

  @EventHandler
  public void onCommit(MatchStartEvent event) {
    bbmm.removeBossBar(unreadyBar);
    unreadyReasons.clear();
  }

  // FIXME: Unsafe cast to SingleCountdownContext
  private SingleCountdownContext cc() {
    return (SingleCountdownContext) match.getCountdown();
  }

  /** If true, the match start countdown will automatically start when conditions allow it */
  public boolean isAutoStart() {
    return autoStart;
  }

  /** Enable/disable auto-start and return true if the setting was changed */
  public boolean setAutoStart(boolean autoStart) {
    if (this.autoStart != autoStart) {
      this.autoStart = autoStart;
      return true;
    } else {
      return false;
    }
  }

  public @Nullable Component formatUnreadyReason() {
    if (unreadyReasons.isEmpty()) {
      return null;
    } else {
      return new PersonalizedText(unreadyReasons.iterator().next().getReason(), ChatColor.RED);
    }
  }

  /**
   * Get all {@link UnreadyReason}s currently preventing the match from starting. If forceStart is
   * true, only return the reasons that prevent the match from being force started.
   */
  public Collection<UnreadyReason> getUnreadyReasons(boolean forceStart) {
    if (forceStart) {
      List<UnreadyReason> reasons = new ArrayList<>(unreadyReasons.size());
      for (UnreadyReason reason : unreadyReasons) {
        if (!reason.canForceStart()) reasons.add(reason);
      }
      return reasons;
    } else {
      return unreadyReasons;
    }
  }

  /** Add the given unready reason, replacing any existing reasons of the same type */
  public void addUnreadyReason(UnreadyReason newReason) {
    addUnreadyReason(newReason.getClass(), newReason);
  }

  /** Atomically replace all unready reasons of the given type with one other reason */
  public void addUnreadyReason(
      Class<? extends UnreadyReason> oldReasonType, UnreadyReason newReason) {
    boolean removed = removeUnreadyReasonsNoUpdate(oldReasonType);
    boolean added = addUnreadyReasonNoUpdate(newReason);
    if (removed || added) update();
  }

  /** Withdraw all unready reasons of the given type */
  public void removeUnreadyReason(Class<? extends UnreadyReason> reasonType) {
    if (removeUnreadyReasonsNoUpdate(reasonType)) {
      update();
    }
  }

  private boolean addUnreadyReasonNoUpdate(UnreadyReason reason) {
    if (unreadyReasons.add(reason)) {
      match.getLogger().fine("Added " + reason);
      return true;
    }
    return false;
  }

  private boolean removeUnreadyReasonsNoUpdate(Class<? extends UnreadyReason> reasonType) {
    boolean removed = false;
    for (Iterator<UnreadyReason> iterator = unreadyReasons.iterator(); iterator.hasNext(); ) {
      UnreadyReason reason = iterator.next();
      if (reasonType.isInstance(reason)) {
        iterator.remove();
        removed = true;
        match.getLogger().fine("Removed " + reason);
      }
    }
    return removed;
  }

  public boolean canStart(boolean force) {
    if (!match.getPhase().canTransitionTo(MatchPhase.STARTING)) return false;
    for (UnreadyReason reason : unreadyReasons) {
      if (!force || !reason.canForceStart()) return false;
    }
    return true;
  }

  /** Force the countdown to start with default duration if at all possible */
  public boolean forceStartCountdown() {
    return forceStartCountdown(null, null);
  }

  /** Force the countdown to start with the given duration if at all possible */
  public boolean forceStartCountdown(@Nullable Duration duration, @Nullable Duration huddle) {
    return canStart(true) && startCountdown(duration, huddle, true);
  }

  /**
   * Start the countdown with default duration if auto-start is enabled, and there are no soft
   * {@link UnreadyReason}s preventing it.
   */
  public boolean autoStartCountdown() {
    return isAutoStart() && canStart(false) && startCountdown(null, null, false);
  }

  public boolean restartUnreadyTimeout() {
    if (cc().getCountdown() instanceof UnreadyTimeout) {
      startUnreadyTimeout();
      return true;
    }
    return false;
  }

  private boolean startCountdown(
      @Nullable Duration duration, @Nullable Duration huddle, boolean force) {
    if (duration == null) duration = config.countdown();
    if (huddle == null) huddle = config.huddle();

    match.getLogger().fine("STARTING countdown");
    cc().start(new StartCountdown(match, force, huddle), duration);

    return true;
  }

  private boolean cancelCountdown() {
    if (cc().cancelAll(StartCountdown.class)) {
      match.getLogger().fine("Cancelled countdown");
      return true;
    }
    return false;
  }

  private void startUnreadyTimeout() {
    Duration duration = config.timeout();
    if (duration != null) {
      match.getLogger().fine("STARTING unready timeout with duration " + duration);
      cc().start(new UnreadyTimeout(match), duration);
    }
  }

  private void cancelUnreadyTimeout() {
    if (cc().cancelAll(UnreadyTimeout.class)) {
      match.getLogger().fine("Cancelled unready timeout");
    }
  }

  private void update() {
    final StartCountdown countdown = cc().getCountdown(StartCountdown.class);
    final boolean ready = canStart(countdown != null && countdown.isForced());
    final boolean empty = match.getPlayers().isEmpty();
    if (countdown == null && ready && isAutoStart()) {
      startCountdown(null, null, false);
    } else if (countdown != null && !ready) {
      cancelCountdown();
    }

    final UnreadyTimeout timeout = cc().getCountdown(UnreadyTimeout.class);
    if (timeout == null && !ready && !empty) {
      startUnreadyTimeout();
    } else if (timeout != null && (ready || empty)) {
      cancelUnreadyTimeout();
    }

    unreadyBar.invalidate();
  }

  @EventHandler
  public void onJoin(PlayerJoinMatchEvent event) {
    if (match.getPlayers().size() == 1) {
      update();
    }
  }

  @EventHandler
  public void onLeave(PlayerLeaveMatchEvent event) {
    if (match.getPlayers().isEmpty()) {
      update();
    }
  }
}
