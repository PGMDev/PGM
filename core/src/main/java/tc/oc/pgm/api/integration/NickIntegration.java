package tc.oc.pgm.api.integration;

import javax.annotation.Nullable;
import org.bukkit.entity.Player;

public interface NickIntegration {

  @Nullable
  String getNick(Player player);
}
