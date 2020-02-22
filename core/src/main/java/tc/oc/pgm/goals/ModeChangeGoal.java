package tc.oc.pgm.goals;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.material.MaterialData;

public interface ModeChangeGoal<T extends GoalDefinition> extends Goal<T> {

  void replaceBlocks(MaterialData newMaterial);

  boolean isObjectiveMaterial(Block block);

  String getModeChangeMessage(Material material);

  boolean isAffectedByModeChanges();
}
