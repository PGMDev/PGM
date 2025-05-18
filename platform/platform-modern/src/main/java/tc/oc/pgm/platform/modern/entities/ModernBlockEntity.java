package tc.oc.pgm.platform.modern.entities;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import tc.oc.pgm.platform.modern.material.ModernBlockMaterialData;
import tc.oc.pgm.util.bukkit.entities.BlockEntity;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = PAPER, minVersion = "1.21.1")
public class ModernBlockEntity implements BlockEntity.Factory {
  @Override
  public BlockEntity spawnBlockEntity(
      Location loc, BlockMaterialData blockMaterialData, float size, Vector velocity) {
    Location corrected = loc.clone();
    corrected.setPitch(0);
    corrected.setYaw(0);
    final BlockDisplay entity = loc.getWorld().spawn(corrected, BlockDisplay.class);
    entity.setBlock(((ModernBlockMaterialData) blockMaterialData).getBlock());
    entity.setTeleportDuration(1);

    final Matrix4f translation =
        new Matrix4f().translate(new Vector3f(-0.5f * size, -0.5f * size, -0.5f * size));

    final Matrix4f rotationMatrix = new Matrix4f();
    final Quaternionf rotation = new Quaternionf();
    rotation.rotateLocalX((float) Math.toRadians(-loc.getPitch()));
    rotation.rotateLocalY((float) Math.toRadians(180 - loc.getYaw()));
    rotation.get(rotationMatrix);

    final Matrix4f scaleMatrix = new Matrix4f().scale(size);
    final Matrix4f transformationMatrix = rotationMatrix.mul(translation.mul(scaleMatrix));
    entity.setTransformationMatrix(transformationMatrix);

    return new Impl(entity);
  }

  private record Impl(Entity entity) implements BlockEntity {

    public Location getLocation() {
      return entity.getLocation();
    }

    public void teleport(Location loc) {
      entity.teleport(loc);
    }

    @Override
    public void remove() {
      entity.remove();
    }
  }
}
