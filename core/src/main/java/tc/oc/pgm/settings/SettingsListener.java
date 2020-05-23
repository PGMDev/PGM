package tc.oc.pgm.settings;

import net.kyori.text.Component;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.ObserverInteractEvent;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.settings.tools.FlySpeedTool;
import tc.oc.pgm.settings.tools.GamemodeTool;
import tc.oc.pgm.settings.tools.NightVisionTool;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;
import tc.oc.pgm.util.inventory.ItemBuilder;
import tc.oc.pgm.util.menu.InventoryMenu;
import tc.oc.pgm.util.menu.InventoryMenuBuilder;
import tc.oc.pgm.util.menu.InventoryMenuListener;
import tc.oc.pgm.util.menu.item.InventoryItem;
import tc.oc.pgm.util.menu.item.InventoryItemBuilder;

public class SettingsListener implements Listener {

  // Slot where tool item is placed
  public static final int TOOL_BUTTON_SLOT = 8;

  // Material of tool item item
  public static final Material TOOL_MATERIAL = Material.DIAMOND;

  public static final String INVENTORY_TITLE = "setting.title";
  public static final int SETTINGS_ROWS = (SettingKey.values().length + 9 - 1) / 9;

  private final InventoryMenu observerMenu;
  private final InventoryMenu otherMenu;

  public SettingsListener() {
    ObserverTool[] tools =
        new ObserverTool[] {new FlySpeedTool(), new GamemodeTool(), new NightVisionTool()};
    int obsToolsRows = (tools.length + 9 - 1) / 9;

    InventoryMenuListener manager = PGM.get().getInventoryMenuListener();

    InventoryMenuBuilder obsBuilder =
        new InventoryMenuBuilder(manager, SETTINGS_ROWS + obsToolsRows);
    InventoryMenuBuilder otherBuilder = new InventoryMenuBuilder(manager, SETTINGS_ROWS);

    obsBuilder.setName(TranslatableComponent.of(INVENTORY_TITLE, TextColor.AQUA));
    otherBuilder.setName(TranslatableComponent.of(INVENTORY_TITLE, TextColor.AQUA));

    for (int i = 0; i < SettingKey.values().length; i++) {
      SettingKey key = SettingKey.values()[i];
      InventoryItem invItem =
          InventoryItemBuilder.createItem(
                  manager, (menu, player) -> createSettingMenuItem(player, key))
              .onClick(
                  (menu, player) -> {
                    PGM.get().getMatchManager().getPlayer(player).getSettings().toggleValue(key);
                    menu.invalidate(player);
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
          InventoryItemBuilder.createItem(manager, (menu, player) -> tool.createItem(player))
              .onClick(tool));
    }

    this.observerMenu = obsBuilder.build();
    this.otherMenu = otherBuilder.build();
  }

  @EventHandler
  public void onObserverKitApply(ObserverKitApplyEvent event) {
    refreshKit(event.getPlayer());
  }

  @EventHandler
  public void onToolClick(ObserverInteractEvent event) {
    if (event.getClickType() == ClickType.RIGHT) {
      ItemStack item = event.getPlayer().getBukkit().getItemInHand();

      if (item.getType().equals(TOOL_MATERIAL)) {
        openMenu(event.getPlayer());
      }
    }
  }

  public void openMenu(MatchPlayer player) {
    if (player.isObserving()) {
      observerMenu.open(player.getBukkit());
    } else {
      otherMenu.open(player.getBukkit());
    }
  }

  private void refreshKit(MatchPlayer player) {
    if (player.isObserving()) {
      player.getInventory().setItem(TOOL_BUTTON_SLOT, createToolItem(player.getBukkit()));
    }
  }

  private ItemStack createToolItem(Player player) {
    Component displayName =
        TranslatableComponent.of(INVENTORY_TITLE, TextColor.AQUA, TextDecoration.BOLD);
    Component lore = TranslatableComponent.of("setting.lore", TextColor.GRAY);

    return new ItemBuilder()
        .material(TOOL_MATERIAL)
        .name(player, displayName)
        .lore(player, lore)
        .flags(ItemFlag.values())
        .build();
  }

  private ItemStack createSettingMenuItem(Player player, SettingKey setting) {
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    SettingValue value = matchPlayer.getSettings().getValue(setting);
    Material material = value.getIcon();
    if (material == null) {
      material = setting.getIcon();
    }

    return new ItemBuilder()
        .material(material)
        .name(player, setting.getDisplayName())
        .lore(player, setting.getDescription(value.getDisplayName()))
        .flags(ItemFlag.values())
        .build();
  }
}
