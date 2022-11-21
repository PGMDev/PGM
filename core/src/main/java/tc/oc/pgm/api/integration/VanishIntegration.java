package tc.oc.pgm.api.integration;

import org.bukkit.entity.Player;
import tc.oc.pgm.api.player.MatchPlayer;

public interface VanishIntegration {

  boolean isVanished(Player player);

  boolean setVanished(MatchPlayer player, boolean vanish, boolean quiet);
}
