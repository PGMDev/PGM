package tc.oc.pgm.projectile.projectiles;

import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.material.BlockMaterialData;

public record BridgeEggProjectile(
    int bridgeRange, BlockMaterialData bridgeMaterial, boolean teamColor, boolean silent)
    implements PgmProjectile {

  @Override
  public Entity spawn(Player player, Vector velocity) {
    return player.launchProjectile(Egg.class, velocity);
  }
}
