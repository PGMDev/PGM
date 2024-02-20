package tc.oc.pgm.projectile;

import static tc.oc.pgm.util.text.TemporalComponent.ticker;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.tag.ItemTags;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.inventory.InventoryUtils;
import tc.oc.pgm.util.text.TextTranslations;

public class ProjectileCooldowns {

  private final ProjectileMatchModule pmm;
  private final MatchPlayer player;
  private final HashMap<ProjectileDefinition, Instant> cooldowns = new HashMap<>();

  private final BukkitRunnable runnable = new CooldownRunnable();
  private Future<?> runnableTask = null;

  public ProjectileCooldowns(ProjectileMatchModule projectileMatchModule, MatchPlayer player) {
    this.pmm = projectileMatchModule;
    this.player = player;
  }

  public boolean isActive(ProjectileDefinition definition) {
    return cooldowns.containsKey(definition);
  }

  private String getTimeLeftString(long timeLeft) {
    return TextTranslations.translateLegacy(ticker(timeLeft), player.getBukkit());
  }

  private Duration getTimeLeft(ProjectileDefinition projectile) {
    Instant endTime = cooldowns.get(projectile);

    if (endTime == null || endTime.isBefore(Instant.now())) {
      return Duration.ZERO;
    } else {
      return Duration.between(Instant.now(), endTime);
    }
  }

  public void start(ProjectileDefinition definition) {
    if (this.isActive(definition))
      throw new RuntimeException("cooldown started when already in progress");

    cooldowns.put(definition, Instant.now().plus(definition.coolDown));

    if (runnableTask != null && !this.runnableTask.isDone()) return;

    this.runnableTask =
        player
            .getMatch()
            .getExecutor(MatchScope.RUNNING)
            .scheduleAtFixedRate(this.runnable, 0, TimeUtils.TICK, TimeUnit.MILLISECONDS);
  }

  private void end() {
    this.runnableTask.cancel(true);
  }

  private void resetItemName(ItemStack item) {
    ItemMeta itemMeta = item.getItemMeta();
    itemMeta.setDisplayName(ItemTags.ORIGINAL_NAME.get(item));
    item.setItemMeta(itemMeta);
  }

  public void setItemCountdownName(ItemStack item, ProjectileDefinition definition) {
    setItemCountdownName(item, getTimeLeft(definition).getSeconds());
  }

  private void setItemCountdownName(ItemStack item, long timeLeft) {
    ItemMeta itemMeta = item.getItemMeta();
    itemMeta.setDisplayName(
        ItemTags.ORIGINAL_NAME.get(item) + " | " + this.getTimeLeftString(timeLeft));
    item.setItemMeta(itemMeta);
  }

  private class CooldownRunnable extends BukkitRunnable {

    long lastUpdateSeconds = 0;

    @Override
    public void run() {
      // Remove any cooldowns that have expired keeping note of previous size
      Instant now = Instant.now();
      PlayerInventory inventory = Objects.requireNonNull(player.getInventory());

      int initialSize = cooldowns.size();
      cooldowns.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));

      if (initialSize == cooldowns.size()) {
        // Tick the held item if cooldowns active
        setItemCooldown(inventory.getItemInHand(), now);
      } else {
        inventory.forEach(this::resetItemCooldownName);
      }

      if (cooldowns.isEmpty()) {
        lastUpdateSeconds = -1;
        end();
      }
    }

    private void resetItemCooldownName(ItemStack item) {
      if (InventoryUtils.isNothing(item)) return;

      String projectileId = ItemTags.PROJECTILE.get(item);
      if (projectileId == null) return;

      // When no cooldown exists reset item name
      if (!cooldowns.containsKey(pmm.getProjectileDefinition(projectileId))) {
        resetItemName(item);
      }
    }

    private void setItemCooldown(ItemStack item, Instant now) {
      if (InventoryUtils.isNothing(item)) return;

      ProjectileDefinition definition = pmm.getProjectileDefinition(item);
      if (definition == null) return;

      Instant expiresAt = cooldowns.get(definition);
      if (expiresAt == null) return;

      // Add 500ms to display value so value rounds rather than flooring
      Duration timeLeft = Duration.between(now, expiresAt);
      long displaySeconds = (timeLeft.toMillis() + 500) / 1000;

      // Only update countdown text every 1 second
      // Do not update for cooldowns under 200ms (to prevent rapid name changes)
      if (lastUpdateSeconds != displaySeconds && timeLeft.toMillis() >= 200) {
        setItemCountdownName(item, displaySeconds);
        lastUpdateSeconds = displaySeconds;
      }
    }
  }
}
