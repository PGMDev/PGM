package tc.oc.pgm.variables.types;

import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.blitz.BlitzMatchModule;

public class LivesVariable extends AbstractVariable<MatchPlayer> {
  public static final LivesVariable INSTANCE = new LivesVariable();

  public LivesVariable() {
    super(MatchPlayer.class);
  }

  @Override
  protected double getValueImpl(MatchPlayer player) {
    return player
        .moduleOptional(BlitzMatchModule.class)
        .map(bmm -> bmm.getNumOfLives(player.getId()))
        .orElse(-1);
  }

  @Override
  protected void setValueImpl(MatchPlayer player, double value) {
    int amt = Math.max((int) value, 0);
    player.moduleOptional(BlitzMatchModule.class).ifPresent(bmm -> bmm.setLives(player, amt));
  }
}
