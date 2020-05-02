package tc.oc.pgm.observers;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
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
import tc.oc.pgm.menu.InventoryMenu;
import tc.oc.pgm.menu.InventoryMenuBuilder;
import tc.oc.pgm.menu.items.InventoryItemBuilder;
import tc.oc.pgm.menu.items.ItemBuilder;
import tc.oc.pgm.observers.tools.FlySpeedTool;
import tc.oc.pgm.observers.tools.GamemodeTool;
import tc.oc.pgm.observers.tools.NightVisionTool;
import tc.oc.pgm.observers.tools.VisibilityTool;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.ComponentRenderers;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;

@ListenerScope(MatchScope.LOADED)
public class ObserverSettingsMatchModule implements MatchModule, Listener {

  public static class Factory implements MatchModuleFactory<ObserverSettingsMatchModule> {
    @Override
    public ObserverSettingsMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new ObserverSettingsMatchModule(match);
    }
  }

  // Slot where tool item is placed
  public static final int TOOL_BUTTON_SLOT = 8;

  // Material of tool item item
  public static final Material TOOL_MATERIAL = Material.DIAMOND;

  public static final String INVENTORY_TITLE = "setting.title";
  public static final int INVENTORY_ROWS = 1;

  private final Match match;
  private final InventoryMenu toolMenu;

  public ObserverSettingsMatchModule(Match match) {
    this.match = match;

    InventoryMenuBuilder builder = new InventoryMenuBuilder(INVENTORY_ROWS);
    builder.setName(new PersonalizedTranslatable(INVENTORY_TITLE).add(ChatColor.AQUA));

    ObserverTool[] tools =
        new ObserverTool[] {
          new FlySpeedTool(), new GamemodeTool(), new NightVisionTool(), new VisibilityTool()
        };
    for (int i = 0; i < tools.length; i++) {
      ObserverTool tool = tools[i];
      builder.addItem(
          0,
          1 + i * 2,
          InventoryItemBuilder.createItem((menu, player) -> tool.createItem(player)).onClick(tool));
    }

    this.toolMenu = builder.build();
  }

  @EventHandler
  public void onObserverKitApply(ObserverKitApplyEvent event) {
    refreshKit(event.getPlayer());
  }

  @EventHandler
  public void onToolClick(PlayerInteractEvent event) {
    if (isRightClick(event.getAction())) {
      ItemStack item = event.getPlayer().getItemInHand();

      if (item.getType().equals(TOOL_MATERIAL)) {
        MatchPlayer player = match.getPlayer(event.getPlayer());
        openMenu(player);
      }
    }
  }

  public void openMenu(MatchPlayer player) {
    if (canUse(player)) {
      toolMenu.openAsRoot(player);
    }
  }

  private boolean canUse(MatchPlayer player) {
    return player.isObserving();
  }

  private void refreshKit(MatchPlayer player) {
    if (canUse(player)) {
      player.getInventory().setItem(TOOL_BUTTON_SLOT, createToolItem(player));
    }
  }

  private boolean isRightClick(Action action) {
    return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
  }

  private ItemStack createToolItem(MatchPlayer player) {
    Component displayName =
        new PersonalizedTranslatable("setting.displayName")
            .getPersonalizedText()
            .color(ChatColor.AQUA)
            .bold(true);
    Component lore =
        new PersonalizedTranslatable("setting.lore")
            .getPersonalizedText()
            .color(ChatColor.GRAY);

    return ItemBuilder.of(TOOL_MATERIAL)
        .setName(ComponentRenderers.toLegacyText(displayName, player.getBukkit()))
        .setLore(ComponentRenderers.toLegacyText(lore, player.getBukkit()))
        .manipulateMeta(meta -> meta.addItemFlags(ItemFlag.values()))
        .stack();
  }
}
