package tc.oc.pgm.observers;

import static net.kyori.adventure.text.Component.translatable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;
import tc.oc.pgm.util.inventory.ItemBuilder;
import tc.oc.pgm.util.text.TextTranslations;

@ListenerScope(MatchScope.LOADED)
public class ObserverToolsMatchModule implements MatchModule, Listener {

  public static class Factory implements MatchModuleFactory<ObserverToolsMatchModule> {
    @Override
    public ObserverToolsMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new ObserverToolsMatchModule(match);
    }
  }

  public static final int TOOL_SLOT = 8;
  public static final Material TOOL_MATERIAL = Material.DIAMOND;
  public static final Component TOOL_NAME =
      translatable("setting.displayName", NamedTextColor.AQUA, TextDecoration.BOLD);
  public static final Component TOOL_LORE = translatable("setting.lore", NamedTextColor.GRAY);

  private final Match match;

  public ObserverToolsMatchModule(Match match) {
    this.match = match;
  }

  @EventHandler
  public void onObserverKitApply(ObserverKitApplyEvent event) {
    refreshKit(event.getPlayer());
  }

  @EventHandler
  public void onToolClick(PlayerInteractEvent event) {
    if (isRightClick(event.getAction())) {
      ItemStack item = event.getPlayer().getItemInHand();
      MatchPlayer player = match.getPlayer(event.getPlayer());
      if (player != null && item != null && item.isSimilar(createItem(player.getBukkit()))) {
        openMenu(player);
      }
    }
  }

  public void openMenu(MatchPlayer player) {
    if (canUse(player)) {
      new ObserverToolsMenu(player);
    }
  }

  private void refreshKit(MatchPlayer player) {
    if (canUse(player)) {
      player.getInventory().setItem(TOOL_SLOT, createItem(player.getBukkit()));
    }
  }

  private boolean canUse(MatchPlayer player) {
    return player.isObserving();
  }

  private boolean isRightClick(Action action) {
    return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
  }

  private ItemStack createItem(Player player) {
    return new ItemBuilder()
        .material(TOOL_MATERIAL)
        .amount(1)
        .name(TextTranslations.translateLegacy(TOOL_NAME, player))
        .lore(TextTranslations.translateLegacy(TOOL_LORE, player))
        .flags(ItemFlag.values())
        .build();
  }
}
