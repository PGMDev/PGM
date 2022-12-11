package tc.oc.pgm.integrations;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.integration.PunishmentIntegration;

public class NoopPunishmentIntegration implements PunishmentIntegration {

  @Override
  public boolean isMuted(Player player) {
    return false;
  }

  @Override
  public @Nullable String getMuteReason(Player player) {
    return null;
  }
}
