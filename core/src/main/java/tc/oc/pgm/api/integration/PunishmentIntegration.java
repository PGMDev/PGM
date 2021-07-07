package tc.oc.pgm.api.integration;

import org.bukkit.entity.Player;

public interface PunishmentIntegration {

  boolean isMuted(Player player);

  String getMuteReason(Player player);

  boolean isHidden(Player player);
}
