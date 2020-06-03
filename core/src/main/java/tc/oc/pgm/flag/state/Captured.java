package tc.oc.pgm.flag.state;

import net.kyori.text.TranslatableComponent;
import org.bukkit.Location;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.filters.query.GoalQuery;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.Net;
import tc.oc.pgm.flag.Post;
import tc.oc.pgm.flag.event.FlagCaptureEvent;
import tc.oc.pgm.flag.event.FlagStateChangeEvent;

/**
 * Flag is looking for a place to respawn after being captured. This phase can be delayed by a
 * respawn-filter.
 */
public class Captured extends BaseState implements Returning {

  protected final Net net;
  protected final Location lastLocation;
  protected boolean wasDelayed;

  protected Captured(Flag flag, Post post, Net net, Location lastLocation) {
    super(flag, post);
    this.net = net;
    this.lastLocation = lastLocation;
  }

  protected boolean tryRespawn(boolean allFlagsCaptured) {
    if ((!this.net.isRespawnTogether() || allFlagsCaptured)
        && this.net.getRespawnFilter().query(new GoalQuery(this.flag)).isAllowed()) {

      this.flag.transition(
          new Respawning(this.flag, this.post, this.lastLocation, true, this.wasDelayed));
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void tickRunning() {
    super.tickRunning();

    // This will only be called if respawn was initially prevented by the filter,
    // which is the only case in which we want to broadcast the message.
    if (!this.wasDelayed) {
      if (this.net.getRespawnMessage() != null) {
        this.flag.getMatch().sendMessage(this.net.getRespawnMessage());
      } else if (this.net.isRespawnTogether()) {
        this.flag
            .getMatch()
            .sendMessage(
                TranslatableComponent.of("flag.respawnTogether", this.flag.getComponentName()));
      }
    }

    this.wasDelayed = true;
    tryRespawn(false);
  }

  @Override
  public void onEvent(FlagCaptureEvent event) {
    super.onEvent(event);
    tryRespawn(
        event.areAllFlagsCaptured()
            && event.getNet().getCapturableFlags().contains(this.flag.getDefinition()));
  }

  @Override
  public void onEvent(FlagStateChangeEvent event) {
    super.onEvent(event);
    // Try to respawn immediately after any flag changes state,
    // in case it changed the filter result. This state will
    // receive the event for its own state change, and immediately
    // transition out if respawn is not prevented by the filter.
    tryRespawn(false);
  }

  @Override
  public String getStatusSymbol(Party viewer) {
    return Flag.RESPAWNING_SYMBOL;
  }

  @Override
  public org.bukkit.ChatColor getStatusColor(Party viewer) {
    if (this.flag.getDefinition().hasMultipleCarriers()) {
      return org.bukkit.ChatColor.WHITE;
    } else {
      return super.getStatusColor(viewer);
    }
  }
}
