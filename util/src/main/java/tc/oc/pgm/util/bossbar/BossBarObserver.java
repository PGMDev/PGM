package tc.oc.pgm.util.bossbar;

/**
 * Something that can be notified whenever a {@link BossBar} is invalidated. In order to receive
 * these notifications, the observer must subscribe to them by calling {@link BossBar#addObserver}.
 */
public interface BossBarObserver {
  void invalidate(BossBar bar);
}
