package tc.oc.pgm.prefix;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.PrefixChangeEvent;
import tc.oc.pgm.api.prefix.PrefixProvider;

public class ConfigPrefixProvider implements PrefixProvider {

  private final Map<UUID, String> prefixMap = new HashMap<>();

  @Override
  public void setPrefix(UUID uuid, String prefix) {
    Bukkit.getPluginManager()
        .callEvent(new PrefixChangeEvent(uuid, prefixMap.put(uuid, prefix), prefix));
  }

  @Override
  public String getPrefix(UUID uuid) {
    if (!prefixMap.containsKey(uuid)) {
      setPrefix(uuid, getPrefixFromConfig(uuid));
    }
    return prefixMap.get(uuid);
  }

  @Override
  public void removePlayer(UUID uuid) {
    prefixMap.remove(uuid);
  }

  private String getPrefixFromConfig(UUID uuid) {
    final Player player = Bukkit.getPlayer(uuid);
    if (player == null) return "";

    StringBuilder builder = new StringBuilder();
    for (Config.Group group : PGM.get().getConfiguration().getGroups()) {
      if (player.hasPermission(group.getPermission())) {
        final String prefix = group.getPrefix();
        if (prefix != null) builder.append(prefix);
      }
    }
    return builder.toString();
  }
}
