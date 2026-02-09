package tc.oc.pgm.shops;

import static tc.oc.pgm.shops.ShopKeeper.isKeeper;

import java.util.Map;
import java.util.Set;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.ObserverInteractEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.shops.menu.ShopMenu;

@ListenerScope(MatchScope.LOADED)
public class ShopMatchModule implements MatchModule, Listener {

  private final Match match;
  private final Map<String, Shop> shops;
  private final Set<ShopKeeper> shopKeepers;

  public ShopMatchModule(Match match, Map<String, Shop> shops, Set<ShopKeeper> shopKeepers) {
    this.match = match;
    this.shops = shops;
    this.shopKeepers = shopKeepers;
  }

  @Override
  public void load() {
    for (ShopKeeper keeper : shopKeepers) {
      keeper.spawn(match);
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onShopKeeperDamage(EntityDamageEvent event) {
    if (isKeeper(event.getEntity())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onClickShopKeeper(PlayerInteractEntityEvent event) {
    if (event.getRightClicked() == null) return;

    MatchPlayer player = match.getPlayer(event.getPlayer());
    if (player == null) return;

    Shop shop = getShopFromEntity(event.getRightClicked());
    if (shop != null) {
      new ShopMenu(shop, player);
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onClickShopKeeper(ObserverInteractEvent event) {
    if (isKeeper(event.getClickedEntity())) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onShopKeeperAltDamage(VehicleDamageEvent event) {
    if (isKeeper(event.getVehicle())) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onShopKeeperAltEnter(VehicleEnterEvent event) {
    if (isKeeper(event.getVehicle())) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onShopKeeperAltDestroy(VehicleDestroyEvent event) {
    if (isKeeper(event.getVehicle())) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onShopKeeperAltMove(VehicleEntityCollisionEvent event) {
    if (isKeeper(event.getVehicle())) {
      event.setCancelled(true);
    }
  }

  @Nullable
  private Shop getShopFromEntity(Entity entity) {
    if (isKeeper(entity)) {
      String keeperId = ShopKeeper.getKeeperId(entity);
      return shops.get(keeperId);
    }
    return null;
  }
}
