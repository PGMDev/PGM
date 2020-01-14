package tc.oc.pgm.map;

import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapInfoExtra;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.module.ModuleContext;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class MapInfoExtraImpl extends MapInfoImpl implements MapInfoExtra {

  private final String genre;
  private final Set<String> tags;
  private final List<Integer> teamLimits;
  private final int playerLimit;

  public MapInfoExtraImpl(MapInfo info, ModuleContext<MapModule> context) {
    super(info);

    String genre = null;
    Set<String> tags = new HashSet<>();
    List<Integer> teamLimits = null;
    int playerLimit = 0;

    for(MapModule module : checkNotNull(context).getModules().values()) {
      if (!(module instanceof MapInfoExtra)) continue;
      final MapInfoExtra extra = (MapInfoExtra) module;

      if (extra.getGenre() != null) genre = extra.getGenre();
      if (extra.getTeamLimits() != null) teamLimits = new LinkedList<>(extra.getTeamLimits());
      if (extra.getPlayerLimit() > 0) playerLimit = extra.getPlayerLimit();

      tags.addAll(extra.getTags());
    }

    this.genre = genre;
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
}
