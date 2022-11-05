package tc.oc.pgm.rotation.pools;

import com.google.common.collect.Iterators;
import java.util.Iterator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.util.Aliased;

public enum MapPoolType implements Aliased {
  ORDERED("Rotation", Rotation::new),
  VOTED("Voted", VotingPool::new),
  SHUFFLED("Shuffled", RandomMapPool::new);

  private final String name;
  private final PoolFactory factory;

  MapPoolType(String name, PoolFactory factory) {
    this.name = name;
    this.factory = factory;
  }

  public String getName() {
    return name;
  }

  public static MapPoolType of(String str) {
    for (MapPoolType type : values()) {
      if (type.name().equalsIgnoreCase(str)) return type;
    }
    return null;
  }

  public static MapPool buildPool(
      MapPoolManager manager, FileConfiguration configFile, String key) {
    ConfigurationSection section = configFile.getConfigurationSection("pools." + key);
    String typeStr = section.getString("type");
    MapPoolType type = of(typeStr);
    if (type == null) {
      PGM.get().getLogger().severe("Invalid map pool type for " + key + ": '" + typeStr + "'");
      return new DisabledMapPool(manager, section, key);
    }
    return type.factory.build(type, key, manager, section);
  }

  @NotNull
  @Override
  public Iterator<String> iterator() {
    return Iterators.forArray(name(), name);
  }

  private interface PoolFactory {
    MapPool build(
        MapPoolType type, String name, MapPoolManager manager, ConfigurationSection section);
  }
}
