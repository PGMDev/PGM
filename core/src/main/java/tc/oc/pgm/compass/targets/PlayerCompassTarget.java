package tc.oc.pgm.compass.targets;

import static tc.oc.pgm.util.player.PlayerComponent.player;

import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.compass.CompassTarget;
import tc.oc.pgm.compass.CompassTargetResult;
import tc.oc.pgm.filters.query.PlayerQuery;
import tc.oc.pgm.util.named.NameStyle;

public class PlayerCompassTarget implements CompassTarget {

  private final Filter filter;
  private final Component name;
  private final boolean showPlayerName;

  public PlayerCompassTarget(Filter filter, Component name, boolean showPlayerName) {
    this.filter = filter;
    this.name = name == null ? Component.translatable("compass.tracking.player") : name;
    this.showPlayerName = showPlayerName;
  }

  @Override
  public Filter getFilter() {
    return filter;
  }

  @Override
  public Component getName(Match match, MatchPlayer player) {
    return showPlayerName ? player(player, NameStyle.FANCY) : name;
  }

  @Override
  public Optional<Location> getLocation(Match match, MatchPlayer player) {
    return getClosestPlayer(match, player).map(MatchPlayer::getLocation);
  }

  @Override
  public Optional<CompassTargetResult> getResult(Match match, MatchPlayer player) {
    Optional<MatchPlayer> playerOptional = getClosestPlayer(match, player);
    if (playerOptional.isPresent()) {
      MatchPlayer matchPlayer = playerOptional.get();
      return Optional.of(
          new CompassTargetResult(
              matchPlayer.getLocation(),
              matchPlayer.getLocation().distance(player.getLocation()),
              getName(match, matchPlayer)));
    } else {
      return Optional.empty();
    }
  }

  private Optional<MatchPlayer> getClosestPlayer(Match match, MatchPlayer player) {
    return match.getParticipants().stream()
        .filter((otherPlayer) -> !otherPlayer.equals(player))
        .filter((otherPlayer) -> filter.response(new PlayerQuery(null, otherPlayer)))
        .min(
            (firstPlayer, secondPlayer) ->
                (int)
                    (firstPlayer.getLocation().distance(player.getLocation())
                        - secondPlayer.getLocation().distance(player.getLocation())));
  }
}
