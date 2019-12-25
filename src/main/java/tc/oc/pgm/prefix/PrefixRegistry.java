package tc.oc.pgm.prefix;

import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.party.Party;

public interface PrefixRegistry {

  void updateDisplayName(UUID uuid);

  String getPrefixedName(Player player, Party party);

  void setPrefix(UUID uuid, String prefix);

  @Nullable
  String getPrefix(UUID uuid);

  void removePlayer(UUID uuid);
}
