package tc.oc.pgm.action.actions;

import java.util.Objects;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.shops.Shop;
import tc.oc.pgm.shops.ShopMatchModule;
import tc.oc.pgm.shops.menu.ShopMenu;

public class OpenShop extends AbstractAction<MatchPlayer> {
  final String shopId;

  public OpenShop(String shopId) {
    super(MatchPlayer.class);
    this.shopId = shopId;
  }

  @Override
  public void trigger(MatchPlayer player) {

    Shop shop = Objects.requireNonNull(player.getMatch().getModule(ShopMatchModule.class))
        .getShops()
        .get(shopId);

    if (shop == null) return;

    new ShopMenu(shop, player);
  }
}
