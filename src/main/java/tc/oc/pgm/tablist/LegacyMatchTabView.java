package tc.oc.pgm.tablist;

import org.bukkit.entity.Player;
import tc.oc.util.bukkit.tablist.LegacyTabView;
import tc.oc.util.collection.DefaultProvider;

/**
 * The pre-1.8 tab list is severely limited. This is essentially a no-op {@link MatchTabView} for
 * pre-1.8 clients.
 */
public class LegacyMatchTabView extends LegacyTabView {

  public static class Factory implements DefaultProvider<Player, LegacyMatchTabView> {
    @Override
    public LegacyMatchTabView get(Player key) {
      return new LegacyMatchTabView(key);
    }
  }

  public LegacyMatchTabView(Player viewer) {
    super(viewer);
  }

  @Override
  public void disable() {
    super.disable();
  }
}
