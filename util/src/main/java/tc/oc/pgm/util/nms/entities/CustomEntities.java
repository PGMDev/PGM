package tc.oc.pgm.util.nms.entities;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.material.BlockMaterialData;

public interface CustomEntities {
    BlockEntity spawnBlockEntity(Location loc, BlockMaterialData blockMaterialData, float size, Vector velocity);
}
