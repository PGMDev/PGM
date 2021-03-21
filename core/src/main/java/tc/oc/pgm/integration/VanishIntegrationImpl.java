package tc.oc.pgm.integration;

import org.bukkit.entity.Player;
import tc.oc.pgm.api.integration.VanishIntegration;
import tc.oc.pgm.api.player.MatchPlayer;

public class VanishIntegrationImpl implements VanishIntegration {

  @Override
  public boolean isVanished(Player player) {
    return false;
  }

  @Override
  public boolean setVanished(MatchPlayer player, boolean vanish, boolean quiet) {
    return false;
  }
}
