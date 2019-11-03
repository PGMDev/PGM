package tc.oc.pgm.flag.state;

import javax.annotation.Nullable;
import org.bukkit.Location;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.flag.Post;
import tc.oc.pgm.teams.Team;

public interface State {

  boolean isCurrent();

  boolean isCarrying(MatchPlayer player);

  boolean isCarrying(ParticipantState player);

  boolean isCarrying(Party team);

  boolean isAtPost(Post post);

  Post getPost();

  @Nullable
  Team getController();

  Iterable<Location> getProximityLocations(ParticipantState player);
}
