package tc.oc.pgm.ffa;

import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.NameTagVisibility;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.start.StartModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

@ModuleDescription(
    name = "free-for-all",
    requires = {StartModule.class},
    follows = {TeamModule.class})
public class FreeForAllModule extends MapModule {

  private final FreeForAllOptions options;

  public FreeForAllModule(FreeForAllOptions options) {
    this.options = options;
  }

  public FreeForAllOptions getOptions() {
    return options;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new FreeForAllMatchModule(match, options);
  }

  public static FreeForAllModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    Element elPlayers = doc.getRootElement().getChild("players");

    if (context.hasModule(TeamModule.class)) {
      if (elPlayers != null)
        throw new InvalidXMLException("Cannot combine <players> and <teams>", elPlayers);
      return null;
    } else {
      int minPlayers = Config.minimumPlayers();
      int maxPlayers = Bukkit.getMaxPlayers();
      int maxOverfill = maxPlayers;
      NameTagVisibility nameTagVisibility = NameTagVisibility.ALWAYS;

      if (elPlayers != null) {
        minPlayers = XMLUtils.parseNumber(elPlayers.getAttribute("min"), Integer.class, minPlayers);
        maxPlayers = XMLUtils.parseNumber(elPlayers.getAttribute("max"), Integer.class, maxPlayers);
        maxOverfill =
            XMLUtils.parseNumber(
                elPlayers.getAttribute("max-overfill"), Integer.class, maxOverfill);
        nameTagVisibility =
            XMLUtils.parseNameTagVisibility(
                Node.fromAttr(elPlayers, "show-name-tags"), nameTagVisibility);
      }

      return new FreeForAllModule(
          new FreeForAllOptions(minPlayers, maxPlayers, maxOverfill, nameTagVisibility));
    }
  }
}
