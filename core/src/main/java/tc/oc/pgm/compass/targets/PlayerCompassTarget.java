package tc.oc.pgm.compass.targets;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import java.util.Comparator;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.compass.CompassTarget;
import tc.oc.pgm.compass.CompassTargetResult;
import tc.oc.pgm.filters.query.PlayerQuery;
import tc.oc.pgm.util.named.NameStyle;

public class PlayerCompassTarget extends CompassTarget<MatchPlayer> {

  private final Filter targetFilter;
  private final Component name;
  private final boolean showPlayerName;
  private final boolean hasCustomName;

  public PlayerCompassTarget(
      Filter holderFilter, Filter targetFilter, Component name, boolean showPlayerName) {
    super(holderFilter);
    this.targetFilter = targetFilter;
    this.hasCustomName = name != null;
    this.name = this.hasCustomName ? name : Component.translatable("compass.tracking.player");
    this.showPlayerName = showPlayerName;
  }

  @Override
  protected Optional<MatchPlayer> getMatching(MatchPlayer player) {
    return player.getMatch().getParticipants().stream()
        .filter(target -> !target.equals(player))
        .filter(target -> targetFilter.query(new PlayerQuery(null, target)).isAllowed())
        .min(Comparator.comparingDouble(
            target -> target.getLocation().distance(player.getLocation())));
  }

  protected Optional<CompassTargetResult> buildResult(MatchPlayer target, MatchPlayer holder) {
    return Optional.of(
        CompassTargetResult.of(target.getLocation(), holder.getLocation(), getName(target)));
  }

  private Component getName(MatchPlayer player) {
    if (!showPlayerName) return name;
    if (!hasCustomName) return player(player, NameStyle.FANCY);
    return name.append(text(": ", NamedTextColor.WHITE)).append(player(player, NameStyle.FANCY));
  }
}
