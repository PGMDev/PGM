package tc.oc.pgm.projectile;

import java.time.Duration;
import java.time.Instant;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.kits.tag.ItemTags;
import tc.oc.util.TimeUtils;

public class ProjectileCooldown {
  private final MatchPlayer matchPlayer;
  private final MatchPlayerState matchPlayerState;
  private final ProjectileDefinition projectileDefinition;
  private Instant endTime = null;
  private final BukkitRunnable runnable = new CooldownRunnable(this);
  private BukkitTask runnableTask = null;

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
    return TimeUtils.formatDurationShort(getTimeLeft());
  }

  public boolean isActive() {
    return this.runnableTask != null
        && this.matchPlayer
            .getMatch()
            .getScheduler(MatchScope.RUNNING)
            .isPending(this.runnableTask);
  }

  public void start() {
    if (this.isActive()) throw new RuntimeException("cooldown started when already in progress");

    this.endTime = Instant.now().plus(projectileDefinition.coolDown);
    this.runnableTask =
        this.matchPlayer
            .getMatch()
            .getScheduler(MatchScope.RUNNING)
            .runTaskTimer(0l, 1l, this.runnable);
  }

  public void end() {
    this.runnableTask.cancel();
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
