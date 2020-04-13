package tc.oc.util.bukkit;

import java.time.Duration;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import tc.oc.util.TimeUtils;

public class Scheduler {
  private final Plugin plugin;
  private final BukkitScheduler bukkitScheduler;
  private final WeakHashMap<BukkitTask, Object> tasks = new WeakHashMap<>();
  private boolean cancelled;

  public Scheduler(Plugin plugin, BukkitScheduler scheduler) {
    this.plugin = plugin;
    this.bukkitScheduler = scheduler;
  }

  public Scheduler(Plugin plugin) {
    this(plugin, plugin.getServer().getScheduler());
  }

  private BukkitTask register(final BukkitTask task) {
    if (cancelled) {
      // If the scheduler is cancelled, immediately cancel any task created,
      // but still return it for the sake of consistency.
      task.cancel();
    } else {
      this.tasks.put(task, null);
    }
    return task;
  }

  /**
   * Cancel all currently scheduled tasks and permanently disable the scheduler, so that any future
   * tasks will be cancelled immediately.
   */
  public void cancel() {
    for (BukkitTask task : this.tasks.keySet()) {
      task.cancel();
    }
    this.tasks.clear();
    this.cancelled = true;
  }

  public boolean isPending(BukkitTask task) {
    return this.bukkitScheduler.isQueued(task.getTaskId())
        || this.bukkitScheduler.isCurrentlyRunning(task.getTaskId());
  }

  public BukkitTask runTask(Runnable task) {
    return this.register(this.bukkitScheduler.runTask(this.plugin, task));
  }

  public BukkitTask runTaskLater(long delay, Runnable task) {
    return this.register(this.bukkitScheduler.runTaskLater(this.plugin, task, delay));
  }

  public BukkitTask runTaskLater(Duration delay, Runnable task) {
    return this.register(
        this.bukkitScheduler.runTaskLater(this.plugin, task, TimeUtils.toTicks(delay)));
  }

  public BukkitTask runTaskTimer(long interval, Runnable task) {
    return this.register(this.bukkitScheduler.runTaskTimer(this.plugin, task, 0L, interval));
  }

  public BukkitTask runTaskTimer(long delay, long interval, Runnable task) {
    return this.register(this.bukkitScheduler.runTaskTimer(this.plugin, task, delay, interval));
  }

  public BukkitTask runTaskTimer(Duration interval, Runnable task) {
    return this.register(
        this.bukkitScheduler.runTaskTimer(this.plugin, task, 0L, TimeUtils.toTicks(interval)));
  }

  public BukkitTask runTaskTimer(Duration delay, Duration interval, Runnable task) {
    return this.register(
        this.bukkitScheduler.runTaskTimer(
            this.plugin, task, TimeUtils.toTicks(delay), TimeUtils.toTicks(interval)));
  }

  public <T> Future<T> runMainThread(Callable<T> task) {
    if (this.plugin.getServer().isPrimaryThread()) {
      try {
        return CompletableFuture.completedFuture(task.call());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } else {
      return bukkitScheduler.callSyncMethod(this.plugin, task);
    }
  }
}
