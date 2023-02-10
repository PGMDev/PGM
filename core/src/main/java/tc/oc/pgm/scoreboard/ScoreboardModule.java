package tc.oc.pgm.scoreboard;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.ffa.FreeForAllModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class ScoreboardModule implements MapModule<ScoreboardMatchModule> {

  private final ScoreboardDisplayItem belowName;

  private ScoreboardModule(ScoreboardDisplayItem belowName) {
    this.belowName = belowName;
  }

  public static class Factory implements MapModuleFactory<ScoreboardModule> {

    @Override
    public Collection<Class<? extends MapModule<?>>> getWeakDependencies() {
      return ImmutableList.of(TeamModule.class, FreeForAllModule.class);
    }

    @Override
    public @Nullable ScoreboardModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Element elScoreboard = doc.getRootElement().getChild("scoreboard");

      ScoreboardDisplayItem belowName = ScoreboardDisplayItem.NONE;

      if (elScoreboard != null) {
        belowName =
            XMLUtils.parseEnum(
                elScoreboard.getAttribute("below-name"),
                ScoreboardDisplayItem.class,
                "scoreboard display item");
      }

      return new ScoreboardModule(belowName);
    }
  }

  @Override
  public @Nullable ScoreboardMatchModule createMatchModule(Match match) throws ModuleLoadException {
    return new ScoreboardMatchModule(match, belowName);
  }
}
