package tc.oc.pgm.api.integration;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface NickIntegration {

  @Nullable
  String getNick(Player player);
}
