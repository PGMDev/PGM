package tc.oc.pgm.settings;

import com.google.common.collect.Lists;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.menu.InventoryMenu;
import tc.oc.pgm.menu.InventoryMenuBuilder;
import tc.oc.pgm.menu.items.InventoryItem;
import tc.oc.pgm.menu.items.InventoryItemBuilder;
import tc.oc.pgm.menu.items.ItemBuilder;
import tc.oc.pgm.settings.tools.FlySpeedTool;
import tc.oc.pgm.settings.tools.GamemodeTool;
import tc.oc.pgm.settings.tools.NightVisionTool;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.ComponentRenderers;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;

@ListenerScope(MatchScope.LOADED)
public class SettingsMatchModule implements MatchModule, Listener {

  // Slot where tool item is placed
  public static final int TOOL_BUTTON_SLOT = 8;

  // Material of tool item item
  public static final Material TOOL_MATERIAL = Material.DIAMOND;

  public static final String INVENTORY_TITLE = "setting.title";
  public static final int SETTINGS_ROWS = (SettingKey.values().length + 9 - 1) / 9;

  private final Match match;
  private final InventoryMenu observerMenu;
  private final InventoryMenu otherMenu;

  public SettingsMatchModule(Match match) {
    this.match = match;

    ObserverTool[] tools =
        new ObserverTool[] {new FlySpeedTool(), new GamemodeTool(), new NightVisionTool()};
    int obsToolsRows = (tools.length + 9 - 1) / 9;

    InventoryMenuBuilder obsBuilder = new InventoryMenuBuilder(SETTINGS_ROWS + obsToolsRows);
    InventoryMenuBuilder otherBuilder = new InventoryMenuBuilder(SETTINGS_ROWS);

    obsBuilder.setName(new PersonalizedTranslatable(INVENTORY_TITLE).add(ChatColor.AQUA));
    otherBuilder.setName(new PersonalizedTranslatable(INVENTORY_TITLE).add(ChatColor.AQUA));

    for (int i = 0; i < SettingKey.values().length; i++) {
      SettingKey key = SettingKey.values()[i];
      InventoryItem invItem =
          InventoryItemBuilder.createItem((menu, player) -> createSettingMenuItem(player, key))
              .onClick(
                  (menu, player) -> {
                    player.getSettings().toggleValue(key);
                    menu.refresh(player);
                  })
              .build();

      obsBuilder.addItem(0, i, invItem);
      otherBuilder.addItem(0, i, invItem);
    }

    for (int i = 0; i < tools.length; i++) {
      ObserverTool tool = tools[i];
      obsBuilder.addItem(
          1,
          i,
          InventoryItemBuilder.createItem((menu, player) -> tool.createItem(player)).onClick(tool));
    }

    this.observerMenu = obsBuilder.build();
    this.otherMenu = otherBuilder.build();
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
    if (player.isObserving()) {
      observerMenu.openAsRoot(player);
    } else {
      otherMenu.openAsRoot(player);
    }
  }

  private void refreshKit(MatchPlayer player) {
    if (player.isObserving()) {
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
        new PersonalizedTranslatable("setting.lore").getPersonalizedText().color(ChatColor.GRAY);

    return ItemBuilder.of(TOOL_MATERIAL)
        .setName(ComponentRenderers.toLegacyText(displayName, player.getBukkit()))
        .setLore(ComponentRenderers.toLegacyText(lore, player.getBukkit()))
        .manipulateMeta(meta -> meta.addItemFlags(ItemFlag.values()))
        .stack();
  }

  private ItemStack createSettingMenuItem(MatchPlayer player, SettingKey setting) {
    SettingValue value = player.getSettings().getValue(setting);
    Material material = value.getMaterial();
    if (material == null) {
      material = setting.getMaterial();
    }
    ItemStack stack = new ItemStack(material);

    ItemMeta meta = stack.getItemMeta();
    meta.setDisplayName(setting.getDisplayName().render(player.getBukkit()).toLegacyText());
    Component lore = setting.getLore(value.getDisplayName());
    meta.setLore(Lists.newArrayList(ComponentRenderers.toLegacyText(lore, player.getBukkit())));
    meta.addItemFlags(ItemFlag.values());
    stack.setItemMeta(meta);

    return stack;
  }
}
