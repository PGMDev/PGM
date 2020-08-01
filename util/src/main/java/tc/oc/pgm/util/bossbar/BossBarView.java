package tc.oc.pgm.util.bossbar;

import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInitialSpawnEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.bukkit.ViaUtils;
import tc.oc.pgm.util.text.TextTranslations;

public abstract class BossBarView implements BossBarObserver {

  public static BossBarView of(Plugin plugin, Player viewer, int entityId) {
    if (ViaUtils.enabled() && ViaUtils.getProtocolVersion(viewer) >= ViaUtils.VERSION_1_9)
      return new BossBarViaView(plugin, viewer, ViaUtils.createBossBar());
    return new BossBarWitherView(plugin, viewer, entityId);
  }

  public static final int MAX_TEXT_LENGTH = 64;

  protected final Plugin plugin;
  protected final Player viewer;

  protected BossBarSource bar = BlankBossBar.INSTANCE;

  public BossBarView(Plugin plugin, Player viewer) {
    this.plugin = plugin;
    this.viewer = viewer;
  }

  public void setBar(@Nullable BossBarSource bar) {
    if (bar == null) bar = BlankBossBar.INSTANCE;
    if (this.bar != bar) {
      this.bar.removeObserver(this);
      this.bar = bar;
      this.bar.addObserver(this);
      invalidate(bar);
    }
  }

  @Override
  public void invalidate(BossBarSource bar) {
    if (bar == this.bar) {
      render();
    }
  }

  protected String renderText() {
    return StringUtils.truncate(
        TextTranslations.translateLegacy(bar.getText(viewer), viewer), MAX_TEXT_LENGTH);
  }

  protected abstract void render();

  public void onPlayerMove(PlayerMoveEvent event) {}

  public void onPlayerRespawn(PlayerInitialSpawnEvent event) {}

  public void onPlayerRespawn(PlayerRespawnEvent event) {}
}
