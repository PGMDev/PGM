package tc.oc.pgm.util.concurrent;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import tc.oc.pgm.util.TimeUtils;

/** An executor service that is backed by a runnable executor. */
public abstract class TaskExecutorService implements ScheduledExecutorService {

  private volatile boolean shutdown;
  private CountDownLatch terminated;
  private final Collection<Task<?>> tasks = new ConcurrentSkipListSet<>();

  /**
   * Run a task.
   *
   * @param task A task.
   * @param delayInTicks The delay in ticks.
   * @return A task id.
   */
  protected abstract int runTask(Runnable task, long delayInTicks);

  /**
   * Run a periodic task.
   *
   * @param task A task.
   * @param initialDelayInTicks The initial delay in ticks.
   * @param periodicDelayInTicks The periodic delay in ticks.
   * @return A task id.
   */
  protected abstract int runPeriodicTask(
      Runnable task, long initialDelayInTicks, long periodicDelayInTicks);

  /**
   * Cancel a task, based on its id.
   *
   * @param id A task id.
   */
  protected abstract void cancelTask(int id);

  @Override
  public void shutdown() {
    shutdown = true;
    terminated = new CountDownLatch(tasks.size());
  }

  @Override
  public List<Runnable> shutdownNow() {
    if (!isShutdown()) shutdown();
    List<Runnable> pending = ImmutableList.copyOf(tasks);

    for (Task<?> task : tasks) {
      task.cancel(true);
    }

    return pending;
  }

  @Override
  public boolean isShutdown() {
    return shutdown;
  }

  @Override
  public boolean isTerminated() {
    return terminated != null && terminated.getCount() == 0;
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    if (terminated == null) return false;

    terminated.await(timeout, unit);
    return isTerminated();
  }

  @Override
  public <T> CompletableFuture<T> submit(Callable<T> task) {
    return new Task<>(task);
  }

  @Override
  public <T> CompletableFuture<T> submit(Runnable task, T result) {
    return new Task<>(task, result);
  }

  @Override
  public CompletableFuture<?> submit(Runnable task) {
    return new Task<>(task, null);
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
    return new Task<>(task, null, -1, delay, unit);
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    return new Task<>(callable, -1, delay, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(
      Runnable task, long initialDelay, long period, TimeUnit unit) {
    return scheduleWithFixedDelay(
        task, initialDelay, period, unit); // FIXME: fixed rate != fixed delay
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(
      Runnable task, long initialDelay, long delay, TimeUnit unit) {
    return new Task<>(task, null, delay, initialDelay, unit);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
      throws InterruptedException {
    return invokeAll(tasks, 1, TimeUnit.DAYS);
  }

  @Override
  public <T> List<Future<T>> invokeAll(
      Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException {
    CompletableFuture<T>[] futures = new CompletableFuture[tasks.size()];

    int i = 0;
    for (Callable<T> task : tasks) {
      futures[i++] = submit(task);
    }

    try {
      CompletableFuture.allOf(futures).get(timeout, unit);
    } catch (ExecutionException | TimeoutException e) {
      throw new InterruptedException(e.getMessage());
    }

    return Lists.newArrayList(futures);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
      throws InterruptedException, ExecutionException {
    try {
      return invokeAny(tasks, 1, TimeUnit.DAYS);
    } catch (TimeoutException e) {
      throw new ExecutionException(e);
    }
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    CompletableFuture<T>[] futures = new CompletableFuture[tasks.size()];

    int i = 0;
    for (Callable<T> task : tasks) {
      futures[i++] = submit(task);
    }

    return (T) CompletableFuture.anyOf(futures).get(timeout, unit);
  }

  @Override
  public void execute(Runnable task) {
    submit(task);
  }

  private class Task<V> extends CompletableFuture<V> implements ScheduledFuture<V>, Runnable {

    private final int taskId;
    private final Callable<V> callable;
    private final boolean periodic;

    private Task(Callable<V> callable, long period, long delay, TimeUnit unit) {
      if (isShutdown()) throw new RejectedExecutionException();

      this.callable = callable;
      this.periodic = period >= 0;

      int taskId;
      if (periodic) {
        taskId =
            runPeriodicTask(this, TimeUtils.toTicks(delay, unit), TimeUtils.toTicks(period, unit));
      } else {
        taskId = runTask(this, TimeUtils.toTicks(delay, unit));
      }
      this.taskId = taskId;

      tasks.add(this);
    }

    private Task(Runnable task, V result, long period, long delay, TimeUnit unit) {
      this(Executors.callable(task, result), period, delay, unit);
    }

    private Task(Callable<V> callable) {
      this(callable, -1, 0, TimeUnit.NANOSECONDS);
    }

    private Task(Runnable task, V result) {
      this(Executors.callable(task, result));
    }

    @Override
    public void run() {
      if (isShutdown()) {
        cancel(true);
        return;
      }

      try {
        complete(callable.call());
      } catch (Throwable t) {
        t.printStackTrace();
        completeExceptionally(t);
      }
    }

    @Override
    public boolean complete(V value) {
      return complete(value, null);
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
      return complete(null, ex);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      if (super.cancel(mayInterruptIfRunning)) {
        tasks.remove(this);
        if (terminated != null) {
          terminated.countDown();
        }
        cancelTask(taskId);
        return true;
      }

      return false;
    }

    @Override
    public long getDelay(TimeUnit unit) {
      return 0;
    }

    @Override
    public int compareTo(Delayed o) {
      if (!(o instanceof Task)) return 0;
      return isDone() ? 1 : 0;
    }

    private boolean complete(V value, Throwable ex) {
      boolean done = false;

      if (!periodic && value != null) {
        done = super.complete(value);
      } else if (ex != null) {
        done = super.completeExceptionally(ex);
      }

      return !done || cancel(true);
    }
  }
}
