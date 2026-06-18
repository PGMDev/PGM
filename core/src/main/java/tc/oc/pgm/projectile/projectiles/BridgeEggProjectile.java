package tc.oc.pgm.projectile.projectiles;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public record BridgeEggProjectile(
    int bridgeRange, List<Material> bridgeMaterials, boolean teamColor, boolean silent)
    implements PgmProjectile {

  @Override
  public Entity spawn(Player player, Vector velocity) {
    return player.launchProjectile(Egg.class, velocity);
  }
}
