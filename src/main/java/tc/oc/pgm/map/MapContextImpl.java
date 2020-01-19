package tc.oc.pgm.map;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapInfoExtra;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.module.ModuleContext;

public class MapContextImpl extends MapInfoImpl implements MapContext {

  private final MapSource source;
  private final List<MapModule> modules;
  private final String genre;
  private final Set<String> tags;
  private final List<Integer> teamLimits;
  private final int playerLimit;

  public MapContextImpl(MapInfo info, MapSource source, ModuleContext<MapModule> context) {
    super(info);
    this.source = checkNotNull(source);
    this.modules = ImmutableList.copyOf(checkNotNull(context).getModules());

    String genre = null;
    Set<String> tags = new HashSet<>();
    List<Integer> teamLimits = null;
    int playerLimit = 0;

    for (MapModule module : context.getModules()) {
      if (!(module instanceof MapInfoExtra)) continue;
      final MapInfoExtra extra = (MapInfoExtra) module;

      if (extra.getGenre() != null) genre = extra.getGenre();
      if (extra.getTeamLimits() != null) teamLimits = new LinkedList<>(extra.getTeamLimits());
      if (extra.getPlayerLimit() > 0) playerLimit = extra.getPlayerLimit();

      tags.addAll(extra.getTags());
    }

    this.genre = genre == null ? "Match" : genre;
    this.tags = Collections.unmodifiableSet(new LinkedHashSet<>(tags));
    this.teamLimits = teamLimits == null ? null : Collections.unmodifiableList(teamLimits);
    this.playerLimit = playerLimit;
  }

  @Override
  public String getGenre() {
    return genre;
  }

  @Override
  public Collection<String> getTags() {
    return tags;
  }

  @Override
  public Collection<Integer> getTeamLimits() {
    return teamLimits;
  }

  @Override
  public int getPlayerLimit() {
    return playerLimit;
  }

  @Override
  public Collection<MapModule> getModules() {
    return modules;
  }

  @Override
  public <N extends MapModule> N getModule(Class<? extends N> key) {
    // FIXME: This is not efficient, but is not currently used anywhere
    return (N)
        modules.stream().filter(module -> module.getClass().equals(key)).findFirst().orElse(null);
  }

  @Override
  public MapSource getSource() {
    return source;
  }

  @Override
  protected void finalize() throws Throwable {
    PGM.get().getLogger().info("Finalize: " + this);
    super.finalize();
  }
}
