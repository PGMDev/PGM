package tc.oc.pgm.maptag;

import static com.google.common.base.Preconditions.*;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import tc.oc.pgm.map.PGMMap;

public class MapTagsCondition implements Predicate<PGMMap> {

  private final Map<MapTag, Boolean> params;

  public MapTagsCondition(Map<MapTag, Boolean> params) {
    this.params = ImmutableMap.copyOf(checkNotNull(params));
  }

  public Map<MapTag, Boolean> getParams() {
    return this.params;
  }

  /**
   * Perform AND operation on this condition.
   *
   * @param map map to perform operation on
   * @return {@code true} whether AND operation is successful, otherwise {@code false}
   */
  @Override
  public boolean test(PGMMap map) {
    checkNotNull(map);

    Set<MapTag> mapTags = map.getPersistentContext().getMapTags();
    for (Map.Entry<MapTag, Boolean> entry : params.entrySet()) {
      if (mapTags.contains(entry.getKey()) != entry.getValue()) {
        return false;
      }
    }

    return true;
  }
}
