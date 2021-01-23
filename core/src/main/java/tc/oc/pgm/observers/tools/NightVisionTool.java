package tc.oc.pgm.observers.tools;

import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tc.oc.pgm.util.menu.InventoryMenu;
import tc.oc.pgm.util.menu.InventoryMenuItem;
import tc.oc.pgm.util.text.TextTranslations;

public class NightVisionTool implements InventoryMenuItem {

  @Override
  public Component getDisplayName() {
    return translatable("setting.nightvision", NamedTextColor.DARK_PURPLE);
  }

  @Override
  public List<String> getLore(Player player) {
    Component status =
        translatable(
            hasNightVision(player) ? "misc.on" : "misc.off",
            hasNightVision(player) ? NamedTextColor.GREEN : NamedTextColor.RED);
    Component lore = translatable("setting.nightvision.lore", NamedTextColor.GRAY, status);
    return Lists.newArrayList(TextTranslations.translateLegacy(lore, player));
  }

  @Override
  public Material getMaterial(Player player) {
    return hasNightVision(player) ? Material.POTION : Material.GLASS_BOTTLE;
  }

  @Override
  public void onInventoryClick(InventoryMenu menu, Player player, ClickType clickType) {
    toggleNightVision(player);
    menu.refreshWindow(player);
  }

  private boolean hasNightVision(Player player) {
    return player.hasPotionEffect(PotionEffectType.NIGHT_VISION);
  }

  public void toggleNightVision(Player player) {
    if (hasNightVision(player)) {
      player.removePotionEffect(PotionEffectType.NIGHT_VISION);
    } else {
      player.addPotionEffect(
          new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
    }
  }
}
