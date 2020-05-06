package tc.oc.pgm.settings.tools;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.text.Component;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tc.oc.pgm.settings.ObserverTool;
import tc.oc.pgm.util.menu.InventoryMenu;
import tc.oc.pgm.util.text.TextTranslations;

public class NightVisionTool implements ObserverTool {

  @Override
  public Component getName() {
    return TranslatableComponent.of("setting.nightvision");
  }

  @Override
  public TextColor getColor() {
    return TextColor.DARK_PURPLE;
  }

  @Override
  public List<String> getLore(Player player) {
    Component status =
        TranslatableComponent.of(hasNightVision(player) ? "misc.on" : "misc.off")
            .color(hasNightVision(player) ? TextColor.GREEN : TextColor.RED);
    Component lore =
        TranslatableComponent.of("setting.nightvision.lore").args(status).color(TextColor.GRAY);
    return Lists.newArrayList(TextTranslations.translateLegacy(lore, player));
  }

  @Override
  public Material getMaterial(Player player) {
    return hasNightVision(player) ? Material.POTION : Material.GLASS_BOTTLE;
  }

  @Override
  public void onClick(InventoryMenu menu, Player player, ClickType clickType) {
    toggleNightVision(player);
    menu.invalidate(player);
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
