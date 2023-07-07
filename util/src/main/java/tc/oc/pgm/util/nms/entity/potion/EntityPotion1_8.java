package tc.oc.pgm.util.nms.entity.potion;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

public class EntityPotion1_8 implements EntityPotion {

  private net.minecraft.server.v1_8_R3.EntityPotion handle;

  public EntityPotion1_8(Location location, ItemStack potionItem) {
    handle =
        new net.minecraft.server.v1_8_R3.EntityPotion(
            ((CraftWorld) location.getWorld()).getHandle(),
            location.getX(),
            location.getY(),
            location.getZ(),
            CraftItemStack.asNMSCopy(potionItem));
  }

  @Override
  public void spawn() {
    handle.spawnIn(handle.getWorld());
  }

  @Override
  public Entity getBukkitEntity() {
    return handle.getBukkitEntity();
  }
}
