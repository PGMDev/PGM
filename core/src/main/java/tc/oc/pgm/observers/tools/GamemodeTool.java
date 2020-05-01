package tc.oc.pgm.observers.tools;

import com.google.common.collect.Lists;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.InventoryMenu;
import tc.oc.pgm.menu.InventoryMenuItem;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.ComponentRenderers;
import tc.oc.pgm.util.component.types.PersonalizedText;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;

public class GamemodeTool implements InventoryMenuItem {

  @Override
  public Component getName() {
    return new PersonalizedTranslatable("setting.gamemode");
  }

  @Override
  public ChatColor getColor() {
    return ChatColor.DARK_AQUA;
  }

  @Override
  public List<String> getLore(MatchPlayer player) {
    Component gamemode =
        new PersonalizedTranslatable("gameMode." + player.getGameMode().name().toLowerCase())
            .color(ChatColor.AQUA);
    Component lore =
        new PersonalizedTranslatable("setting.gamemode.lore", gamemode)
            .getPersonalizedText()
            .color(ChatColor.GRAY);
    return Lists.newArrayList(ComponentRenderers.toLegacyText(lore, player.getBukkit()));
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
      player.sendWarning(getToggleMessage(), true);
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
        new PersonalizedText("/tools")
            .color(ChatColor.AQUA)
            .hoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new PersonalizedTranslatable("setting.gamemode.hover")
                    .getPersonalizedText()
                    .color(ChatColor.GRAY)
                    .render())
            .clickEvent(ClickEvent.Action.RUN_COMMAND, "/tools");
    return new PersonalizedTranslatable("setting.gamemode.warning", command)
        .getPersonalizedText()
        .color(ChatColor.GRAY);
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
