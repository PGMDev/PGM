package tc.oc.pgm.platform.entity.potion;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.nms.entity.potion.EntityPotion;

public class EntityPotionBukkit implements EntityPotion {

  private final Location location;
  private final ItemStack potionItem;
  private ThrownPotion bukkitPotionEntity = null;

  public EntityPotionBukkit(Location location, ItemStack potionItem) {
    this.location = location;
    this.potionItem = potionItem;
  }

  @Override
  public void spawn() {
    this.bukkitPotionEntity =
        (ThrownPotion) this.location.getWorld().spawnEntity(location, EntityType.SPLASH_POTION);
    bukkitPotionEntity.setItem(potionItem);
  }

  @Override
  public Entity getBukkitEntity() {
    return this.bukkitPotionEntity;
  }
}
