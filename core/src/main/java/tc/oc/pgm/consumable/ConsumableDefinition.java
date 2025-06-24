package tc.oc.pgm.consumable;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;

public class ConsumableDefinition extends SelfIdentifyingFeatureDefinition {

  private final Action<? super MatchPlayer> action;
  private final ConsumeCause cause;
  /** If true, replaces vanilla behaviour, otherwise keeps vanilla behaviour */
  private final boolean override;
  /** If the item should be consumed after use */
  private final boolean consume;

  public ConsumableDefinition(
      @Nullable String id,
      Action<? super MatchPlayer> action,
      ConsumeCause cause,
      boolean override,
      boolean consume) {
    super(id);
    this.action = action;
    this.cause = cause;
    this.override = override;
    this.consume = consume;
  }

  public Action<? super MatchPlayer> getAction() {
    return action;
  }

  public ConsumeCause getCause() {
    return cause;
  }

  public boolean getOverride() {
    return override;
  }

  public boolean getConsume() {
    return consume;
  }
}
