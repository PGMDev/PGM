package tc.oc.pgm.consumable;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;

public class ConsumableDefinition extends SelfIdentifyingFeatureDefinition {

  private final Action<? super MatchPlayer> action;
  private final ConsumeCause cause;
  /** If true, replaces vanilla behaviour, otherwise keeps vanilla behaviour */
  private final boolean preventDefault;

  public ConsumableDefinition(
      @Nullable String id,
      Action<? super MatchPlayer> action,
      ConsumeCause cause,
      boolean preventDefault) {
    super(id);
    this.action = action;
    this.cause = cause;
    this.preventDefault = preventDefault;
  }

  public Action<? super MatchPlayer> getAction() {
    return action;
  }

  public ConsumeCause getCause() {
    return cause;
  }

  public boolean getPreventDefault() {
    return preventDefault;
  }
}
