package tc.oc.pgm.platform.sportpaper.entities;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.nms.entities.BlockEntity;
import tc.oc.pgm.util.nms.entities.CustomEntities;
import tc.oc.pgm.util.platform.Supports;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

@Supports(SPORTPAPER)
public class SpCustomEntities implements CustomEntities {
    @Override
    public BlockEntity spawnBlockEntity(Location loc, BlockMaterialData blockMaterialData, float size, Vector velocity) {
        org.bukkit.entity.FallingBlock fallingBlock = blockMaterialData.spawnFallingBlock(loc);
        fallingBlock.setVelocity(velocity);

        return new tc.oc.pgm.platform.sportpaper.entities.FallingBlock(fallingBlock);
    }
}
