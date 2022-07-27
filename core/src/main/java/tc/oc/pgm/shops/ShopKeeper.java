package tc.oc.pgm.shops;

import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.util.nms.NMSHacks;

public class ShopKeeper {

  public static final String METADATA_KEY = "SHOP_KEEPER";

  private final String name;
  private final Vector location;
  private final float yaw;
  private final float pitch;
  private final Class<? extends Entity> type;
  private final Shop shop;

  public ShopKeeper(
      @Nullable String name,
      Vector location,
      float yaw,
      float pitch,
      Class<? extends Entity> type,
      Shop shop) {
    this.name = name;
    this.location = location;
    this.yaw = yaw;
    this.pitch = pitch;
    this.type = type;
    this.shop = shop;
  }

  public Vector getLocation() {
    return location;
  }

  public Shop getShop() {
    return shop;
  }

  public Class<? extends Entity> getType() {
    return type;
  }

  public String getName() {
    return name == null || name.isEmpty() ? ChatColor.GRAY + getShop().getName() : colorize(name);
  }

  public void spawn(World world) {
    Entity keeper = world.spawn(getLocation().toLocation(world, yaw, pitch), type);
    keeper.setCustomName(getName());
    keeper.setCustomNameVisible(true);
    keeper.setMetadata(METADATA_KEY, new FixedMetadataValue(PGM.get(), shop.getName()));
    NMSHacks.freezeEntity(keeper);
  }
}
