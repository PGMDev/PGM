package tc.oc.pgm.flag.state;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.Post;
import tc.oc.pgm.flag.event.FlagCaptureEvent;
import tc.oc.pgm.util.nms.NMSHacks;

/**
 * Base class for flag states in which the banner is physically present somewhere in the map (i.e.
 * most of them).
 */
public abstract class Spawned extends BaseState {

  protected int particleClock;

  public Spawned(Flag flag, Post post) {
    super(flag, post);
  }

  // Location the flag must travel from to respawn
  public abstract Location getLocation();

  // True if capturing another flag can return this one in the current state
  public abstract boolean isRecoverable();

  protected void recover() {
    if (isRecoverable()) {
      this.flag.transition(new Respawning(this.flag, this.post, this.getLocation(), false, false));
    }
  }

  @Override
  public void onEvent(FlagCaptureEvent event) {
    super.onEvent(event);

    // Not crazy about using an event for game logic, but this is by far the simplest way to do it
    if (event.getNet().getRecoverableFlags().contains(this.flag.getDefinition())) {
      this.recover();
    }
  }

  protected boolean canSeeParticles(Player player) {
    return true;
  }

  @Override
  public void tickLoaded() {
    super.tickLoaded();

    this.particleClock++;

    if (this.flag.getDefinition().showBeam()) {
      for (MatchPlayer player : flag.getMatch().getPlayers()) {
        Player bukkitPlayer = player.getBukkit();
        if (this.canSeeParticles(bukkitPlayer)) {
          NMSHacks.spawnFlagParticles(bukkitPlayer, flag.getDyeColor(), this.getLocation());
        }
      }
    }
  }
}
