package tc.oc.pgm.shops;

import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.points.PointProvider;
import tc.oc.pgm.util.nms.NMSHacks;

public class ShopKeeper {

  private static final String METADATA_KEY = "SHOP_KEEPER";

  private final String name;
  private final PointProvider location;
  private final Class<? extends Entity> type;
  private final Shop shop;

  public ShopKeeper(
      @Nullable String name, PointProvider location, Class<? extends Entity> type, Shop shop) {
    this.name = name;
    this.location = location;
    this.type = type;
    this.shop = shop;
  }

  public Shop getShop() {
    return shop;
  }

  public Class<? extends Entity> getType() {
    return type;
  }

  public String getName() {
    return name == null || name.isEmpty() ? ChatColor.GRAY + getShop().getId() : colorize(name);
  }

  public void spawn(Match match) {
    if (match == null) throw new IllegalArgumentException("Match can not be null!");

    Location loc = location.getPoint(match, null);
    loc.getWorld().getChunkAt(loc); // Load chunk

    Entity keeper = loc.getWorld().spawn(loc, type);
    keeper.setCustomName(getName());
    keeper.setCustomNameVisible(true);
    keeper.setMetadata(METADATA_KEY, new FixedMetadataValue(PGM.get(), shop.getId()));
    NMSHacks.freezeEntity(keeper);
  }

  public static boolean isKeeper(Entity entity) {
    return entity != null && entity.hasMetadata(ShopKeeper.METADATA_KEY);
  }

  public static String getKeeperId(Entity entity) {
    MetadataValue meta = entity.getMetadata(ShopKeeper.METADATA_KEY, PGM.get());
    if (meta.asString() != null) {
      return meta.asString();
    }
    return null;
  }
}
