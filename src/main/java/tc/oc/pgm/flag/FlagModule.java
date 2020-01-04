package tc.oc.pgm.flag;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.goals.GoalModule;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.maptag.MapTag;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.module.ModuleLoadException;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(
    name = "Capture the Flag",
    follows = {TeamModule.class, RegionModule.class, FilterModule.class, GoalModule.class})
public class FlagModule extends MapModule<FlagMatchModule> {

  private static final MapTag FLAG_TAG = MapTag.forName("flag");

  private final ImmutableList<Post> posts;
  private final ImmutableList<Net> nets;
  private final ImmutableList<FlagDefinition> flags;

  public FlagModule(List<Post> posts, List<Net> nets, List<FlagDefinition> flags) {
    this.posts = ImmutableList.copyOf(posts);
    this.nets = ImmutableList.copyOf(nets);
    this.flags = ImmutableList.copyOf(flags);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void loadTags(Set tags) {
    tags.add(FLAG_TAG);
  }

  @Override
  public FlagMatchModule createMatchModule(Match match) throws ModuleLoadException {
    return new FlagMatchModule(match, this.nets, this.flags);
  }
  // ---------------------
  // ---- XML Parsing ----
  // ---------------------

  public static FlagModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    return new FlagParser(context, logger).parse(doc);
  }

  @Override
  public void postParse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    for (FlagDefinition flag : flags) {
      if (flag.getCarryKit() != null && !flag.getCarryKit().isRemovable()) {
        throw new InvalidXMLException(
            "carry-kit is not removable", context.features().getNode(flag));
      }
    }
  }
}
