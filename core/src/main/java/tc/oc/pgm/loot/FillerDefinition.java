package tc.oc.pgm.loot;

import java.time.Duration;
import java.util.Objects;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.util.compose.Composition;

@FeatureInfo(name = "fill")
public class FillerDefinition implements FeatureDefinition {

  private final Composition<ItemStack> loot;
  private final Filter filter;
  private final Filter refillTrigger; // Dynamic

  private final Duration refillInterval;

  private final boolean clearBeforeRefill;

  public FillerDefinition(
      Composition<ItemStack> loot,
      Filter filter,
      Filter refillTrigger,
      Duration refillInterval,
      boolean clearBeforeRefill) {
    this.loot = loot;
    this.filter = filter;
    this.refillTrigger = refillTrigger;
    this.refillInterval = refillInterval;
    this.clearBeforeRefill = clearBeforeRefill;
  }

  /** Items to fill with */
  public Composition<ItemStack> getLoot() {
    return this.loot;
  }

  /** Blocks/entities that are fillable */
  public Filter getFilter() {
    return this.filter;
  }

  /** Refill all blocks/entities when this filter goes high. */
  public Filter getRefillTrigger() {
    return this.refillTrigger;
  }

  /** Refill an individual block/entity this much time after it was last filled */
  public Duration getRefillInterval() {
    return this.refillInterval;
  }

  /** Clear contents before refilling */
  public boolean cleanBeforeRefill() {
    return this.clearBeforeRefill;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FillerDefinition that = (FillerDefinition) o;
    return clearBeforeRefill == that.clearBeforeRefill
        && loot.equals(that.loot)
        && filter.equals(that.filter)
        && Objects.equals(refillTrigger, that.refillTrigger)
        && refillInterval.equals(that.refillInterval);
  }

  @Override
  public String toString() {
    return "FillerDefinition{"
        + "loot="
        + loot
        + ", filter="
        + filter
        + ", refillTrigger="
        + refillTrigger
        + ", refillInterval="
        + refillInterval
        + ", clearBeforeRefill="
        + clearBeforeRefill
        + '}';
  }

  @Override
  public int hashCode() {
    return Objects.hash(loot, filter, refillTrigger, refillInterval, clearBeforeRefill);
  }
}
