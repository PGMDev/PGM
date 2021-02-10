package tc.oc.pgm.vanish;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.UUID;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.VanishManager;

public class VanishManagerImpl implements VanishManager {

  private VanishManager manager;

  @Override
  public boolean isVanished(UUID uuid) {
    return manager != null ? manager.isVanished(uuid) : false;
  }

  @Override
  public Collection<MatchPlayer> getOnlineVanished() {
    return manager != null ? manager.getOnlineVanished() : Lists.newArrayList();
  }

  @Override
  public boolean setVanished(MatchPlayer player, boolean vanish, boolean quiet) {
    return manager != null ? manager.setVanished(player, vanish, quiet) : false;
  }

  @Override
  public void disable() {}

  @Override
  public VanishManager getManager() {
    return manager;
  }

  @Override
  public void setManager(VanishManager manager) {
    this.manager = manager;
  }
}
