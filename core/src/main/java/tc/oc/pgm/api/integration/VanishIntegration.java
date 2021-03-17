package tc.oc.pgm.api.integration;

import java.util.Collection;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.player.MatchPlayer;

public interface VanishIntegration {

  boolean isVanished(Player player);

  default Collection<Player> getVanished() {
    return Bukkit.getOnlinePlayers().stream().filter(this::isVanished).collect(Collectors.toList());
  }

  boolean setVanished(MatchPlayer player, boolean vanish, boolean quiet);

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
}
