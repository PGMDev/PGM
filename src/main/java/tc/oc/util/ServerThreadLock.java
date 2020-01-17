package tc.oc.util;

import java.util.concurrent.CountDownLatch;
import tc.oc.pgm.api.PGM;

/** A lock that runs code on the {@link org.bukkit.Bukkit#getServer()} main thread. */
public class ServerThreadLock implements AutoCloseable {

  private final CountDownLatch waiting = new CountDownLatch(1);
  private final CountDownLatch done = new CountDownLatch(1);

  public static ServerThreadLock acquire() {
    return new ServerThreadLock();
  }

  private ServerThreadLock() {
    if (PGM.get().getServer().isPrimaryThread()) {
      waiting.countDown();
    } else {
      PGM.get().getServer().getScheduler().scheduleSyncDelayedTask(PGM.get(), this::signal);
      await();
    }
  }

  @Override
  public void close() {
    done.countDown();
  }

  private void await() {
    try {
      waiting.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void signal() {
    waiting.countDown();
    try {
      done.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
