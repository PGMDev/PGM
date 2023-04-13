package tc.oc.pgm.api.integration;

import java.util.Collection;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface SquadIntegration {

  boolean areInSquad(Player a, Player b);

  @Nullable
  Collection<UUID> getSquad(Player player);
}
