package tc.oc.pgm.platform.modern.material;

import net.minecraft.world.level.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

class ModernEncodeUtil {
  private static final int ENCODED_NULL_MATERIAL = -1;

  static int encode(BlockData blockData) {
    return Block.BLOCK_STATE_REGISTRY.getId(((CraftBlockData) blockData).getState());
  }

  static ModernBlockData decode(int encoded) {
    if (encoded == ENCODED_NULL_MATERIAL) return null;
    var vanillaBlockstate = Block.BLOCK_STATE_REGISTRY.byId(encoded);
    if (vanillaBlockstate == null) return null;
    return new ModernBlockData(vanillaBlockstate.createCraftBlockData());
  }
}
