package tc.oc.pgm.util.nms.entities;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface BlockEntity {
    Location getLocation();
    void teleport(Location loc);
    boolean isDisplayEntity();
    void setBlock(Material block);
    void setTeleportationDuration(int duration);
    void remove();
}