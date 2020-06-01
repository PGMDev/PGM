package tc.oc.pgm.util.bossbar;

import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInitialSpawnEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.nms.NMSHacks;
import tc.oc.pgm.util.text.TextTranslations;

public class BossBarView implements BossBarObserver {

  public static final float BOSS_18_HEALTH = 300;
  public static final double BOSS_18_DISTANCE = 50;
  public static final float BOSS_18_ANGLE = 20;
  public static final int MAX_TEXT_LENGTH = 64;

  private final Plugin plugin;
  private final Player viewer;
  private final int entityId;

  private BossBar bar = BlankBossBar.INSTANCE;
  private Location location;
  private boolean spawned;

  public BossBarView(Plugin plugin, Player viewer, int entityId) {
    this.plugin = plugin;
    this.viewer = viewer;
    this.entityId = entityId;
  }

  public void setBar(@Nullable BossBar bar) {
    if (bar == null) bar = BlankBossBar.INSTANCE;
    if (this.bar != bar) {
      this.bar.removeObserver(this);
      this.bar = bar;
      this.bar.addObserver(this);
      invalidate(bar);
    }
  }

  @Override
  public void invalidate(BossBar bar) {
    if (bar == this.bar) {
      render();
    }
  }

  private void render() {
    if (bar.isVisible(viewer)) {
      if (spawned) {
        NMSHacks.updateBoss(viewer, entityId, renderText(), renderMeter());
      } else {
        spawnBoss();
      }
    } else if (spawned) {
      NMSHacks.destroyEntities(viewer, entityId);
      spawned = false;
    }
  }

  private float renderMeter() {
    // Keep the boss barely alive, to avoid the death animation
    return Math.max(0F, Math.min(1F, bar.getMeter(viewer))) * (BOSS_18_HEALTH - 0.1F) + 0.1F;
  }

  private String renderText() {
    return StringUtils.truncate(
        TextTranslations.translateLegacy(bar.getText(viewer), viewer), MAX_TEXT_LENGTH);
  }

  private void spawnBoss() {
    resetBossLocation(viewer.getLocation());
    NMSHacks.spawnWither(viewer, entityId, location, renderText(), renderMeter());
    spawned = true;
  }

  private void resetBossLocation(Location pos) {
    location = pos;
    // Keep the boss a few degrees up from the center of the player's view
    location.setPitch(location.getPitch() - BOSS_18_ANGLE);
    location.add(location.getDirection().multiply(BOSS_18_DISTANCE));
  }

  private void handleSpawn() {
    if (spawned) {
      viewer
          .getServer()
          .getScheduler()
          .runTask(
              plugin,
              new Runnable() {
                @Override
                public void run() {
                  if (viewer.isOnline() && spawned) {
                    spawnBoss();
                  }
                }
              });
    }
  }

  // Dispatched from elsewhere
  public void onPlayerMove(PlayerMoveEvent event) {
    if (viewer == event.getPlayer() && spawned) {
      resetBossLocation(event.getTo().clone());
      NMSHacks.teleportEntity(viewer, entityId, location);
    }
  }

  // Dispatched from elsewhere
  public void onPlayerRespawn(PlayerInitialSpawnEvent event) {
    // Entities are destroyed when a player respawns, so we resend it.
    if (viewer == event.getPlayer()) handleSpawn();
  }

  // Dispatched from elsewhere
  public void onPlayerRespawn(PlayerRespawnEvent event) {
    // Entities are destroyed when a player respawns, so we resend it.
    if (viewer == event.getPlayer()) handleSpawn();
  }
}
