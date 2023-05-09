package tc.oc.pgm.map;

import static tc.oc.pgm.util.Assert.assertNotNull;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.List;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.ffa.FreeForAllModule;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamModule;

public class MapContextImpl implements MapContext {
  private static final MapTag TERRAIN = new MapTag("terrain", "Terrain", false, true);

  private final MapInfo info;
  private final List<MapModule> modules;

  public MapContextImpl(MapInfoImpl info, Collection<MapModule<?>> modules) {
    info.context = new SoftReference<>(this);
    this.info = info;
    this.modules = ImmutableList.copyOf(assertNotNull(modules));

    for (MapModule<?> module : this.modules) {
      info.tags.addAll(module.getTags());

      if (module instanceof TeamModule) {
        info.players.clear();
        info.players.addAll(
            Collections2.transform(((TeamModule) module).getTeams(), TeamFactory::getMaxPlayers));
      }

      if (module instanceof FreeForAllModule) {
        info.players.clear();
        info.players.add(((FreeForAllModule) module).getOptions().maxPlayers);
      }
    }

    if (info.getWorld().hasTerrain()) {
      info.tags.add(TERRAIN);
    }
  }

  public MapInfo getInfo() {
    return info;
  }

  @Override
  public Collection<MapModule> getModules() {
    return modules;
  }

  @Override
  public <N extends MapModule> N getModule(Class<? extends N> key) {
    throw new UnsupportedOperationException(
        "Not allowed to query for specific modules in " + getClass().getSimpleName());
  }
}
