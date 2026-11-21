package tc.oc.pgm.util.material;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;

public interface BlockMaterialData extends MaterialData {

  void applyTo(Block block, boolean update);

  void applyTo(BlockState block);

  void sendBlockChange(Player player, Location location);

  int encoded();

  /**
   * Check if this material data is the same as the material of the given block.
   *
   * @param block the block to compare against
   * @return true if the materials match
   */
  boolean sameMaterialAs(Block block);

  FallingBlock spawnFallingBlock(Location location);
}
