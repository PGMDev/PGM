package tc.oc.pgm.platform.sportpaper.entities;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftArmorStand;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftFallingSand;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.bukkit.entities.BlockEntity;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.platform.Supports;

@Supports(SPORTPAPER)
public class SpCustomEntities implements BlockEntity.Factory {
  @Override
  public BlockEntity spawnBlockEntity(
      Location loc, BlockMaterialData blockMaterialData, float size, Vector velocity) {
    var fallingBlock = blockMaterialData.spawnFallingBlock(loc);
    fallingBlock.setVelocity(velocity);

    var stand = loc.getWorld().spawn(loc, ArmorStand.class);
    stand.setCustomNameVisible(false);
    stand.setVisible(false);
    stand.setMarker(true);
    stand.setBasePlate(false);
    stand.setGravity(false);
    stand.setPassenger(fallingBlock);

    ((CraftEntity) stand).getHandle().noclip = true;
    ((CraftFallingSand) fallingBlock).getHandle().noclip = true;
    return new Impl(stand);
  }

  private record Impl(Entity entity) implements BlockEntity {

    public Location getLocation() {
      return entity.getLocation();
    }

    public void teleport(Location loc) {
      entity.teleport(loc);
      var nms = ((CraftArmorStand) entity).getHandle();
      nms.move(loc.getX() - nms.locX, loc.getY() - nms.locY, loc.getZ() - nms.locZ);
    }

    @Override
    public void remove() {
      entity.getPassenger().remove();
      entity.remove();
    }
  }
}
