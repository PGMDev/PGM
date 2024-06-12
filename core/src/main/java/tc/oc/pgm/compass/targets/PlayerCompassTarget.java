package tc.oc.pgm.compass.targets;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
  private final boolean hasCustomName;

  public PlayerCompassTarget(Filter filter, Component name, boolean showPlayerName) {
    this.filter = filter;
    this.hasCustomName = name != null;
    this.name = this.hasCustomName ? name : Component.translatable("compass.tracking.player");
    this.showPlayerName = showPlayerName;
  }

  @Override
  public Filter getFilter() {
    return filter;
  }

  @Override
  public Component getName(Match match, MatchPlayer player) {
    if (showPlayerName) {
      if (hasCustomName) {
        return name.append(text(": ", NamedTextColor.WHITE))
            .append(player(player, NameStyle.FANCY));
      } else {
        return player(player, NameStyle.FANCY);
      }
    } else {
      return name;
    }
  }

  @Override
  public Optional<Location> getLocation(Match match, MatchPlayer player) {
    return getClosestPlayer(match, player).map(MatchPlayer::getLocation);
  }

  @Override
  public Optional<CompassTargetResult> getResult(Match match, MatchPlayer player) {
    Optional<MatchPlayer> playerOptional = getClosestPlayer(match, player);
    if (playerOptional.isPresent()) {
      MatchPlayer targetPlayer = playerOptional.get();
      Location targetLoc = targetPlayer.getLocation();
      Location playerLoc = player.getLocation();
      double yDifference = targetLoc.getY() - playerLoc.getY();

      return Optional.of(
          new CompassTargetResult(
              targetLoc, targetLoc.distance(playerLoc), yDifference, getName(match, targetPlayer)));
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
