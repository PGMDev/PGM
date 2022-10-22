package tc.oc.pgm.structure;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;

@FeatureInfo(name = "dynamic")
public class DynamicStructureDefinition extends SelfIdentifyingFeatureDefinition {

  private final StructureDefinition structure;
  private final Filter trigger;
  private final Filter passive;
  private final @Nullable Vector position;
  private final @Nullable Vector offset;

  DynamicStructureDefinition(
      String id,
      StructureDefinition structure,
      Filter trigger,
      Filter passive,
      @Nullable Vector position,
      @Nullable Vector offset) {
    super(id);
    this.structure = checkNotNull(structure);
    this.trigger = checkNotNull(trigger);
    this.passive = checkNotNull(passive);
    this.position = position;
    this.offset = offset;
  }

  /**
   * The structure this dynamic will place/clear
   *
   * @return the structure this dynamic will place/clear
   */
  public StructureDefinition getStructureDefinition() {
    return structure;
  }

  /**
   * The dynamic filter triggering place/clear commands for this dynamic
   *
   * @return The dynamic filter triggering place/clear commands for this dynamic
   */
  public Filter getTrigger() {
    return trigger;
  }

  /**
   * The filter used to filter weather a structure gets placed/cleared when the dynamic filter
   * rises/falls //TODO check this behaviour
   *
   * @return The filter used to filter weather a structure gets placed/cleared when the dynamic
   *     filter rises/falls //TODO check this behaviour
   */
  public Filter getPassive() {
    return passive;
  }

  /**
   * The position to place/clear the structure to/from. Can not be used if the offset attribute is
   * used.
   *
   * @return The position to place/clear this structure to/from
   */
  public @Nullable Vector getPosition() {
    return position;
  }

  /**
   * The offset to use when placing/clearing the structure. Can not be used if the position
   * attribute is used.
   *
   * @return The offset to use when placing/clearing the structure
   */
  public @Nullable Vector getOffset() {
    return offset;
  }
}
