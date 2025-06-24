package tc.oc.pgm.map;

import static tc.oc.pgm.util.Assert.assertNotNull;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapModule;

public class MapContextImpl implements MapContext {

  private final MapInfo info;
  private final List<MapModule> modules;

  public MapContextImpl(MapInfoImpl info, Collection<MapModule<?>> modules) {
    this.info = info;
    this.modules = ImmutableList.copyOf(assertNotNull(modules));

    // Update the map info with stuff derived from modules, like team sizes or tags.
    info.setContext(this);
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
