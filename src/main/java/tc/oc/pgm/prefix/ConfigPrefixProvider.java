package tc.oc.pgm.prefix;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.bukkit.Bukkit;
import tc.oc.pgm.Config.Prefixes;
import tc.oc.pgm.Config.Prefixes.Prefix;

public class ConfigPrefixProvider implements PrefixProvider {

  private final Map<UUID, String> prefixMap = new HashMap<UUID, String>();

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
    if (Bukkit.getPlayer(uuid) == null) {
      return "";
    }
    StringBuilder prefix = new StringBuilder();
    for (Entry<String, Prefix> entry : Prefixes.getPrefixes().entrySet()) {
      if (Bukkit.getPlayer(uuid).hasPermission("pgm.group." + entry.getKey())) {
        prefix.append(entry.getValue().toString());
      }
    }
    return prefix.toString();
  }
}
