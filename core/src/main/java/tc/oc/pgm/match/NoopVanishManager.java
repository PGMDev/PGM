package tc.oc.pgm.match;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.UUID;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.VanishManager;

public class NoopVanishManager implements VanishManager {

  @Override
  public boolean isVanished(UUID uuid) {
    return false;
  }

  @Override
  public Collection<MatchPlayer> getOnlineVanished() {
    return Lists.newArrayList();
  }

  @Override
  public boolean setVanished(MatchPlayer player, boolean vanish, boolean quiet) {
    return false;
  }

  @Override
  public void disable() {}
}
