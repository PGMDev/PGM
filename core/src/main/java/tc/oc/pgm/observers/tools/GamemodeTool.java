package tc.oc.pgm.observers.tools;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.api.Audience;
import tc.oc.pgm.menu.MenuItem;
import tc.oc.pgm.util.text.TextTranslations;

public class GamemodeTool implements MenuItem {

  @Override
  public Component getDisplayName() {
    return translatable("setting.gamemode", NamedTextColor.DARK_AQUA);
  }

  @Override
  public List<String> getLore(Player player) {
    Component gamemode =
        translatable("misc." + player.getGameMode().name().toLowerCase(), NamedTextColor.AQUA);
    Component lore = translatable("setting.gamemode.lore", NamedTextColor.GRAY, gamemode);
    return Lists.newArrayList(TextTranslations.translateLegacy(lore, player));
  }

  @Override
  public Material getMaterial(Player player) {
    return isCreative(player) ? Material.SEA_LANTERN : Material.PRISMARINE;
  }

  @Override
  public void onClick(Player player, ClickType click) {
    player.setGameMode(getOppositeMode(player.getGameMode()));
    if (player.getGameMode() == GameMode.SPECTATOR) {
      Audience.get(player).sendWarning(getToggleMessage());
    } else if (isCreative(player)) {
      // Note: When WorldEdit is present, this executes a command to ensure the player is not stuck
      if (Bukkit.getPluginManager().isPluginEnabled("WorldEdit")
          && player.hasPermission("worldedit.navigation.unstuck")) {
        player.performCommand("worldedit:!");
      }
    }
  }

  private boolean isCreative(Player player) {
    return player.getGameMode().equals(GameMode.CREATIVE);
  }

  private Component getToggleMessage() {
    Component command =
        text("/tools", NamedTextColor.AQUA)
            .hoverEvent(showText(translatable("setting.gamemode.hover", NamedTextColor.GRAY)))
            .clickEvent(runCommand("/tools"));
    return translatable("setting.gamemode.warning", NamedTextColor.GRAY, command);
  }

  private GameMode getOppositeMode(GameMode mode) {
    switch (mode) {
      case CREATIVE:
        return GameMode.SPECTATOR;
      case SPECTATOR:
        return GameMode.CREATIVE;
      default:
        return mode;
    }
  }
}
