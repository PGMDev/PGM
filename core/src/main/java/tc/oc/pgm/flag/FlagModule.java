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
import tc.oc.pgm.flag.post.PostDefinition;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.xml.InvalidXMLException;

public class FlagModule implements MapModule<FlagMatchModule> {

  private static final Collection<MapTag> TAGS =
      ImmutableList.of(new MapTag("ctf", "flag", "Capture the Flag", true, false));
  private final ImmutableList<PostDefinition> posts;
  private final ImmutableList<NetDefinition> nets;
  private final ImmutableList<FlagDefinition> flags;

  public FlagModule(
      List<PostDefinition> posts, List<NetDefinition> nets, List<FlagDefinition> flags) {
    this.posts = ImmutableList.copyOf(posts);
    this.nets = ImmutableList.copyOf(nets);
    this.flags = ImmutableList.copyOf(flags);
  }

  @Override
  public Collection<Class<? extends MatchModule>> getSoftDependencies() {
    return ImmutableList.of(GoalMatchModule.class);
  }

  @Override
  public FlagMatchModule createMatchModule(Match match) throws ModuleLoadException {
    return new FlagMatchModule(match, this.posts, this.nets, this.flags);
  }

  @Override
  public Collection<MapTag> getTags() {
    return TAGS;
  }

  public static class Factory implements MapModuleFactory<FlagModule> {
    @Override
    public Collection<Class<? extends MapModule<?>>> getWeakDependencies() {
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
