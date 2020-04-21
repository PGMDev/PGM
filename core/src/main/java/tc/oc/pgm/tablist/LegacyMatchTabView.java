package tc.oc.pgm.tablist;

import org.bukkit.entity.Player;
import tc.oc.util.bukkit.tablist.LegacyTabView;

/**
 * The pre-1.8 tab list is severely limited. This is essentially a no-op {@link MatchTabView} for
 * pre-1.8 clients.
 */
public class LegacyMatchTabView extends LegacyTabView {
  public LegacyMatchTabView(Player viewer) {
    super(viewer);
  }
}
