package tc.oc.pgm.platform.v1_20_6.material;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialMatcher;

public interface ModernBlockMaterialData extends BlockMaterialData {
  BlockData getBlock();

  @Override
  default void applyTo(Block block, boolean update) {
    block.setBlockData(getBlock(), update);
  }

  @Override
  default void applyTo(BlockState block) {
    block.setBlockData(getBlock());
  }

  @Override
  default void sendBlockChange(Player player, Location location) {
    player.sendBlockChange(location, getBlock());
  }

  @Override
  default int encoded() {
    return getItemType().ordinal();
  }

  @Override
  default Material getItemType() {
    return getBlock().getMaterial();
  }

  @Override
  default MaterialMatcher toMatcher() {
    return new BlockStateMaterialMatcher(getBlock());
  }
}
