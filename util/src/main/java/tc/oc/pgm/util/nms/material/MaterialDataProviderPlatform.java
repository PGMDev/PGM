package tc.oc.pgm.util.nms.material;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Minecart;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;

public interface MaterialDataProviderPlatform {
  MaterialData from(int hash);

  MaterialData from(Block block);

  MaterialData from(BlockState blockState);

  MaterialData from(ItemStack itemStack);

  MaterialData from(Material material);

  MaterialData from(Material material, byte data);

  MaterialData from(Minecart minecart);

  MaterialData from(ChunkSnapshot chunkSnapshot, int x, int y, int z);

  MaterialData from(EntityChangeBlockEvent event);
}
