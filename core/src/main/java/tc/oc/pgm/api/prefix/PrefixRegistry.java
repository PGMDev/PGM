package tc.oc.pgm.api.prefix;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.event.PrefixChangeEvent;
import tc.oc.pgm.api.party.Party;

public interface PrefixRegistry extends Listener {

  void onPrefixChange(PrefixChangeEvent event);

  String getPrefixedName(Player player, Party party);

  String getPrefix(UUID uuid);

  void removePlayer(UUID uuid);

  void setPrefixProvider(PrefixProvider prefixProvider);

  PrefixProvider getPrefixProvider();
}
