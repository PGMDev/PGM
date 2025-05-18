package tc.oc.pgm.platform.modern.entities;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import tc.oc.pgm.util.nms.entities.BlockEntity;

public record DisplayEntity(Entity entity) implements BlockEntity {
    @Override
    public boolean isDisplayEntity() {
        return true;
    }

    public Location getLocation() {
        return entity.getLocation();
    }

    public void teleport(Location loc) {
        entity.teleport(loc);
    }

    @Override
    public void setBlock(Material block) {
        ((BlockDisplay) entity).setBlock(block.createBlockData());
    }

    @Override
    public void setTeleportationDuration(int duration) {
        ((BlockDisplay) entity).setTeleportDuration(duration);
    }

    @Override
    public void remove() {
        entity.remove();
    }
}