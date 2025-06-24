package tc.oc.pgm.rotation.pools;

import static tc.oc.pgm.api.map.MapSource.DEFAULT_VARIANT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.map.VariantInfo;

public class MapParser {
  private final MapLibrary maps = PGM.get().getMapLibrary();
  private final String poolName;
  private final List<String> variantIds;
  private final ArrayList<MapInfo> mapList = new ArrayList<>();
  private final Map<MapInfo, Double> weights = new HashMap<>();

  public static MapParser parse(String poolName, ConfigurationSection section) {
    var parser = new MapParser(poolName, section.getStringList("variants"));
    if (section.contains("maps")) parser.parseSection(section, 1d);
    return parser;
  }

  private MapParser(String poolName, List<String> variants) {
    this.poolName = poolName;
    if (variants != null) {
      int def = variants.indexOf(DEFAULT_VARIANT);
      if (def >= 0) variants = variants.subList(0, def);
      if (variants.isEmpty()) variants = null;
    }
    this.variantIds = variants;
  }

  public List<MapInfo> getMaps() {
    return mapList;
  }

  public double getWeight(MapInfo info) {
    return weights.getOrDefault(info, 1d);
  }

  private void parseSection(ConfigurationSection parent, double parentWeight) {
    Set<String> keys;
    double weight;
    if (parent.contains("maps")) {
      keys = Set.of("maps");
      weight = parent.getDouble("weight", parentWeight);
    } else {
      keys = parent.getKeys(false);
      weight = parentWeight;
    }
    keys.forEach(k -> {
      if (parent.isConfigurationSection(k)) parseSection(parent.getConfigurationSection(k), weight);
      else parseMapList(parent.getStringList(k), weight);
    });
  }

  private void parseMapList(List<String> mapNames, double weight) {
    if (mapNames == null) return;

    mapList.ensureCapacity(mapList.size() + mapNames.size());

    for (String mapName : mapNames) {
      MapInfo map = maps.getMap(mapName);
      if (map != null) {
        map = getVariant(map);
        weights.computeIfAbsent(map, k -> {
          mapList.add(k);
          return weight;
        });
      } else {
        PGM.get()
            .getLogger()
            .warning(
                "[MapPool] [" + poolName + "] " + mapName + " not found in map repo. Ignoring...");
      }
    }
  }

  private MapInfo getVariant(MapInfo map) {
    if (variantIds == null || !map.getVariantId().equals(DEFAULT_VARIANT)) return map;

    Map<String, ? extends VariantInfo> variants = map.getVariants();
    for (String varId : variantIds) {
      VariantInfo variant = variants.get(varId);
      if (variant == null) continue;
      MapInfo variantMap = maps.getMapById(variant.getId());
      if (variantMap != null) return variantMap;
      // Should never happen
      PGM.get()
          .getLogger()
          .warning("[MapPool] Failed to get map " + variant.getId() + ". Moving on...");
    }
    return map;
  }
}
