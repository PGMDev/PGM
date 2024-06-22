package tc.oc.pgm.compass.targets;

import java.util.Optional;
import net.kyori.adventure.text.Component;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.compass.CompassTarget;
import tc.oc.pgm.compass.CompassTargetResult;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.FlagDefinition;

public class FlagCompassTarget extends CompassTarget<Flag> {

  private final FlagDefinition flag;
  private final Component name;

  public FlagCompassTarget(Filter holderFilter, FlagDefinition flag, Component name) {
    super(holderFilter);
    this.flag = flag;
    this.name = name;
  }

  @Override
  protected Optional<Flag> getMatching(MatchPlayer player) {
    return Optional.of(flag.getGoal(player.getMatch()));
  }

  @Override
  protected Optional<CompassTargetResult> buildResult(Flag flag, MatchPlayer player) {
    if (flag.isCarrying(player)) return Optional.empty();
    return flag.getLocation()
        .map(loc -> CompassTargetResult.of(
            loc, player.getLocation(), name != null ? name : flag.getComponentName()));
  }
}
