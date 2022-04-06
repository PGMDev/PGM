package tc.oc.pgm.goals;

import com.google.common.collect.ImmutableSet;
import org.bukkit.block.Block;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.api.goal.Goal;
import tc.oc.pgm.api.goal.GoalDefinition;
import tc.oc.pgm.modes.Mode;

public interface ModeChangeGoal<T extends GoalDefinition> extends Goal<T> {

  void replaceBlocks(MaterialData newMaterial);

  boolean isObjectiveMaterial(Block block);

  ImmutableSet<Mode> getModes();
}
