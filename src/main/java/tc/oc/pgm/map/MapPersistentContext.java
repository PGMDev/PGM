package tc.oc.pgm.map;

import static com.google.common.base.Preconditions.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import tc.oc.pgm.maptag.MapTag;
import tc.oc.util.SemanticVersion;

/**
 * This is a replacement for classes that need information from MapModuleContext to use.
 * MapModuleContext will not be guaranteed to be available if a match isn't being played for the
 * map, but MapPersistentContext will always contain basic information required to reference the map
 * (like map name, authors, or player amount).
 */
public class MapPersistentContext {
  private final SemanticVersion proto;
  private final MapInfo mapInfo;
  private final List<Integer> maxPlayers;
  private final int totalMaxPlayers;
  private final Set<MapTag> mapTags;

  public MapPersistentContext(
      SemanticVersion proto, MapInfo mapInfo, List<Integer> maxPlayers, Set<MapTag> mapTags) {
    maxPlayers.sort(Comparator.reverseOrder()); // reverse so that bigger teams are always first
    this.proto = proto;
    this.mapInfo = checkNotNull(mapInfo);
    this.maxPlayers = ImmutableList.copyOf(maxPlayers);
    this.totalMaxPlayers = maxPlayers.stream().mapToInt(Integer::intValue).sum();
    this.mapTags = ImmutableSortedSet.copyOf(Comparator.naturalOrder(), checkNotNull(mapTags));
  }

  public SemanticVersion getProto() {
    return proto;
  }

  public List<Integer> getMaxPlayers() {
    return maxPlayers;
  }

  public int getTotalMaxPlayers() {
    return totalMaxPlayers;
  }

  public MapInfo getInfo() {
    return mapInfo;
  }

  public Set<MapTag> getMapTags() {
    return mapTags;
  }
}
