package tc.oc.pgm.projectile;

import static tc.oc.pgm.util.text.TemporalComponent.ticker;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.kits.tag.ItemTags;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.text.TextTranslations;

public class ProjectileCooldown {
  private final MatchPlayer matchPlayer;
  private final MatchPlayerState matchPlayerState;
  private final ProjectileDefinition projectileDefinition;
  private Instant endTime = null;
  private final BukkitRunnable runnable = new CooldownRunnable(this);
  private Future<?> runnableTask = null;

  public ProjectileCooldown(MatchPlayer matchPlayer, ProjectileDefinition projectileDefinition) {
    this.matchPlayer = matchPlayer;
    this.matchPlayerState = matchPlayer.getState();
    this.projectileDefinition = projectileDefinition;
  }

  public Duration getTimeLeft() {
    if (endTime == null || endTime.isBefore(Instant.now())) {
      return Duration.ZERO;
    } else {
      return Duration.between(Instant.now(), endTime);
    }
  }

  public String getTimeLeftString() {
    return TextTranslations.translateLegacy(ticker(getTimeLeft()), matchPlayer.getBukkit());
  }

  public boolean isActive() {
    return this.runnableTask != null && !runnableTask.isDone();
  }

  public void start() {
    if (this.isActive()) throw new RuntimeException("cooldown started when already in progress");

    this.endTime = Instant.now().plus(projectileDefinition.coolDown);
    this.runnableTask =
        matchPlayer
            .getMatch()
            .getExecutor(MatchScope.RUNNING)
            .scheduleAtFixedRate(this.runnable, 0, TimeUtils.TICK, TimeUnit.MILLISECONDS);
  }

  public void end() {
    this.runnableTask.cancel(true);
  }

  public ProjectileDefinition getProjectileDefinition() {
    return projectileDefinition;
  }

  public MatchPlayer getMatchPlayer() {
    return matchPlayer;
  }

  public MatchPlayerState getMatchPlayerState() {
    return matchPlayerState;
  }

  public void resetItemName(ItemStack item) {
    ItemMeta itemMeta = item.getItemMeta();
    itemMeta.setDisplayName(ItemTags.ORIGINAL_NAME.get(item));
    item.setItemMeta(itemMeta);
  }

  public void setItemCoutdownName(ItemStack item) {
    ItemMeta itemMeta = item.getItemMeta();
    itemMeta.setDisplayName(ItemTags.ORIGINAL_NAME.get(item) + " | " + this.getTimeLeftString());
    item.setItemMeta(itemMeta);
  }
}
