package tc.oc.pgm.shops;

import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.points.PointProvider;
import tc.oc.pgm.util.nms.NMSHacks;

public class ShopKeeper {

  public static final String METADATA_KEY = "SHOP_KEEPER";

  private final String name;
  private final ImmutableList<PointProvider> location;
  private final Class<? extends Entity> type;
  private final Shop shop;

  public ShopKeeper(
      @Nullable String name,
      ImmutableList<PointProvider> location,
      Class<? extends Entity> type,
      Shop shop) {
    this.name = name;
    this.location = location;
    this.type = type;
    this.shop = shop;
  }

  @Nullable
  public Location getLocation(Match match) {
    // TODO: support multiple shopkeeper locations?
    return location.stream().findAny().map(pp -> pp.getPoint(match, null)).orElse(null);
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
    if (getLocation(match) == null) return;

    World world = match.getWorld();
    Entity keeper = world.spawn(getLocation(match), type);
    keeper.setCustomName(getName());
    keeper.setCustomNameVisible(true);
    keeper.setMetadata(METADATA_KEY, new FixedMetadataValue(PGM.get(), shop.getId()));
    NMSHacks.freezeEntity(keeper);
  }
}
