package tc.oc.pgm.observers.tools;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
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
    return TranslatableComponent.of("setting.gamemode");
  }

  @Override
  public ChatColor getColor() {
    return ChatColor.DARK_AQUA;
  }

  @Override
  public List<String> getLore(MatchPlayer player) {
    Component gamemode =
        TranslatableComponent.of(
            "gameMode." + player.getGameMode().name().toLowerCase(), TextColor.AQUA);
    Component lore = TranslatableComponent.of("setting.gamemode.lore", TextColor.GRAY, gamemode);
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
        TextComponent.of("/tools", TextColor.AQUA)
            .hoverEvent(
                HoverEvent.showText(
                    TranslatableComponent.of("setting.gamemode.hover", TextColor.GRAY)))
            .clickEvent(ClickEvent.runCommand("/tools"));
    return TranslatableComponent.of("setting.gamemode.warning", TextColor.GRAY, command);
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
