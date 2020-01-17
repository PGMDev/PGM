package tc.oc.pgm.teams;

import static tc.oc.pgm.api.map.MapProtos.FILTER_FEATURES;

import javax.annotation.Nullable;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.features.FeatureReference;
import tc.oc.pgm.features.ImmediateFeatureReference;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

public class Teams {

  private Teams() {}

  /**
   * Lookup a team by name or ID. Prior to {@link MapProtos#FILTER_FEATURES}, teams are looked
   * up by name.
   */
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

  public static TeamFactory getTeam(String team, Match match) {
    return match.needModule(TeamMatchModule.class).bestFuzzyMatch(team).getInfo();
  }

  public static FeatureReference<TeamFactory> getTeamRef(Node node, MapFactory factory)
      throws InvalidXMLException {
    String id = node.getValueNormalize();
    if (factory.getProto().isOlderThan(FILTER_FEATURES)) {
      TeamFactory definition = getTeam(id, factory);
      if (definition == null) {
        throw new InvalidXMLException("Unknown team '" + id + "'", node);
      }
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
