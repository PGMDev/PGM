package tc.oc.pgm.modules;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;
import tc.oc.block.BlockVectors;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.event.CoarsePlayerMoveEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.regions.Region;
import tc.oc.pgm.util.MatchPlayers;

public class PlayableRegionMatchModule implements MatchModule, Listener {

  private final Match match;
  private final Region playableRegion;

  public PlayableRegionMatchModule(Match match, Region playableRegion) {
    this.match = match;
    this.playableRegion = playableRegion;
  }

  @EventHandler(ignoreCancelled = true)
  public void onBlockTransformEvent(final BlockTransformEvent event) {
    Location center = BlockVectors.center(event.getNewState());
    if (!this.playableRegion.contains(center.toVector())) {
      event.setCancelled(
          true, new PersonalizedTranslatable("match.playableArea.blockInteractWarning"));
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onCoarsePlayerMoveEvent(final CoarsePlayerMoveEvent event) {
    MatchPlayer player = this.match.getPlayer(event.getPlayer());

    if (MatchPlayers.canInteract(player)) {
      Vector from = BlockVectors.center(event.getFrom()).toVector();
      Vector to = BlockVectors.center(event.getTo()).toVector();
      if (this.playableRegion.contains(from) && !this.playableRegion.contains(to)) {
        event.setCancelled(
            true, new PersonalizedTranslatable("match.playableArea.leaveAreaWarning"));
      }
    }
  }
}
