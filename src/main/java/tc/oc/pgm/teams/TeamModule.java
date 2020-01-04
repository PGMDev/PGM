package tc.oc.pgm.teams;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.NameTagVisibility;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.maptag.MapTag;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.modules.InfoModule;
import tc.oc.pgm.start.StartModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.util.StringUtils;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

@ModuleDescription(
    name = "Team",
    requires = {InfoModule.class, StartModule.class})
public class TeamModule extends MapModule<TeamMatchModule> {

  private static final MapTag _4TEAMS_TAG = MapTag.forName("4teams");
  private static final MapTag EVENTEAMS_TAG = MapTag.forName("eventeams");
  private static final MapTag TEAMS_TAG = MapTag.forName("teams");

  private final Set<TeamFactory> teams;
  private final @Nullable Boolean requireEven;

  public TeamModule(Set<TeamFactory> teams, @Nullable Boolean requireEven) {
    this.teams = teams;
    this.requireEven = requireEven;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{teams=[" + Joiner.on(", ").join(teams) + "]}";
  }

  @Override
  public void loadTags(Set<MapTag> tags) {
    if (teams.size() == 4) tags.add(_4TEAMS_TAG);
    if (requireEven != null && requireEven) tags.add(EVENTEAMS_TAG);
    tags.add(TEAMS_TAG);
  }

  @Override
  public TeamMatchModule createMatchModule(Match match) {
    return new TeamMatchModule(
        match, teams, requireEven != null ? requireEven : Config.Teams.requireEven());
  }

  /**
   * Gets the set of TeamInfo instances this map provides.
   *
   * @return Teams this map supports.
   */
  public Set<TeamFactory> getTeams() {
    return teams;
  }

  public TeamFactory getTeamByName(String name) {
    return StringUtils.bestFuzzyMatch(name, getTeams(), 0.9);
  }

  // ---------------------
  // ---- XML Parsing ----
  // ---------------------

  public TeamFactory parseTeam(Attribute attr, MapModuleContext context)
      throws InvalidXMLException {
    if (attr == null) {
      return null;
    }
    String name = attr.getValue();
    TeamFactory team = Teams.getTeam(name, context);

    if (team == null) {
      throw new InvalidXMLException("unknown team '" + name + "'", attr);
    }
    return team;
  }

  public static TeamModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    Set<TeamFactory> teamFactories = Sets.newLinkedHashSet();
    Boolean requireEven = null;

    for (Element teamRootElement : doc.getRootElement().getChildren("teams")) {
      requireEven = XMLUtils.parseBoolean(teamRootElement.getAttribute("even"), requireEven);

      for (Element teamElement : teamRootElement.getChildren("team")) {
        teamFactories.add(parseTeamDefinition(teamElement, context));
      }
    }

    return teamFactories.isEmpty() ? null : new TeamModule(teamFactories, requireEven);
  }

  private static TeamFactory parseTeamDefinition(Element el, MapModuleContext context)
      throws InvalidXMLException {
    String id = el.getAttributeValue("id");

    String name = el.getTextNormalize();
    if ("".equals(name)) {
      throw new InvalidXMLException("Team name cannot be blank", el);
    }

    boolean plural = XMLUtils.parseBoolean(el.getAttribute("plural"), false);

    ChatColor color = XMLUtils.parseChatColor(Node.fromAttr(el, "color"), ChatColor.WHITE);
    ChatColor overheadColor = XMLUtils.parseChatColor(Node.fromAttr(el, "overhead-color"), null);
    NameTagVisibility nameTagVisibility =
        XMLUtils.parseNameTagVisibility(
            Node.fromAttr(el, "show-name-tags"), NameTagVisibility.ALWAYS);

    int minPlayers =
        XMLUtils.parseNumber(
            Node.fromAttr(el, "min"), Integer.class, Config.Teams.minimumPlayers());
    int maxPlayers = XMLUtils.parseNumber(Node.fromRequiredAttr(el, "max"), Integer.class);

    Attribute attrMaxOverfill = el.getAttribute("max-overfill");
    int maxOverfill =
        XMLUtils.parseNumber(
            attrMaxOverfill, Integer.class, (int) (maxPlayers * Config.Join.overfillRatio()));
    if (maxOverfill < maxPlayers) {
      throw new InvalidXMLException("Max overfill can not be less then max players.", el);
    }

    TeamFactory teamFactory =
        new TeamFactory(
            id,
            name,
            plural,
            color,
            overheadColor,
            minPlayers,
            maxPlayers,
            maxOverfill,
            nameTagVisibility);
    context.features().addFeature(el, teamFactory);

    return teamFactory;
  }
}
