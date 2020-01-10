package tc.oc.pgm.modules;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;

public class MaxBuildHeightMatchModule implements MatchModule, Listener {

  private final int buildHeight;

  public MaxBuildHeightMatchModule(Match match, int buildHeight) {
    this.buildHeight = buildHeight;
  }

  @EventHandler(ignoreCancelled = true)
  public void checkBuildHeight(BlockTransformEvent event) {
    if (event.getNewState().getType() != Material.AIR) {
      if (event.getNewState().getY() >= this.buildHeight) {
        event.setCancelled(
            true,
            new PersonalizedTranslatable(
                "match.maxBuildHeightWarning",
                new PersonalizedText(
                    String.valueOf(buildHeight), net.md_5.bungee.api.ChatColor.AQUA)));
      }
    }
  }
}
