package tc.oc.pgm.settings.tools;

import com.google.common.collect.Lists;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tc.oc.pgm.settings.ObserverTool;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.ComponentRenderers;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;
import tc.oc.pgm.util.menu.InventoryMenu;

public class NightVisionTool implements ObserverTool {

  @Override
  public Component getName() {
    return new PersonalizedTranslatable("setting.nightvision");
  }

  @Override
  public ChatColor getColor() {
    return ChatColor.DARK_PURPLE;
  }

  @Override
  public List<String> getLore(Player player) {
    Component status =
        new PersonalizedTranslatable(hasNightVision(player) ? "misc.on" : "misc.off")
            .getPersonalizedText()
            .color(hasNightVision(player) ? ChatColor.GREEN : ChatColor.RED);
    Component lore =
        new PersonalizedTranslatable("setting.nightvision.lore", status)
            .getPersonalizedText()
            .color(ChatColor.GRAY);
    return Lists.newArrayList(ComponentRenderers.toLegacyText(lore, player));
  }

  @Override
  public Material getMaterial(Player player) {
    return hasNightVision(player) ? Material.POTION : Material.GLASS_BOTTLE;
  }

  @Override
  public void onClick(InventoryMenu menu, Player player, ClickType clickType) {
    toggleNightVision(player);
    menu.refresh(player);
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
