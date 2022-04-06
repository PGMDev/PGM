package tc.oc.pgm.api.time;

import java.time.Instant;
import org.apache.commons.lang3.builder.ToStringBuilder;

/** Represents a Minecraft server {@link Tick}. */
public final class Tick implements Comparable<Tick> {
  public final long tick;
  public final Instant instant;

  public Tick(long tick, Instant instant) {
    this.tick = tick;
    this.instant = instant;
  }

  @Override
  public int compareTo(Tick o) {
    return Long.compare(tick, o.tick);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Tick && tick == ((Tick) obj).tick;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(tick);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}
