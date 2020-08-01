package tc.oc.pgm.util.bossbar;

/**
 * Something that can be notified whenever a {@link BossBarSource} is invalidated. In order to
 * receive these notifications, the observer must subscribe to them by calling {@link
 * BossBarSource#addObserver}.
 */
public interface BossBarObserver {
  void invalidate(BossBarSource bar);
}
