package tc.oc.pgm.flag.state;

import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.Post;
import tc.oc.pgm.flag.event.FlagCaptureEvent;

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
      List<Player> players =
          flag.getMatch().getPlayers().stream()
              .map(MatchPlayer::getBukkit)
              .filter(this::canSeeParticles)
              .collect(Collectors.toList());
      Color color = flag.getColor();

      Material material = Material.getMaterial(flag.getDyeColor().name() + "_WOOL");
      if (material != null) {
        BlockData blockData = Bukkit.createBlockData(material);
        Particle.BLOCK_DUST
            .builder()
            .data(blockData)
            .count(40)
            .receivers(players)
            .location(this.getLocation().clone().add(0, 0, 2))
            .offset(0, 56, 0)
            .force(true)
            .spawn();
      }
    }
  }
}
