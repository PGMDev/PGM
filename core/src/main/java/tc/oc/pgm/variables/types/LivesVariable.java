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
    return player.moduleRequire(BlitzMatchModule.class).getNumOfLives(player.getId());
  }

  @Override
  protected void setValueImpl(MatchPlayer player, double value) {
    player.moduleRequire(BlitzMatchModule.class).setLives(player, Math.max((int) value, 0));
  }
}
