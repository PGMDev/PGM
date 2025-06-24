package tc.oc.pgm.goals;

import org.bukkit.Material;
import org.bukkit.block.Block;
import tc.oc.pgm.modes.Mode;
import tc.oc.pgm.util.material.BlockMaterialData;

public interface ModeChangeGoal<T extends GoalDefinition> extends Goal<T> {

  boolean isAffectedBy(Mode mode);

  void replaceBlocks(BlockMaterialData newMaterial);

  boolean isObjectiveMaterial(Block block);

  String getModeChangeMessage(Material material);
}
