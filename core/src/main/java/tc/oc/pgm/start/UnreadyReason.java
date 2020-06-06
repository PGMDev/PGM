package tc.oc.pgm.start;

import net.kyori.text.Component;

/**
 * Other modules can register instances of this with {@link StartMatchModule} to preempt match start
 * and display the reason to players.
 */
public interface UnreadyReason {
  /**
   * Why the match cannot start
   *
   * @return
   */
  Component getReason();

  /**
   * Can the match be forced to start by a user command, despite this reason? If this is false,
   * there is no way for any user to override this reason and start the match.
   */
  boolean canForceStart();
}
