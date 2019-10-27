package tc.oc.pgm.map;

import tc.oc.util.SemanticVersion;

/**
 * This is a replacement for classes that need information from MapModuleContext to use.
 * MapModuleContext will not be guaranteed to be available if a match isn't being played for the
 * map, but MapPersistentContext will always contain basic information required to reference the map
 * (like map name, authors, or player amount).
 */
public class MapPersistentContext {
  private SemanticVersion proto;
  private MapInfo mapInfo;
  private int maxPlayers;

  public MapPersistentContext(SemanticVersion proto, MapInfo mapInfo, int maxPlayers) {
    this.proto = proto;
    this.mapInfo = mapInfo;
    this.maxPlayers = maxPlayers;
  }

  public SemanticVersion getProto() {
    return proto;
  }

  public int getMaxPlayers() {
    return maxPlayers;
  }

  public MapInfo getInfo() {
    return mapInfo;
  }
}
