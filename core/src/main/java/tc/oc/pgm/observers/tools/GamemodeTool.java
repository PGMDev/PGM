package tc.oc.pgm.observers.tools;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.InventoryMenu;
import tc.oc.pgm.menu.InventoryMenuItem;
import tc.oc.pgm.util.text.TextTranslations;

public class GamemodeTool implements InventoryMenuItem {

  @Override
  public Component getName() {
    return translatable("setting.gamemode");
  }

  @Override
  public ChatColor getColor() {
    return ChatColor.DARK_AQUA;
  }

  @Override
  public List<String> getLore(MatchPlayer player) {
    Component gamemode =
        translatable("gameMode." + player.getGameMode().name().toLowerCase(), NamedTextColor.AQUA);
    Component lore = translatable("setting.gamemode.lore", NamedTextColor.GRAY, gamemode);
    return Lists.newArrayList(TextTranslations.translateBaseComponent(lore, player.getBukkit()));
  }

  @Override
  public Material getMaterial(MatchPlayer player) {
    return isCreative(player) ? Material.SEA_LANTERN : Material.PRISMARINE;
  }

  @Override
  public void onInventoryClick(InventoryMenu menu, MatchPlayer player, ClickType clickType) {
    toggleObserverGameMode(player);
    menu.refreshWindow(player);
  }

  public void toggleObserverGameMode(MatchPlayer player) {
    player.setGameMode(getOppositeMode(player.getGameMode()));
    if (player.getGameMode() == GameMode.SPECTATOR) {
      player.sendWarning(getToggleMessage());
    } else if (isCreative(player)) {
      // Note: When WorldEdit is present, this executes a command to ensure the player is not stuck
      if (Bukkit.getPluginManager().isPluginEnabled("WorldEdit")) {
        player.getBukkit().performCommand("worldedit:!");
      }
    }
  }

  private boolean isCreative(MatchPlayer player) {
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
