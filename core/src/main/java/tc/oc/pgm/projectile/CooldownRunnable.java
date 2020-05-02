package tc.oc.pgm.projectile;

import java.time.Duration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.kits.tag.ItemTags;
import tc.oc.pgm.util.inventory.InventoryUtils;

public class CooldownRunnable extends BukkitRunnable {
  private final ProjectileCooldown projectileCooldown;

  public CooldownRunnable(ProjectileCooldown projectileCooldown) {
    this.projectileCooldown = projectileCooldown;
  }

  int lastUpdateSeconds = 0;

  @Override
  public void run() {
    MatchPlayer matchPlayer = projectileCooldown.getMatchPlayer();
    MatchPlayerState matchPlayerState = projectileCooldown.getMatchPlayerState();
    if (matchPlayer.isDead() || matchPlayer.getParty() != matchPlayerState.getParty())
      projectileCooldown.end();

    Duration timeLeft = projectileCooldown.getTimeLeft();

    for (ItemStack item : matchPlayer.getInventory()) {
      if (InventoryUtils.isNothing(item)) continue;
      String projectileId = ItemTags.PROJECTILE.get(item);
      if (projectileId != null
          && projectileId.equals(projectileCooldown.getProjectileDefinition().getId())) {
        if (timeLeft.isZero()) {
          projectileCooldown.resetItemName(item);
        } else {
          // Only update countdown text every 1 second until the last second in which it updates
          // every tick.
          // Not updating the last tenth ms to prevent the countdown text being 0.0.
          if (timeLeft.getSeconds() != lastUpdateSeconds
              || (timeLeft.toMillis() < 1000 && timeLeft.toMillis() >= 100)) {
            projectileCooldown.setItemCoutdownName(item);
          }
        }
      }
    }

    if (timeLeft.isZero()) projectileCooldown.end();
    lastUpdateSeconds = (int) timeLeft.getSeconds();
  }
}
