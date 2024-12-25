package tc.oc.pgm.platform.sportpaper.entities;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import net.minecraft.server.v1_8_R3.EntityArmorStand;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftArmorStand;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftFallingSand;
import org.bukkit.entity.ArmorStand;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.bukkit.entities.BlockEntity;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.platform.Supports;

@Supports(SPORTPAPER)
public class SpBlockEntity implements BlockEntity.Factory {
  // We use a falling sand entity, which is 1 block high, so we want to lower actual positon by half
  // of that
  private static final double OFFSET = 0.5d;

  @Override
  public BlockEntity spawnBlockEntity(
      Location center, BlockMaterialData blockMaterialData, float size, Vector velocity) {
    center.setY(center.getY() - OFFSET);
    var fallingBlock = blockMaterialData.spawnFallingBlock(center);
    var stand = center.getWorld().spawn(center, ArmorStand.class);
    stand.setCustomNameVisible(false);
    stand.setVisible(false);
    stand.setMarker(true);
    stand.setBasePlate(false);
    stand.setGravity(false);
    stand.setPassenger(fallingBlock);

    // Reset the original location
    center.setY(center.getY() + OFFSET);

    var nmsStand = ((CraftArmorStand) stand).getHandle();
    var nmsFallingSand = ((CraftFallingSand) fallingBlock).getHandle();
    // Noclip makes movement ignore all collisions (faster), which we don't need as we run our own
    nmsStand.noclip = true;
    nmsFallingSand.noclip = true;

    return new Impl(stand, nmsStand);
  }

  private record Impl(ArmorStand bukkit, EntityArmorStand nms) implements BlockEntity {
    @Override
    public void teleport(Location l) {
      // We NEED to use NMS move because bukkit's teleport does not work on entities with passengers
      nms.move(l.getX() - nms.locX, l.getY() - nms.locY - OFFSET, l.getZ() - nms.locZ);
    }

    @Override
    public void remove() {
      bukkit.getPassenger().remove();
      bukkit.remove();
    }
  }
}
