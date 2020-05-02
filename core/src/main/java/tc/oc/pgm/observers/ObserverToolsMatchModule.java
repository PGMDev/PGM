package tc.oc.pgm.observers;

import com.google.common.collect.Lists;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
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
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.menu.InventoryMenu;
import tc.oc.pgm.menu.InventoryMenuItem;
import tc.oc.pgm.observers.tools.FlySpeedTool;
import tc.oc.pgm.observers.tools.GamemodeTool;
import tc.oc.pgm.observers.tools.NightVisionTool;
import tc.oc.pgm.observers.tools.VisibilityTool;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.ComponentRenderers;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;

@ListenerScope(MatchScope.LOADED)
public class ObserverToolsMatchModule implements MatchModule, Listener {

  public static class Factory implements MatchModuleFactory<ObserverToolsMatchModule> {
    @Override
    public ObserverToolsMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new ObserverToolsMatchModule(match);
    }
  }

  // Slot where tool item is placed
  public static final int TOOL_BUTTON_SLOT = 8;

  // Material of tool item item
  public static final Material TOOL_MATERIAL = Material.DIAMOND;

  private final Match match;
  private ObserverToolMenu menu;

  public ObserverToolsMatchModule(Match match) {
    this.match = match;
    this.menu = new ObserverToolMenu();
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

  @EventHandler(priority = EventPriority.LOWEST)
  public void onInventoryClick(final InventoryClickEvent event) {
    if (event.getCurrentItem() == null
        || event.getCurrentItem().getItemMeta() == null
        || event.getCurrentItem().getItemMeta().getDisplayName() == null) return;

    if (event.getWhoClicked() instanceof Player) {
      MatchPlayer player = match.getPlayer(event.getWhoClicked());
      if (menu.isViewing(player)) {
        ItemStack clicked = event.getCurrentItem();
        menu.getTools()
            .forEach(
                tool -> {
                  if (clicked.getType().equals(tool.getMaterial(player))) {
                    tool.onInventoryClick(menu, player, event.getClick());
                  }
                });
      }
    }
  }

  @EventHandler
  public void onInventoryClose(final InventoryCloseEvent event) {
    // Remove viewing of menu upon inventory close
    menu.remove(match.getPlayer((Player) event.getPlayer()));
  }

  public void openMenu(MatchPlayer player) {
    if (canUse(player)) {
      menu.display(player);
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
    ItemStack tool = new ItemStack(TOOL_MATERIAL);
    ItemMeta meta = tool.getItemMeta();
    Component displayName =
        new PersonalizedTranslatable("setting.displayName")
            .getPersonalizedText()
            .color(ChatColor.AQUA)
            .bold(true);
    Component lore =
        new PersonalizedTranslatable("setting.lore").getPersonalizedText().color(ChatColor.GRAY);
    meta.setDisplayName(ComponentRenderers.toLegacyText(displayName, player.getBukkit()));
    meta.setLore(Lists.newArrayList(ComponentRenderers.toLegacyText(lore, player.getBukkit())));
    meta.addItemFlags(ItemFlag.values());
    tool.setItemMeta(meta);
    return tool;
  }

  public class ObserverToolMenu extends InventoryMenu {

    public static final String INVENTORY_TITLE = "setting.title";
    public static final int INVENTORY_ROWS = 1;

    private List<InventoryMenuItem> tools;

    public ObserverToolMenu() {
      super(INVENTORY_TITLE, INVENTORY_ROWS);
      registerTools();
    }

    public List<InventoryMenuItem> getTools() {
      return tools;
    }

    // Register each of the observer tools
    // TODO?: Add config options to enable/disable each tool
    private void registerTools() {
      this.tools = Lists.newArrayList();
      this.tools.add(new FlySpeedTool());
      this.tools.add(new NightVisionTool());
      this.tools.add(new VisibilityTool());
      this.tools.add(new GamemodeTool());
    }

    @Override
    public String getTranslatedTitle(MatchPlayer player) {
      return ChatColor.AQUA + super.getTranslatedTitle(player);
    }

    @Override
    public ItemStack[] createWindowContents(MatchPlayer player) {
      List<ItemStack> items = Lists.newArrayList();

      items.add(null);
      for (InventoryMenuItem tool : tools) {
        items.add(tool.createItem(player));
        if (items.size() < ROW_WIDTH - 1) {
          items.add(null);
        }
      }
      return items.toArray(new ItemStack[items.size()]);
    }
  }
}
