package tc.oc.pgm.api.integration;

import org.bukkit.entity.Player;

public interface NickIntegration {

  String getNick(Player player);

  boolean hasNick(Player player);

  public class NickIntegrationImpl implements NickIntegration {
    @Override
    public String getNick(Player player) {
      return null;
    }

    @Override
    public boolean hasNick(Player player) {
      return false;
    }
  }
}
