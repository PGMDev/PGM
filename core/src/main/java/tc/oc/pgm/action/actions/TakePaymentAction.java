package tc.oc.pgm.action.actions;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.shops.menu.Payable;

public class TakePaymentAction extends AbstractAction<MatchPlayer> {

  private final Payable payable;
  private final @Nullable Action<? super MatchPlayer> onSuccess;
  private final @Nullable Action<? super MatchPlayer> onFailure;

  public TakePaymentAction(
      Payable payable,
      @Nullable Action<? super MatchPlayer> onSuccess,
      @Nullable Action<? super MatchPlayer> onFailure) {
    super(MatchPlayer.class);
    this.payable = payable;
    this.onSuccess = onSuccess;
    this.onFailure = onFailure;
  }

  @Override
  public void trigger(MatchPlayer player) {
    if (payable.takePayment(player)) {
      if (onSuccess != null) onSuccess.trigger(player);
    } else {
      if (onFailure != null) onFailure.trigger(player);
    }
  }
}
