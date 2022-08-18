package tc.oc.pgm.shops;

import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.points.PointProvider;
import tc.oc.pgm.util.nms.NMSHacks;

public class ShopKeeper {

  private static final String METADATA_KEY = "SHOP_KEEPER";

  private final String name;
  private final ImmutableList<PointProvider> location;
  private final Class<? extends Entity> type;
  private final Shop shop;

  public ShopKeeper(
      @Nullable String name,
      List<PointProvider> location,
      Class<? extends Entity> type,
      Shop shop) {
    this.name = name;
    this.location = ImmutableList.copyOf(location);
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

    for (Location loc :
        location.stream().map(pp -> pp.getPoint(match, null)).collect(Collectors.toList())) {
      World world = match.getWorld();
      Entity keeper = world.spawn(loc, type);
      keeper.setCustomName(getName());
      keeper.setCustomNameVisible(true);
      keeper.setMetadata(METADATA_KEY, new FixedMetadataValue(PGM.get(), shop.getId()));
      NMSHacks.freezeEntity(keeper);
    }
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
