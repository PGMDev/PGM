package tc.oc.pgm.projectile;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.joda.time.Duration;
import org.joda.time.Instant;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.kits.tag.ItemTags;
import tc.oc.util.bukkit.component.PeriodFormats;

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
    if (endTime == null || endTime.isBeforeNow()) {
      return Duration.ZERO;
    } else {
      return new Duration(Instant.now(), endTime);
    }
  }

  public String getTimeLeftString() {
    if (this.getTimeLeft().getMillis() < 1000) {
      // display only tenths of seconds
      return this.getTimeLeft()
          .toPeriod()
          .toString(PeriodFormats.COUNTDOWN)
          .substring(0, 3)
          .concat(" sec");
    } else {
      return this.getTimeLeft().toPeriod().withMillis(0).toString(PeriodFormats.COUNTDOWN);
    }
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
