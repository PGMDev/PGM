package tc.oc.pgm.prefix;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.Config.Prefixes;
import tc.oc.pgm.Config.Prefixes.Prefix;

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
    for (Prefix prefix : Prefixes.getPrefixes().values()) {
      if (player.hasPermission(prefix.permission)) {
        return prefix.toString();
      }
    }
    return "";
  }
}
