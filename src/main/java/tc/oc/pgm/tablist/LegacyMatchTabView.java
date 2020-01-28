package tc.oc.pgm.tablist;

import org.bukkit.entity.Player;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.tablist.LegacyTabView;
import tc.oc.util.collection.DefaultProvider;

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

  public void onViewerJoinMatch(PlayerJoinMatchEvent event) {}
}
