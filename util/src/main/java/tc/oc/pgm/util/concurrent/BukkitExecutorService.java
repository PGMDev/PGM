package tc.oc.pgm.util.concurrent;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public class BukkitExecutorService extends TaskExecutorService {

  private final Plugin plugin;
  private final BukkitScheduler scheduler;
  private final boolean async;

  public BukkitExecutorService(Plugin plugin, boolean async) {
    this.plugin = plugin;
    this.scheduler = plugin.getServer().getScheduler();
    this.async = async;
  }

  @Override
  protected int runTask(Runnable task, long delayInTicks) {
    BukkitTask bukkitTask =
        async
            ? scheduler.runTaskLaterAsynchronously(plugin, task, delayInTicks)
            : scheduler.runTaskLater(plugin, task, delayInTicks);
    return bukkitTask.getTaskId();
  }

  @Override
  protected int runPeriodicTask(
      Runnable task, long initialDelayInTicks, long periodicDelayInTicks) {
    BukkitTask bukkitTask =
        async
            ? scheduler.runTaskTimerAsynchronously(
                plugin, task, initialDelayInTicks, periodicDelayInTicks)
            : scheduler.runTaskTimer(plugin, task, initialDelayInTicks, periodicDelayInTicks);
    return bukkitTask.getTaskId();
  }

  @Override
  protected void cancelTask(int id) {
    scheduler.cancelTask(id);
  }
}
