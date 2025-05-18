package tc.oc.pgm.platform.sportpaper.entities;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import tc.oc.pgm.util.nms.entities.BlockEntity;

public record FallingBlock(Entity entity) implements BlockEntity {
    @Override
    public boolean isDisplayEntity() {
        return false;
    }

    public Location getLocation() {
        return entity.getLocation();
    }

    public void teleport(Location loc) {
        entity.teleport(loc);
    }

    @Override
    public void setBlock(Material block) { }

    @Override
    public void setTeleportationDuration(int duration) { }

    @Override
    public void remove() {
        entity.remove();
    }
}
