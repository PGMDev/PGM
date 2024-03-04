package tc.oc.pgm.shops;

import static net.kyori.adventure.key.Key.key;
import static net.kyori.adventure.sound.Sound.sound;
import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.sound.Sound;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.shops.menu.Category;
import tc.oc.pgm.shops.menu.Icon;

public class Shop extends SelfIdentifyingFeatureDefinition {

  private static Sound PURCHASE_SOUND = sound(key("fire.ignite"), Sound.Source.MASTER, 1f, 1.4f);

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
        .collect(Collectors.toList());
  }

  public void purchase(Icon icon, MatchPlayer buyer) {
    if (icon.takePayment(buyer)) {
      icon.getAction().trigger(buyer);
      buyer.getBukkit().updateInventory();
      buyer.playSound(PURCHASE_SOUND);
    } else if (!buyer.getMatch().isRunning()) {
      buyer.sendWarning(translatable("match.error.noMatch"));
    } else {
      buyer.sendWarning(translatable("shop.currency.insufficient"));
    }
  }
}
