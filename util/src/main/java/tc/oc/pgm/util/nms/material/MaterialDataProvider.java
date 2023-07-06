package tc.oc.pgm.util.nms.material;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Minecart;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.nms.NMSHacks;

public interface MaterialDataProvider {

  MaterialDataProviderPlatform PROVIDER = NMSHacks.getMaterialDataProvider();

  static MaterialData from(int hash) {
    return PROVIDER.from(hash);
  }

  static MaterialData from(Block block) {
    return PROVIDER.from(block);
  }

  static MaterialData from(BlockState blockState) {
    return PROVIDER.from(blockState);
  }

  static MaterialData from(ItemStack itemStack) {
    return PROVIDER.from(itemStack);
  }

  static MaterialData from(Material material) {
    return PROVIDER.from(material);
  }

  static MaterialData from(Material material, byte data) {
    return PROVIDER.from(material, data);
  }

  static MaterialData from(Minecart minecart) {
    return PROVIDER.from(minecart);
  }

  static MaterialData from(ChunkSnapshot chunkSnapshot, int x, int y, int z) {
    return PROVIDER.from(chunkSnapshot, x, y, z);
  }

  static MaterialData from(EntityChangeBlockEvent event) {
    return PROVIDER.from(event);
  }

  static MaterialData from(String materialString) {
    if (materialString == null || materialString.isEmpty()) {
      return null;
    }
    return PROVIDER.from(materialString);
  }
}
