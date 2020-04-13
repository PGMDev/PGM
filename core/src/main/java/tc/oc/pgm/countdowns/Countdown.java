package tc.oc.pgm.countdowns;

import java.time.Duration;

public abstract class Countdown {

  // The following are meant to be overridden by subclasses.
  public void onStart(Duration remaining, Duration total) {}

  public void onTick(Duration remaining, Duration total) {}

  public void onCancel(Duration remaining, Duration total) {}

  public abstract void onEnd(Duration total);
}
