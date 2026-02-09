package tc.oc.pgm.action.actions;

import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.shops.Shop;
import tc.oc.pgm.shops.menu.ShopMenu;

public class OpenShop extends AbstractAction<MatchPlayer> {
  final FeatureReference<Shop> shop;

  public OpenShop(FeatureReference<Shop> shop) {
    super(MatchPlayer.class);
    this.shop = shop;
  }

  @Override
  public void trigger(MatchPlayer player) {
    new ShopMenu(this.shop.get(), player);
  }
}
