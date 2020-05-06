package tc.oc.pgm.settings.tools;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.text.Component;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.settings.ObserverTool;
import tc.oc.pgm.util.menu.InventoryMenu;
import tc.oc.pgm.util.text.TextTranslations;

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
    return TranslatableComponent.of("setting.gamemode");
  }

  @Override
  public TextColor getColor() {
    return TextColor.DARK_AQUA;
  }

  @Override
  public List<String> getLore(Player player) {
    Component gamemode =
        TranslatableComponent.of("setting.gamemode." + player.getGameMode().name().toLowerCase())
            .color(TextColor.AQUA);
    Component lore =
        TranslatableComponent.of("setting.gamemode.lore").args(gamemode).color(TextColor.GRAY);
    return Lists.newArrayList(TextTranslations.translateLegacy(lore, player));
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
      match.getPlayer(player).sendWarning(getToggleMessage());
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
        TranslatableComponent.of("/settings")
            .color(TextColor.AQUA)
            .hoverEvent(
                HoverEvent.of(
                    HoverEvent.Action.SHOW_TEXT,
                    TranslatableComponent.of("setting.gamemode.hover").color(TextColor.GRAY)))
            .clickEvent(ClickEvent.of(ClickEvent.Action.RUN_COMMAND, "/settings"));
    return TranslatableComponent.of("setting.gamemode.warning").args(command).color(TextColor.GRAY);
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
