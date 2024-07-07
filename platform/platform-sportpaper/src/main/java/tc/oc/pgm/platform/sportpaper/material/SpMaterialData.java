package tc.oc.pgm.platform.sportpaper.material;

import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.ItemMaterialData;

@SuppressWarnings("deprecation")
class SpMaterialData implements LegacyMaterialData, ItemMaterialData, BlockMaterialData {
  private final Material material;
  private final short damage;

  public SpMaterialData(Material material, short damage) {
    this.material = material;
    this.damage = damage;
  }

  public SpMaterialData(Material material) {
    this(material, (short) 0);
  }

  public SpMaterialData(org.bukkit.material.MaterialData md) {
    this(md.getItemType(), md.getData());
  }

  @Override
  public Material getItemType() {
    return material;
  }

  @Override
  public byte getData() {
    return (byte) damage;
  }

  @Override
  public void applyTo(Block block, boolean update) {
    block.setTypeIdAndData(material.getId(), (byte) damage, update);
  }

  @Override
  public void applyTo(BlockState block) {
    block.setMaterialData(new org.bukkit.material.MaterialData(material, (byte) damage));
  }

  @Override
  public ItemStack toItemStack(int amount) {
    return new ItemStack(material, amount, damage);
  }

  @Override
  public FallingBlock spawnFallingBlock(Location location) {
    return location.getWorld().spawnFallingBlock(location, material, (byte) damage);
  }

  @Override
  public void sendBlockChange(Player player, Location location) {
    player.sendBlockChange(location, material, (byte) damage);
  }

  @Override
  public int encoded() {
    return material.getId() + (((int) damage) << 12);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof LegacyMaterialData that)) return false;
    return damage == that.getData() && material == that.getItemType();
  }

  @Override
  public int hashCode() {
    return Objects.hash(material, damage);
  }
}
