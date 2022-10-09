package tc.oc.pgm.teams;

import static tc.oc.pgm.api.map.MapProtos.FILTER_FEATURES;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.features.ImmediateFeatureReference;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public class Teams {

  private Teams() {}

  public static TeamFactory getTeam(String team, MapFactory factory) {
    TeamFactory teamFactory = factory.getFeatures().get(team, TeamFactory.class);
    if (factory.getProto().isOlderThan(FILTER_FEATURES)) {
      TeamModule teams = factory.getModule(TeamModule.class);
      if (teams != null) {
        TeamFactory teamByName = teams.getTeamByName(team);
        if (teamByName != null) teamFactory = teamByName;
      }
    }

    return teamFactory;
  }

  /**
   * Lookup a team by name or ID. Prior to {@link MapProtos#FILTER_FEATURES}, teams are looked up by
   * name.
   */
  public static TeamFactory getTeam(Node node, MapFactory factory) throws InvalidXMLException {
    TeamFactory teamFactory = getTeam(node.getValueNormalize(), factory);

    if (teamFactory == null) {
      throw new InvalidXMLException("unknown team '" + node.getValue() + "'", node);
    }

    return teamFactory;
  }

  public static TeamFactory getTeam(String name, Match match) {
    final Team team = match.needModule(TeamMatchModule.class).bestFuzzyMatch(name);
    return team == null ? null : team.getInfo();
  }

  public static FeatureReference<TeamFactory> getTeamRef(Node node, MapFactory factory)
      throws InvalidXMLException {
    if (factory.getProto().isOlderThan(FILTER_FEATURES)) {
      TeamFactory definition = getTeam(node, factory);
      return new ImmediateFeatureReference<>(definition);
    } else {
      return factory.getFeatures().createReference(node, TeamFactory.class);
    }
  }

  public static @Nullable Team get(Party party) {
    return party instanceof Team ? (Team) party : null;
  }

  public static @Nullable Team get(MatchPlayer player) {
    return get(player.getParty());
  }

  public static @Nullable TeamFactory getDefinition(Party party) {
    return party instanceof Team ? ((Team) party).getInfo() : null;
  }
}
