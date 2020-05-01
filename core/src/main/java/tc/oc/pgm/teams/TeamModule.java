package tc.oc.pgm.teams;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.NameTagVisibility;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class TeamModule implements MapModule<TeamMatchModule> {
  private static final double OVERFILL_RATIO = 1.25;
  private static final Map<Integer, Collection<MapTag>> TAGS = new ConcurrentHashMap<>();

  private final Set<TeamFactory> teams;
  private final @Nullable Boolean requireEven;

  public TeamModule(Set<TeamFactory> teams, @Nullable Boolean requireEven) {
    this.teams = teams;
    this.requireEven = requireEven;
  }

  @Override
  public Collection<MapTag> getTags() {
    final int id = teams.size();
    Collection<MapTag> tags = TAGS.get(id);
    if (tags == null) {
      tags =
          ImmutableList.of(
              MapTag.create(
                  id + "team" + (id == 1 ? "" : "s"),
                  id + " Team" + (id == 1 ? "" : "s"),
                  false,
                  true));
      TAGS.put(id, tags);
    }
    return tags;
  }

  @Override
  public Collection<Class<? extends MatchModule>> getHardDependencies() {
    return ImmutableList.of(JoinMatchModule.class, StartMatchModule.class);
  }

  public static class Factory implements MapModuleFactory<TeamModule> {
    @Override
    public TeamModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Set<TeamFactory> teamFactories = Sets.newLinkedHashSet();
      Boolean requireEven = null;

      for (Element teamRootElement : doc.getRootElement().getChildren("teams")) {
        requireEven = XMLUtils.parseBoolean(teamRootElement.getAttribute("even"), requireEven);

        for (Element teamElement : teamRootElement.getChildren("team")) {
          teamFactories.add(parseTeamDefinition(teamElement, factory));
        }
      }

      return teamFactories.isEmpty() ? null : new TeamModule(teamFactories, requireEven);
    }
  }

  @Override
  public TeamMatchModule createMatchModule(Match match) {
    return new TeamMatchModule(match, teams);
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

  public TeamFactory parseTeam(Attribute attr, MapFactory factory) throws InvalidXMLException {
    if (attr == null) {
      return null;
    }
    String name = attr.getValue();
    TeamFactory team = Teams.getTeam(name, factory);

    if (team == null) {
      throw new InvalidXMLException("unknown team '" + name + "'", attr);
    }
    return team;
  }

  private static TeamFactory parseTeamDefinition(Element el, MapFactory factory)
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

    int minPlayers = XMLUtils.parseNumber(Node.fromAttr(el, "min"), Integer.class, 0);
    int maxPlayers = XMLUtils.parseNumber(Node.fromRequiredAttr(el, "max"), Integer.class);

    Attribute attrMaxOverfill = el.getAttribute("max-overfill");
    int maxOverfill =
        XMLUtils.parseNumber(attrMaxOverfill, Integer.class, (int) (maxPlayers * OVERFILL_RATIO));
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
    factory.getFeatures().addFeature(el, teamFactory);

    return teamFactory;
  }
}
