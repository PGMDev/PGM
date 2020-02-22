package tc.oc.pgm.flag;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.util.xml.InvalidXMLException;

public class FlagModule implements MapModule {

  private static final Collection<MapTag> TAGS =
      ImmutableList.of(MapTag.create("flag", "Capture the Flag", true, false));
  private final ImmutableList<Post> posts;
  private final ImmutableList<Net> nets;
  private final ImmutableList<FlagDefinition> flags;

  public FlagModule(List<Post> posts, List<Net> nets, List<FlagDefinition> flags) {
    this.posts = ImmutableList.copyOf(posts);
    this.nets = ImmutableList.copyOf(nets);
    this.flags = ImmutableList.copyOf(flags);
  }

  @Override
  public Collection<Class<? extends MatchModule>> getSoftDependencies() {
    return ImmutableList.of(GoalMatchModule.class);
  }

  @Override
  public MatchModule createMatchModule(Match match) throws ModuleLoadException {
    return new FlagMatchModule(match, this.nets, this.flags);
  }

  @Override
  public Collection<MapTag> getTags() {
    return TAGS;
  }

  public static class Factory implements MapModuleFactory<FlagModule> {
    @Override
    public Collection<Class<? extends MapModule>> getWeakDependencies() {
      return ImmutableList.of(TeamModule.class, RegionModule.class, FilterModule.class);
    }

    @Override
    public FlagModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      return new FlagParser(factory).parse(doc);
    }
  }

  @Override
  public void postParse(MapFactory factory, Logger logger, Document doc)
      throws InvalidXMLException {
    for (FlagDefinition flag : flags) {
      if (flag.getCarryKit() != null && !flag.getCarryKit().isRemovable()) {
        throw new InvalidXMLException(
            "carry-kit is not removable", factory.getFeatures().getNode(flag));
      }
    }
  }
}
