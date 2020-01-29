package tc.oc.pgm.flag.state;

import java.util.Collections;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;
import org.joda.time.Duration;
import org.joda.time.Instant;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.Post;
import tc.oc.pgm.flag.event.FlagCaptureEvent;
import tc.oc.pgm.flag.event.FlagStateChangeEvent;
import tc.oc.pgm.goals.events.GoalEvent;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.server.BukkitUtils;
import tc.oc.util.TimeUtils;

/** Base class for all {@link Flag} states */
public abstract class BaseState implements Runnable, State {

  protected final Flag flag;
  protected final Post post;
  protected final Instant enterTime;
  protected @Nullable Long remainingTicks;
  private BukkitTask task;

  protected BaseState(Flag flag, Post post) {
    this.flag = flag;
    this.post = post;
    this.enterTime = Instant.now();
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  /**
   * Called just after the flag's state has been changed to this object.
   *
   * <p>This method is NOT allowed to transition this flag into any other state, and doing so will
   * throw an exception. Care must be taken that any events fired cannot cause another transition
   * for this flag. Transitioning other flags is OK.
   *
   * <p>If this state wants to immediately transition, it should return zero from {@link
   * #getDuration}, which will cause {@link #finishCountdown} to be called after this method
   * returns.
   */
  public void enterState() {
    this.task = this.flag.getMatch().getScheduler(MatchScope.LOADED).runTaskTimer(1, 1, this);
  }

  public void leaveState() {
    if (this.task != null) {
      this.task.cancel();
      this.task = null;
    }
  }

  @Override
  public Iterable<Location> getProximityLocations(ParticipantState player) {
    return Collections.emptySet();
  }

  @Override
  public boolean isCurrent() {
    return this.flag.isCurrent(this);
  }

  protected @Nullable Duration getDuration() {
    return null;
  }

  public void startCountdown() {
    Duration duration = getDuration();
    if (duration != null) {
      if (Duration.ZERO.equals(duration)) {
        this.finishCountdown();
      } else if (!TimeUtils.isInfinite(duration)) {
        this.remainingTicks = duration.getMillis() / 50;
      }
    }
  }

  protected boolean isCountingDown() {
    return this.flag.getMatch().isRunning()
        && this.remainingTicks != null
        && this.remainingTicks > 0;
  }

  protected long getRemainingSeconds() {
    return this.remainingTicks == null ? -1 : (this.remainingTicks + 19) / 20;
  }

  @Override
  public void run() {
    this.tickLoaded();
    if (this.flag.getMatch().isRunning()) this.tickRunning();
  }

  public void tickLoaded() {}

  public void tickRunning() {
    if (this.isCountingDown()) {
      long before = this.getRemainingSeconds();
      if (--this.remainingTicks == 0) {
        this.finishCountdown();
      }
      long after = this.getRemainingSeconds();

      if (before != after) {
        this.tickSeconds(after);
      }
    }
  }

  protected void tickSeconds(long seconds) {}

  protected void finishCountdown() {}

  @Override
  public boolean isCarrying(MatchPlayer player) {
    return false;
  }

  @Override
  public boolean isCarrying(ParticipantState player) {
    MatchPlayer matchPlayer = player.getPlayer().orElse(null);
    return matchPlayer != null && isCarrying(matchPlayer);
  }

  @Override
  public boolean isCarrying(Party party) {
    return false;
  }

  @Override
  public Post getPost() {
    return this.post;
  }

  @Override
  public boolean isAtPost(Post post) {
    return post == this.post;
  }

  @Override
  public @Nullable Team getController() {
    if (this.post.getOwner() != null) {
      return this.flag.getMatch().needModule(TeamMatchModule.class).getTeam(this.post.getOwner());
    } else {
      return null;
    }
  }

  public ChatColor getStatusColor(Party viewer) {
    return BukkitUtils.convertColor(this.flag.getChatColor());
  }

  public ChatColor getLabelColor(Party viewer) {
    if (this.flag.hasMultipleControllers()) {
      Team controller = this.getController();
      return controller != null ? controller.getColor() : ChatColor.WHITE;
    } else {
      return ChatColor.WHITE;
    }
  }

  public String getStatusText(Party viewer) {
    if (this.isCountingDown()) {
      return String.valueOf(this.getRemainingSeconds());
    } else {
      return this.getStatusSymbol(viewer);
    }
  }

  public abstract String getStatusSymbol(Party viewer);

  public void onEvent(GoalEvent event) {}

  public void onEvent(FlagStateChangeEvent event) {}

  public void onEvent(FlagCaptureEvent event) {}

  public void onEvent(PlayerMoveEvent event) {}

  public void onEvent(BlockTransformEvent event) {}

  public void onEvent(PlayerDropItemEvent event) {}

  public void onEvent(ParticipantDespawnEvent event) {}

  public void onEvent(InventoryClickEvent event) {}

  public void onEvent(EntityDamageEvent event) {}
}
