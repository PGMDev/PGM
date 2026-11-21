package tc.oc.pgm.projectile.projectiles;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.nms.NMSHacks;

public record EntityProjectile(
    Class<? extends Entity> entity, BlockMaterialData blockMaterial, Float power, boolean precise)
    implements PgmProjectile {

  public Entity spawn(Player player, Vector velocity) {
    Entity projectile;
    if (Projectile.class.isAssignableFrom(entity)) {
      projectile = player.launchProjectile(entity.asSubclass(Projectile.class), velocity);
      if (projectile instanceof Fireball fireball && precise) {
        NMSHacks.NMS_HACKS.setFireballDirection(fireball, velocity);
      }
    } else {
      if (FallingBlock.class.isAssignableFrom(entity)) {
        projectile = blockMaterial.spawnFallingBlock(player.getEyeLocation());
      } else {
        projectile = player.getWorld().spawn(player.getEyeLocation(), entity);
      }
      projectile.setVelocity(velocity);
    }
    if (power != null && projectile instanceof Explosive) {
      ((Explosive) projectile).setYield(power);
    }
    return projectile;
  }
}
