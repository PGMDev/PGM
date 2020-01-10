package tc.oc.pgm.teams;

import static tc.oc.pgm.api.map.ProtoVersions.FILTER_FEATURES;

import javax.annotation.Nullable;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.ProtoVersions;
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
   * Lookup a team by name or ID. Prior to {@link ProtoVersions#FILTER_FEATURES}, teams are looked
   * up by name.
   */
  public static TeamFactory getTeam(String team, MapContext context) {
    TeamFactory teamFactory = context.legacy().getFeatures().get(team, TeamFactory.class);
    if (context.getInfo().getProto().isOlderThan(FILTER_FEATURES)) {
      TeamModule teams = context.getModule(TeamModule.class);
      if (teams != null) {
        TeamFactory teamByName = teams.getTeamByName(team);
        if (teamByName != null) teamFactory = teamByName;
      }
    }

    return teamFactory;
  }

  public static FeatureReference<TeamFactory> getTeamRef(Node node, MapContext context)
      throws InvalidXMLException {
    String id = node.getValueNormalize();
    if (context.getInfo().getProto().isOlderThan(FILTER_FEATURES)) {
      TeamFactory definition = getTeam(id, context);
      if (definition == null) {
        throw new InvalidXMLException("Unknown team '" + id + "'", node);
      }
      return new ImmediateFeatureReference<>(definition);
    } else {
      return context.legacy().getFeatures().createReference(node, TeamFactory.class);
    }
  }

  public static Team getTeam(String team, Match match) {
    TeamFactory teamFactory = getTeam(team, match.getMapContext());
    return teamFactory == null
        ? null
        : match.needMatchModule(TeamMatchModule.class).getTeam(teamFactory);
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
