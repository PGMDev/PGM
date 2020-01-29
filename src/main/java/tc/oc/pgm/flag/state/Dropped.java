package tc.oc.pgm.flag.state;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.joda.time.Duration;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.Post;
import tc.oc.pgm.goals.events.GoalStatusChangeEvent;
import tc.oc.pgm.scoreboard.SidebarMatchModule;
import tc.oc.util.TimeUtils;

/**
 * State of a flag after a player drops it on the ground, either by dying or by clicking on the
 * banner in their inventory. A flag can only enter this state when subject to a return delay.
 */
public class Dropped extends Uncarried implements Missing {

  // Minimum time between a player dropping the flag and picking it up again
  private static final Duration PICKUP_DELAY = Duration.standardSeconds(2);

  private final MatchPlayer dropper;

  public Dropped(Flag flag, Post post, Location location, MatchPlayer dropper) {
    super(flag, post, location);
    this.dropper = dropper;
  }

  @Override
  protected Duration getDuration() {
    return this.post.getRecoverTime();
  }

  @Override
  public void enterState() {
    super.enterState();

    if (!Duration.ZERO.equals(getDuration())) {
      this.flag.playStatusSound(Flag.DROP_SOUND_OWN, Flag.DROP_SOUND);
      this.flag
          .getMatch()
          .sendMessage(
              new PersonalizedTranslatable("match.flag.drop", this.flag.getComponentName()));
    }

    if (TimeUtils.isInfinite(getDuration())) {
      SidebarMatchModule smm = this.flag.getMatch().getModule(SidebarMatchModule.class);
      if (smm != null) smm.blinkGoal(this.flag, 2, null);
    }
  }

  @Override
  public void leaveState() {
    SidebarMatchModule smm = this.flag.getMatch().getModule(SidebarMatchModule.class);
    if (smm != null) smm.stopBlinkingGoal(this.flag);
    super.leaveState();
  }

  @Override
  protected void tickSeconds(long seconds) {
    super.tickSeconds(seconds);
    this.flag.getMatch().callEvent(new GoalStatusChangeEvent(this.flag.getMatch(), this.flag));
    this.labelEntity.setCustomName(this.flag.getColoredName() + " " + ChatColor.AQUA + seconds);
  }

  @Override
  protected void finishCountdown() {
    super.finishCountdown();
    this.recover();
  }

  @Override
  public boolean isRecoverable() {
    return true;
  }

  @Override
  protected boolean canPickup(MatchPlayer player) {
    return super.canPickup(player)
        && (player != this.dropper || this.enterTime.plus(PICKUP_DELAY).isBeforeNow());
  }

  @Override
  public ChatColor getStatusColor(Party viewer) {
    if (this.isCountingDown()) {
      return ChatColor.AQUA;
    } else if (this.flag.getDefinition().hasMultipleCarriers()) {
      return ChatColor.WHITE;
    } else {
      return super.getStatusColor(viewer);
    }
  }

  @Override
  public String getStatusSymbol(Party viewer) {
    return Flag.DROPPED_SYMBOL;
  }
}
