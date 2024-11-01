package tc.oc.pgm.structure;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;

@FeatureInfo(name = "dynamic")
public class DynamicStructureDefinition extends SelfIdentifyingFeatureDefinition {

  private final StructureDefinition structure;
  private final Filter trigger;
  private final Filter passive;
  private final @Nullable BlockVector position;
  private final @NotNull BlockVector offset;
  private final boolean update;

  DynamicStructureDefinition(
      String id,
      StructureDefinition structure,
      Filter trigger,
      Filter passive,
      boolean update,
      @Nullable BlockVector position,
      @Nullable BlockVector offset) {
    super(id);
    this.structure = assertNotNull(structure);
    this.trigger = assertNotNull(trigger);
    this.passive = assertNotNull(passive);
    this.update = update;
    this.position = position;
    this.offset = offset == null ? new BlockVector() : offset;
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
   * The filter used to filter weather a structure gets placed when the dynamic filter rises
   *
   * @return The filter used to filter weather a structure gets placed when the dynamic filter rises
   */
  public Filter getPassive() {
    return passive;
  }

  /** @return If this dynamic should generate block updates when placing */
  public boolean shouldUpdate() {
    return update;
  }

  /**
   * The offset to use when placing/clearing the structure. Can not be used if the position
   * attribute is used.
   *
   * @return The offset to use when placing/clearing the structure
   */
  public BlockVector getOffset() {
    if (position != null)
      return position.clone().subtract(this.structure.getOrigin()).toBlockVector();
    return offset.clone();
  }
}
