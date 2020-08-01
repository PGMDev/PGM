package tc.oc.pgm.util.bossbar;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import us.myles.ViaVersion.api.boss.BossBar;

public class BossBarViaView extends BossBarView {

  private boolean visible;
  private BossBar<?> bossBar;

  public BossBarViaView(Plugin plugin, Player viewer, BossBar<?> bossBar) {
    super(plugin, viewer);
    this.bossBar = bossBar;
  }

  protected void render() {
    boolean shouldBeVisible = bar.isVisible(viewer);
    if (shouldBeVisible != visible) {
      if (shouldBeVisible) bossBar.addPlayer(viewer.getUniqueId());
      else bossBar.removePlayer(viewer.getUniqueId());
      visible = shouldBeVisible;
    }
    if (visible) {
      bossBar.setTitle(renderText());
      bossBar.setHealth(bar.getMeter(viewer));
    }
  }
}
