package tc.oc.pgm.api.time;

import java.time.Instant;
import org.jspecify.annotations.NonNull;

/** Represents a Minecraft server {@link Tick}. */
public record Tick(long tick, Instant instant) implements Comparable<Tick> {

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
  public @NonNull String toString() {
    return "Tick{tick=" + this.tick + ", epoch=" + this.instant.toEpochMilli() + "}";
  }
}
