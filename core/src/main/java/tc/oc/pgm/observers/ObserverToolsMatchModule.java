package tc.oc.pgm.observers;

import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.ImmutableList;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.ObserverInteractEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.observers.tools.FlySpeedTool;
import tc.oc.pgm.observers.tools.GamemodeTool;
import tc.oc.pgm.observers.tools.NightVisionTool;
import tc.oc.pgm.observers.tools.VisibilityTool;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;
import tc.oc.pgm.util.menu.InventoryMenu;
import tc.oc.pgm.util.menu.InventoryMenuItem;
import tc.oc.pgm.util.menu.pattern.SingleRowMenuArranger;

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
  private final InventoryMenu menu;
  private final InventoryMenuItem toolItem;

  public ObserverToolsMatchModule(Match match) {
    this.match = match;

    final List<InventoryMenuItem> tools =
        ImmutableList.of(
            new FlySpeedTool(), new NightVisionTool(), new VisibilityTool(), new GamemodeTool());

    this.menu =
        new InventoryMenu(
            match.getWorld(),
            translatable("setting.title", NamedTextColor.AQUA),
            tools,
            new SingleRowMenuArranger());

    this.toolItem = new ObserverToolsInventoryMenuItem(this.menu);
  }

  @EventHandler
  public void onObserverKitApply(ObserverKitApplyEvent event) {
    refreshKit(event.getPlayer());
  }

  @EventHandler(ignoreCancelled = true)
  public void onToolClick(ObserverInteractEvent event) {
    MatchPlayer player = event.getPlayer();
    if (player != null) {
      ItemStack item = event.getPlayer().getBukkit().getItemInHand();
      if (item != null && item.getType().equals(TOOL_MATERIAL) && canUse(player)) {
        this.toolItem.onInventoryClick(null, player.getBukkit(), ClickType.RIGHT);
      }
    }
  }

  public void openMenuManual(MatchPlayer player) {
    if (canUse(player)) {
      menu.display(player.getBukkit());
    }
  }

  private boolean canUse(MatchPlayer player) {
    return player.isObserving();
  }

  private void refreshKit(MatchPlayer player) {
    if (canUse(player)) {
      player.getInventory().setItem(TOOL_BUTTON_SLOT, toolItem.createItem(player.getBukkit()));
    }
  }
}
