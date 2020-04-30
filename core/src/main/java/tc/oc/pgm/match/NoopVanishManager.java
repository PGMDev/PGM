package tc.oc.pgm.match;

import app.ashcon.intake.CommandException;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.UUID;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.VanishManager;

public class NoopVanishManager implements VanishManager {

  @Override
  public boolean isVanished(UUID uuid) {
    return false;
  }

  @Override
  public List<MatchPlayer> getOnlineVanished() {
    return Lists.newArrayList();
  }

  @Override
  public void setVanished(MatchPlayer player, boolean vanish, boolean quiet)
      throws CommandException {}

  @Override
  public void disable() {}
}
