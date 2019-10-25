package tc.oc.pgm.match;

import tc.oc.util.logging.ClassLogger;

/** Scope: Match */
public abstract class MatchModule {
  protected final Match match;
  protected final ClassLogger logger;

  public MatchModule(Match match) {
    this.match = match;
    this.logger = acquireLogger();
  }

  protected ClassLogger acquireLogger() {
    return ClassLogger.get(this.match.getLogger(), this.getClass());
  }

  /**
   * Called before {@link #load()} to check if the module should load for this match. If this
   * returns false, the module will not be added to the context for the current match, will not be
   * registered for events or ticks, and no further callback methods will be called.
   *
   * <p>The module IS stored in the match context when this method is called, but is removed if the
   * method returns false.
   *
   * <p>The base implementation always returns true. Naturally, if a module returns false, it must
   * ensure that its constructor does not have any unwanted side-effects.
   */
  public boolean shouldLoad() {
    return true;
  }

  /**
   * Called immediately after a match is loaded. The map is loaded but players have not yet been
   * transitioned from any prior match.
   */
  public void load() {}

  /**
   * Called immediately before a match is unloaded. The map is still loaded but any players have
   * already been transitioned to the next match.
   */
  public void unload() {}

  /** Called when the match starts */
  public void enable() {}

  /** Called when the match ends */
  public void disable() {}

  public ClassLogger getLogger() {
    return this.logger;
  }

  public Match getMatch() {
    return this.match;
  }
}
