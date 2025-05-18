package tc.oc.pgm.platform.modern.entities;

import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.nms.entities.BlockEntity;
import tc.oc.pgm.util.nms.entities.CustomEntities;
import tc.oc.pgm.util.platform.Supports;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

@Supports(value = PAPER, minVersion = "1.20.6")
public class ModernCustomEntities implements CustomEntities {
    @Override
    public BlockEntity spawnBlockEntity(Location loc, BlockMaterialData blockMaterialData, float size) {
        Location initialLoc = loc.clone();
        loc.setPitch(0);
        loc.setYaw(0);

        final BlockDisplay entity = loc.getWorld().spawn(loc, BlockDisplay.class);
        DisplayEntity blockEntity = new DisplayEntity(entity);
        blockEntity.setBlock(blockMaterialData.getItemType());
//        blockEntity.align(initialLoc.getPitch(), initialLoc.getYaw(), size);

        final Matrix4f translation = new Matrix4f().translate(
                new Vector3f(
                        -0.5f * size,
                        -0.5f * size,
                        -0.5f * size
                ));

        final Matrix4f rotationMatrix = new Matrix4f();
        final Quaternionf rotation = new Quaternionf();
        rotation.rotateLocalX((float) Math.toRadians(-1 * initialLoc.getPitch()));
        rotation.rotateLocalY((float) Math.toRadians(180 - initialLoc.getYaw()));
        rotation.get(rotationMatrix);

        final Matrix4f scaleMatrix = new Matrix4f().scale(size);
        final Matrix4f transformationMatrix = rotationMatrix.mul(translation.mul(scaleMatrix));
        entity.setTransformationMatrix(transformationMatrix);

        return blockEntity;
    }
}
