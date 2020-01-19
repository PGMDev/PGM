package tc.oc.pgm.map;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.Taggable;
import tc.oc.pgm.ffa.FreeForAllModule;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamModule;

public class MapContextImpl extends MapInfoImpl implements MapContext {

  private final MapSource source;
  private final List<MapModule> modules;

  public MapContextImpl(MapInfo info, MapSource source, Collection<MapModule> modules) {
    super(info);
    this.source = checkNotNull(source);
    this.modules = ImmutableList.copyOf(checkNotNull(modules));

    for (MapModule module : this.modules) {
      if (module instanceof Taggable) {
        this.tags.addAll(((Taggable) module).getTags());
      }

      if (module instanceof TeamModule) {
        this.players.clear();
        this.players.addAll(
            Collections2.transform(((TeamModule) module).getTeams(), TeamFactory::getMaxPlayers));
      }

      if (module instanceof FreeForAllModule) {
        this.players.clear();
        this.players.add(((FreeForAllModule) module).getOptions().maxPlayers);
      }
    }
  }

  @Override
  public MapSource getSource() {
    return source;
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

  @Override
  protected void finalize() throws Throwable {
    PGM.get().getLogger().info("Finalize: " + this);
    super.finalize();
  }
}
