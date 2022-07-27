package tc.oc.pgm.shops;

import static net.kyori.adventure.key.Key.key;
import static net.kyori.adventure.sound.Sound.sound;
import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.ImmutableList;
import java.util.List;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.shops.menu.Category;
import tc.oc.pgm.shops.menu.Icon;

@FeatureInfo(name = "shops")
public class Shop implements FeatureDefinition {

  private static Sound PURCHASE_SOUND = sound(key("fire.ignite"), Sound.Source.MASTER, 1f, 1.4f);

  private final String name;
  private final ImmutableList<Category> categories;

  public Shop(String name, List<Category> categories) {
    this.name = name;
    this.categories = ImmutableList.copyOf(categories);
  }

  public String getName() {
    return name;
  }

  public ImmutableList<Category> getCategories() {
    return categories;
  }

  public boolean canPurchase(Icon icon, MatchPlayer buyer) {
    Material currency = icon.getCurrency();
    int price = icon.getPrice();

    if (buyer.getMatch().isRunning() && buyer.isParticipating()) {
      PlayerInventory inventory = buyer.getInventory();
      return inventory.containsAtLeast(new ItemStack(currency), price);
    }

    return false;
  }

  public boolean purchase(Icon icon, MatchPlayer buyer) {
    if (!canPurchase(icon, buyer)) {
      if (!buyer.getMatch().isRunning()) {
        buyer.sendWarning(translatable("match.error.noMatch"));
      } else {
        buyer.sendWarning(translatable("shop.currency.insufficient"));
      }
      return false;
    }

    // Remove items from inventory
    PlayerInventory inventory = buyer.getInventory();
    for (int i = 0; i < icon.getPrice(); i++) {
      inventory.removeItem(new ItemStack(icon.getCurrency()));
    }
    buyer.getBukkit().updateInventory();
    buyer.applyKit(icon.getKit(), true);
    buyer.playSound(PURCHASE_SOUND);
    return true;
  }
}
