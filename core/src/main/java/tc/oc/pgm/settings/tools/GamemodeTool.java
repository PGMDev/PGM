package tc.oc.pgm.settings.tools;

import com.google.common.collect.Lists;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.settings.ObserverTool;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.ComponentRenderers;
import tc.oc.pgm.util.component.types.PersonalizedText;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;
import tc.oc.pgm.util.menu.InventoryMenu;

public class GamemodeTool implements ObserverTool {

  private final Match match;

  /**
   * Constructor.
   *
   * @param match the match this tool is being used in
   */
  public GamemodeTool(Match match) {
    this.match = match;
  }

  @Override
  public Component getName() {
    return new PersonalizedTranslatable("setting.gamemode");
  }

  @Override
  public ChatColor getColor() {
    return ChatColor.DARK_AQUA;
  }

  @Override
  public List<String> getLore(Player player) {
    Component gamemode =
        new PersonalizedTranslatable("gameMode." + player.getGameMode().name().toLowerCase())
            .color(ChatColor.AQUA);
    Component lore =
        new PersonalizedTranslatable("setting.gamemode.lore", gamemode)
            .getPersonalizedText()
            .color(ChatColor.GRAY);
    return Lists.newArrayList(ComponentRenderers.toLegacyText(lore, player));
  }

  @Override
  public Material getMaterial(Player player) {
    return isCreative(player) ? Material.SEA_LANTERN : Material.PRISMARINE;
  }

  @Override
  public void onClick(InventoryMenu menu, Player player, ClickType clickType) {
    toggleObserverGameMode(player);
    menu.refresh(player);
  }

  public void toggleObserverGameMode(Player player) {
    player.setGameMode(getOppositeMode(player.getGameMode()));
    if (player.getGameMode() == GameMode.SPECTATOR) {
      match.getPlayer(player).sendWarning(getToggleMessage(), true);
    } else if (isCreative(player)) {
      // Note: When WorldEdit is present, this executes a command to ensure the player is not stuck
      if (Bukkit.getPluginManager().isPluginEnabled("WorldEdit")) {
        player.performCommand("worldedit:!");
      }
    }
  }

  private boolean isCreative(Player player) {
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
