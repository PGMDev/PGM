package tc.oc.pgm.observers.tools;

import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.InventoryMenu;
import tc.oc.pgm.menu.InventoryMenuItem;
import tc.oc.pgm.util.text.TextTranslations;

public class NightVisionTool implements InventoryMenuItem {

  @Override
  public Component getName() {
    return translatable("setting.nightvision");
  }

  @Override
  public ChatColor getColor() {
    return ChatColor.DARK_PURPLE;
  }

  @Override
  public List<String> getLore(MatchPlayer player) {
    Component status =
        translatable(
            hasNightVision(player) ? "misc.on" : "misc.off",
            hasNightVision(player) ? NamedTextColor.GREEN : NamedTextColor.RED);
    Component lore = translatable("setting.nightvision.lore", NamedTextColor.GRAY, status);
    return Lists.newArrayList(TextTranslations.translateLegacy(lore, player.getBukkit()));
  }

  @Override
  public Material getMaterial(MatchPlayer player) {
    return hasNightVision(player) ? Material.POTION : Material.GLASS_BOTTLE;
  }

  @Override
  public void onInventoryClick(InventoryMenu menu, MatchPlayer player, ClickType clickType) {
    toggleNightVision(player);
    menu.refreshWindow(player);
  }

  private boolean hasNightVision(MatchPlayer player) {
    return player.getBukkit().hasPotionEffect(PotionEffectType.NIGHT_VISION);
  }

  public void toggleNightVision(MatchPlayer player) {
    if (hasNightVision(player)) {
      player.getBukkit().removePotionEffect(PotionEffectType.NIGHT_VISION);
    } else {
      player
          .getBukkit()
          .addPotionEffect(
              new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
    }
  }
}
