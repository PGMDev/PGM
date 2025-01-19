package tc.oc.pgm.shops;

import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.ImmutableList;
import java.util.List;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.shops.menu.Category;
import tc.oc.pgm.shops.menu.Icon;
import tc.oc.pgm.util.bukkit.Sounds;

public class Shop extends SelfIdentifyingFeatureDefinition {

  private final String name;
  private final ImmutableList<Category> categories;

  public Shop(String id, String name, List<Category> categories) {
    super(id);
    this.name = name != null ? name : id;
    this.categories = ImmutableList.copyOf(categories);
  }

  public String getName() {
    return name;
  }

  public List<Category> getCategories() {
    return categories;
  }

  public List<Category> getVisibleCategories(MatchPlayer player) {
    return categories.stream()
        .filter(c -> c.getFilter().query(player).isAllowed())
        .toList();
  }

  public void purchase(Icon icon, MatchPlayer buyer) {
    if (icon.takePayment(buyer)) {
      icon.getAction().trigger(buyer);
      buyer.getBukkit().updateInventory();
      buyer.playSound(Sounds.SHOP_PURCHASE);
    } else if (!buyer.getMatch().isRunning()) {
      buyer.sendWarning(translatable("match.error.noMatch"));
    } else {
      buyer.sendWarning(translatable("shop.currency.insufficient"));
    }
  }
}
