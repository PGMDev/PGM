package tc.oc.pgm.goals;

import com.google.common.collect.ImmutableSet;
import org.bukkit.block.Block;
import tc.oc.pgm.modes.Mode;
import tc.oc.pgm.util.material.BlockMaterialData;

public interface ModeChangeGoal<T extends GoalDefinition> extends Goal<T> {

  void replaceBlocks(BlockMaterialData newMaterial);

  boolean isObjectiveMaterial(Block block);

  ImmutableSet<Mode> getModes();
}
