package tc.oc.pgm.compass;

import java.util.Optional;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.player.MatchPlayer;

public abstract class CompassTarget<T> {
  private final Filter holderFilter;

  public CompassTarget(Filter holderFilter) {
    this.holderFilter = holderFilter;
  }

  public final Optional<CompassTargetResult> getResult(MatchPlayer player) {
    if (!holderFilter.query(player).isAllowed()) return Optional.empty();
    return getMatching(player).flatMap(r -> buildResult(r, player));
  }

  protected abstract Optional<T> getMatching(MatchPlayer player);

  protected abstract Optional<CompassTargetResult> buildResult(T type, MatchPlayer player);
}
