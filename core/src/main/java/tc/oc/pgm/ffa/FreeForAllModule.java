package tc.oc.pgm.ffa;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.NameTagVisibility;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class FreeForAllModule implements MapModule<FreeForAllMatchModule> {

  private static final Collection<MapTag> TAGS =
      ImmutableList.of(new MapTag("ffa", "Free for All", false, false));
  private final FreeForAllOptions options;

  public FreeForAllModule(FreeForAllOptions options) {
    this.options = options;
  }

  public FreeForAllOptions getOptions() {
    return options;
  }

  @Override
  public Collection<Class<? extends MatchModule>> getHardDependencies() {
    return ImmutableList.of(JoinMatchModule.class, StartMatchModule.class);
  }

  @Override
  public Collection<MapTag> getTags() {
    return TAGS;
  }

  @Override
  public FreeForAllMatchModule createMatchModule(Match match) {
    return new FreeForAllMatchModule(match, options);
  }

  public static class Factory implements MapModuleFactory<FreeForAllModule> {
    @Override
    public Collection<Class<? extends MapModule<?>>> getWeakDependencies() {
      return ImmutableList.of(TeamModule.class);
    }

    @Override
    public FreeForAllModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Element elPlayers = doc.getRootElement().getChild("players");

      if (factory.hasModule(TeamModule.class)) {
        if (elPlayers != null)
          throw new InvalidXMLException("Cannot combine <players> and <teams>", elPlayers);
        return null;
      } else {
        int minPlayers = (int) PGM.get().getConfiguration().getMinimumPlayers();
        int maxPlayers = Bukkit.getMaxPlayers();
        int maxOverfill = maxPlayers;
        NameTagVisibility nameTagVisibility = NameTagVisibility.ALWAYS;
        boolean colors = false;

        if (elPlayers != null) {
          minPlayers =
              XMLUtils.parseNumber(elPlayers.getAttribute("min"), Integer.class, minPlayers);
          maxPlayers =
              XMLUtils.parseNumber(elPlayers.getAttribute("max"), Integer.class, maxPlayers);
          maxOverfill =
              XMLUtils.parseNumber(
                  elPlayers.getAttribute("max-overfill"), Integer.class, maxOverfill);
          nameTagVisibility =
              XMLUtils.parseNameTagVisibility(
                  Node.fromAttr(elPlayers, "show-name-tags"), nameTagVisibility);
          colors = XMLUtils.parseBoolean(Node.fromAttr(elPlayers, "colors"), colors);
        }

        return new FreeForAllModule(
            new FreeForAllOptions(minPlayers, maxPlayers, maxOverfill, nameTagVisibility, colors));
      }
    }
  }
}
