package tc.oc.pgm.flair;

import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.party.Party;

public interface FlairRegistry {

  void updateDisplayName(UUID uuid);

  String getFlairedName(Player player, Party party);

  void setFlair(UUID uuid, String flair);

  @Nullable
  String getFlair(UUID uuid);

  void removeFlair(UUID uuid);
}
