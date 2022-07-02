package tc.oc.pgm.loot;

import java.util.List;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.material.EnderChest;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.player.MatchPlayer;

public class LootMatchModule implements MatchModule {

  // things with inventories that can be kept inside. horses/donkeys with chests might have to be
  // added
  private static final Material[] CONTAINERS = {
    Material.CHEST,
    Material.ENDER_CHEST,
    Material.STORAGE_MINECART,
    Material.FURNACE,
    Material.TRAPPED_CHEST,
    Material.DISPENSER,
    Material.HOPPER,
    Material.DROPPER,
    Material.BREWING_STAND,
    Material.BEACON,
    Material.HOPPER_MINECART
  };
  private final Match match;
  private final List<LootableDefinition> definitions;

  public LootMatchModule(Match match, List<LootableDefinition> definitions) {
    this.match = match;
    this.definitions = definitions;
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  // InventoryOpenEvent (just player's inventory?)
  // PlayerInteractEvent (might be less buggy)
  // fills anything with an inventory and matches filter
  public void handleContainerInventory(PlayerInteractEvent event) {
    MatchPlayer matchPlayer = match.getPlayer(event.getPlayer());
    Material clickedMaterial = event.getClickedBlock().getType();
    if (ArrayUtils.contains(CONTAINERS, clickedMaterial)) {
      for (LootableDefinition definition : definitions) {
        Inventory containerInventory = null;
        switch (clickedMaterial) {
          case CHEST:
            Chest chest = (Chest) event.getClickedBlock().getState();
            containerInventory = chest.getBlockInventory();
            break;
          case STORAGE_MINECART:
            StorageMinecart storageMinecart = (StorageMinecart) event.getClickedBlock().getState();
            containerInventory = storageMinecart.getInventory();
            break;
          case ENDER_CHEST:
            EnderChest enderChest = (EnderChest) event.getClickedBlock().getState();
            // get inventory
            break;
          case DISPENSER:
            Dispenser dispenser = (Dispenser) event.getClickedBlock().getState();
            containerInventory = dispenser.getInventory();
            break;
        }
        if (containerInventory != null) {
          // add items that will always be in loot
          for (Loot loot : definition.lootableItems) {
            containerInventory.addItem(loot.getStack());
          }
          // add maybe items
          for (Maybe maybe : definition.maybeLootables) {
            // query filter, add items
            for (Loot loot : maybe.getMaybeItems()) {
              containerInventory.addItem(loot.getStack());
            }
          }
          for (Any any : definition.anyLootables) {
            // get the count of things
            for (Loot loot : any.getAnyItems()) {
              containerInventory.addItem(loot.getStack());
            }
          }
        }
      }
    }
  }
}
