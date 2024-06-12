package tc.oc.pgm.platform.sportpaper.material;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.material.MaterialData;

@SuppressWarnings("deprecation")
class SpMaterialData implements MaterialData {
  private final org.bukkit.material.MaterialData md;

  public SpMaterialData(org.bukkit.material.MaterialData md) {
    this.md = md;
  }

  @Override
  public Material getItemType() {
    return md.getItemType();
  }

  @Override
  public org.bukkit.material.MaterialData getBukkit() {
    return md;
  }

  @Override
  public void applyTo(Block block, boolean update) {
    block.setTypeIdAndData(md.getItemTypeId(), md.getData(), update);
  }

  @Override
  public void applyTo(BlockState block) {
    block.setMaterialData(md);
  }

  @Override
  public void applyTo(ItemStack item) {
    item.setType(md.getItemType());
    item.setData(md);
  }

  @Override
  public void sendBlockChange(Player player, Location location) {
    player.sendBlockChange(location, md.getItemTypeId(), md.getData());
  }

  @Override
  public int encoded() {
    return md.getItemTypeId() + (((int) md.getData()) << 12);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof MaterialData && md.equals(((MaterialData) o).getBukkit());
  }

  @Override
  public int hashCode() {
    return md.hashCode();
  }
}
