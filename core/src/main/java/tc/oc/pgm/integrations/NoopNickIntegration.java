package tc.oc.pgm.integrations;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.integration.NickIntegration;

public class NoopNickIntegration implements NickIntegration {

  @Override
  public @Nullable String getNick(Player player) {
    return null;
  }
}
