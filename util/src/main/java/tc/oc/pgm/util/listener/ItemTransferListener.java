package tc.oc.pgm.util.listener;

import static tc.oc.pgm.util.bukkit.BukkitUtils.parse;
import static tc.oc.pgm.util.bukkit.InventoryViewUtil.INVENTORY_VIEW;

import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.*;
import tc.oc.pgm.util.event.ItemTransferEvent;
import tc.oc.pgm.util.event.PlayerItemTransferEvent;

/** A listener that calls {@link ItemTransferEvent} and {@link PlayerItemTransferEvent}. */
public class ItemTransferListener implements Listener {
  private static final Sound ITEM_PICKUP =
      parse(Sound::valueOf, "ITEM_PICKUP", "ENTITY_ITEM_PICKUP");

  // Track players dropping an item stack from within an inventory GUI
  private boolean ignoreNextDropEvent;
  private boolean collectToCursor;

  @EventHandler(ignoreCancelled = true)
  public void onPlayerPickupItem(final PlayerPickupItemEvent event) {
    // When this event is fired, the ItemStack in the Item being picked up is temporarily
    // set to the amount that will actually be picked up, while the difference from the
    // actual amount in the stack is available from getRemaining(). When the event returns,
    // the original amount is restored to the stack, meaning that we can't change the amount
    // from inside the event, so instead we replace the entire stack.

    int initialQuantity = event.getItem().getItemStack().getAmount();
    PlayerItemTransferEvent transferEvent = new PlayerItemTransferEvent(
        event,
        ItemTransferEvent.Reason.PICKUP,
        event.getPlayer(),
        null,
        event.getPlayer().getInventory(),
        event.getItem().getItemStack(),
        event.getItem(),
        initialQuantity,
        INVENTORY_VIEW.getCursor(event.getPlayer().getOpenInventory()));

    callEvent(transferEvent);

    int quantity = Math.min(transferEvent.getQuantity(), initialQuantity);

    if (quantity < initialQuantity) {
      event.setCancelled(true);
      if (quantity > 0) {
        ItemStack stack = event.getItem().getItemStack().clone();
        stack.setAmount(stack.getAmount() - quantity);
        event.getItem().setItemStack(stack);

        stack = stack.clone();
        stack.setAmount(quantity);
        event.getPlayer().getInventory().addItem(stack);
        event.getPlayer().playSound(event.getPlayer().getLocation(), ITEM_PICKUP, 1, 1);
      }
    }
  }

  @EventHandler
  public void onBlockPickupItem(final InventoryPickupItemEvent event) {
    int initialQuantity =
        getQuantityPlaceable(event.getItem().getItemStack(), event.getInventory());
    ItemTransferEvent transferEvent = new ItemTransferEvent(
        event,
        ItemTransferEvent.Reason.PICKUP,
        null,
        event.getInventory(),
        event.getItem().getItemStack(),
        event.getItem(),
        initialQuantity);

    callEvent(transferEvent);

    if (initialQuantity != transferEvent.getQuantity() && !event.isCancelled()) {
      event.setCancelled(true);
      ItemStack stack = event.getItem().getItemStack();
      stack.setAmount(stack.getAmount() - transferEvent.getQuantity());
      stack = stack.clone();
      stack.setAmount(transferEvent.getQuantity());
      event.getInventory().addItem(stack);
    }
  }

  @EventHandler
  public void onPlayerClickInventory(final InventoryClickEvent event) {
    // Ignored actions
    switch (event.getAction()) {
      case CLONE_STACK: // Out of scope
      case COLLECT_TO_CURSOR: // Handled in onPlayerInventoryClick
      case NOTHING:
      case UNKNOWN:
        return;
    }

    // Get the player who clicked
    if (!(event.getWhoClicked() instanceof Player)) {
      // Can this happen?
      return;
    }
    Player player = (Player) event.getWhoClicked();

    // In a dual-inventory view, InventoryClickEvent.getInventory() always returns the top
    // inventory, so to figure out which one was actually clicked, we compare the view
    // slot with the inv slot. If they are the same, the click is in the top inv, because
    // it is always mapped to view slot 0. Otherwise, the click is in the bottom inv.
    // This is the Bukkit recommended way to do this.
    Inventory inventory = getLocalInventory(event.getView(), event.getRawSlot());

    if (event.getAction() == InventoryAction.SWAP_WITH_CURSOR) {
      // Click on a stack while holding a stack, fire 2 events for the stacks being swapped
      if (inventory.getHolder() == player) {
        // Swap with own inventory is not a transfer
        return;
      }
      boolean cancelled = event.isCancelled();
      int quantity = event.getCurrentItem().getAmount();

      // The take event has no items on the cursor, because those will be placed by the second event
      PlayerItemTransferEvent transferEvent = new PlayerItemTransferEvent(
          event,
          ItemTransferEvent.Reason.TAKE,
          player,
          inventory,
          null,
          event.getCurrentItem(),
          null,
          quantity,
          null);
      this.callEvent(transferEvent);
      cancelled = cancelled | event.isCancelled() | quantity != transferEvent.getQuantity();

      // Remove the item from the inventory so handlers of the second event can see that it is gone
      ItemStack oldInvStack = event.getCurrentItem();
      event.setCurrentItem(null);

      quantity = event.getCursor().getAmount();
      transferEvent = new PlayerItemTransferEvent(
          event,
          ItemTransferEvent.Reason.PLACE,
          player,
          null,
          inventory,
          event.getCursor(),
          null,
          event.getCursor().getAmount(),
          event.getCursor());
      event.setCancelled(cancelled | event.isCancelled() | quantity != transferEvent.getQuantity());

      // Replace the old item in the inventory
      event.setCurrentItem(oldInvStack);

      return;
    }

    // The remaining actions will generate one event at most
    ItemTransferEvent.Reason type;
    Inventory fromInventory = null;
    Inventory toInventory = null;
    ItemStack itemStack;

    // Determine inv, slot, and stack
    switch (event.getAction()) {
      case PICKUP_ALL:
      case PICKUP_SOME:
      case PICKUP_HALF:
      case PICKUP_ONE:
        if (inventory.getHolder() == player) {
          // Taking from own inventory is not a transfer
          return;
        }
        type = ItemTransferEvent.Reason.TAKE;
        itemStack = event.getCurrentItem();
        fromInventory = inventory;
        break;

      case PLACE_ALL:
      case PLACE_SOME:
      case PLACE_ONE:
        if (inventory.getHolder() == player) {
          // Placing in own inventory is not a transfer
          return;
        }
        type = ItemTransferEvent.Reason.PLACE;
        itemStack = event.getCursor();
        toInventory = inventory;
        break;

      case DROP_ONE_SLOT:
      case DROP_ALL_SLOT:
        type = ItemTransferEvent.Reason.DROP;
        itemStack = event.getCurrentItem();
        fromInventory = inventory;
        break;

      case DROP_ONE_CURSOR:
      case DROP_ALL_CURSOR:
        type = ItemTransferEvent.Reason.DROP;
        itemStack = event.getCursor();
        break;

      case MOVE_TO_OTHER_INVENTORY:
        itemStack = event.getCurrentItem();
        fromInventory = inventory;
        toInventory = getOtherInventory(event.getView(), fromInventory);

        if (toInventory == null || fromInventory.getHolder() == toInventory.getHolder()) {
          // shift-click to hotbar/armor slots
          return;
        }

        if (fromInventory.getHolder() == player && toInventory.getHolder() != player) {
          type = ItemTransferEvent.Reason.PLACE;
        } else if (fromInventory.getHolder() != player && toInventory.getHolder() == player) {
          type = ItemTransferEvent.Reason.TAKE;
        } else {
          type = ItemTransferEvent.Reason.TRANSFER;
        }
        break;

      case HOTBAR_SWAP:
      case HOTBAR_MOVE_AND_READD:
        // Use a hotkey to move a stack in or out of a hotbar slot. If moving a stack into a
        // hotbar slot that is already occupied by an incompatible stack, that stack will be
        // moved to the slot under the cursor, if that slot is in the player's inventory,
        // otherwise it will be moved to the first available slot in the player's inventory.
        if (inventory.getHolder() == player) {
          // Ignore intra-inventory swap
          return;
        }
        if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
          // Moving an item onto the hotbar
          type = ItemTransferEvent.Reason.TAKE;
          itemStack = event.getCurrentItem();
          fromInventory = inventory;
          toInventory = player.getInventory();
        } else {
          itemStack = player.getInventory().getItem(event.getHotbarButton());
          if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
          }
          // Moving an item out of the hotbar
          type = ItemTransferEvent.Reason.PLACE;
          fromInventory = player.getInventory();
          toInventory = inventory;
        }
        break;

      default:
        // TODO: add logger warning
        return;
    }

    int initialQuantity = 0;

    // Determine quantity
    switch (event.getAction()) {
      case PICKUP_ALL: // left-click stack with empty cursor
      case DROP_ALL_SLOT: // press control-drop key while hovering over stack
      case HOTBAR_SWAP:
      case HOTBAR_MOVE_AND_READD:
        initialQuantity = event.getCurrentItem().getAmount();
        break;

      case PLACE_ALL: // left-click with cursor stack on empty slot or matching stack with enough
        // space
      case DROP_ALL_CURSOR: // left-click outside of window with cursor stack
        initialQuantity = event.getCursor().getAmount();
        break;

      case PICKUP_SOME: // left/right-click oversized stack with empty cursor
        initialQuantity =
            Math.min(event.getCurrentItem().getAmount(), event.getCurrentItem().getMaxStackSize());
        break;

      case PLACE_SOME: // left-click with cursor stack on undersized-slot (e.g. beacon) or matching
        // stack without enough space
        initialQuantity = Math.min(
            event.getCursor().getAmount(),
            Math.min(event.getCursor().getMaxStackSize(), event.getInventory().getMaxStackSize()));
        ItemStack existingStack = event.getCurrentItem();
        if (existingStack != null) {
          initialQuantity -= existingStack.getAmount();
        }
        break;

      case PICKUP_HALF: // right-click stack with empty cursor (rounds up)
        initialQuantity = (event.getCurrentItem().getAmount() + 1) / 2;
        break;

      case PICKUP_ONE: // same cause as PICKUP_SOME
      case PLACE_ONE: // right-click with cursor stack on slot/stack with space
      case DROP_ONE_CURSOR: // right-click outside of window with cursor stack
      case DROP_ONE_SLOT: // press drop key while hovering over stack
        initialQuantity = 1;
        break;

      case MOVE_TO_OTHER_INVENTORY: // shift-click in a dual-inventory view
        initialQuantity = getQuantityPlaceable(event.getCurrentItem(), toInventory);
        break;
    }

    if (initialQuantity <= 0) {
      return;
    }

    PlayerItemTransferEvent transferEvent = new PlayerItemTransferEvent(
        event,
        type,
        player,
        fromInventory,
        toInventory,
        itemStack,
        null,
        initialQuantity,
        event.getCursor());

    callEvent(transferEvent);
    int quantity = Math.min(transferEvent.getQuantity(), initialQuantity);

    if (quantity < initialQuantity) {
      event.setCancelled(true);
      if (quantity > 0) {
        ItemStack item;
        ItemStack otherItem;

        switch (event.getAction()) {
          case PICKUP_ALL:
          case PICKUP_SOME:
          case PICKUP_HALF:
          case PICKUP_ONE:
            item = event.getCurrentItem();
            item.setAmount(item.getAmount() - quantity);

            otherItem = item.clone();
            otherItem.setAmount(quantity);
            INVENTORY_VIEW.setCursor(event.getView(), otherItem);
            break;

          case PLACE_ALL:
          case PLACE_SOME:
          case PLACE_ONE:
            otherItem = event.getCursor();
            otherItem.setAmount(otherItem.getAmount() - quantity);
            INVENTORY_VIEW.setCursor(event.getView(), otherItem);

            item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) {
              item = otherItem.clone();
              item.setAmount(quantity);
              event.setCurrentItem(item);
            } else {
              item.setAmount(item.getAmount() + quantity);
            }
            break;

          case DROP_ALL_CURSOR:
          case DROP_ONE_CURSOR:
            otherItem = event.getCursor();
            otherItem.setAmount(otherItem.getAmount() - quantity);
            INVENTORY_VIEW.setCursor(event.getView(), otherItem);

            item = otherItem.clone();
            item.setAmount(quantity);
            dropFromPlayer(player, item);
            break;

          case DROP_ALL_SLOT:
          case DROP_ONE_SLOT:
            item = event.getCurrentItem();
            item.setAmount(item.getAmount() - quantity);

            item = item.clone();
            item.setAmount(quantity);
            dropFromPlayer(player, item);
            break;

          case MOVE_TO_OTHER_INVENTORY:
            if (toInventory != null) {
              item = event.getCurrentItem();
              item.setAmount(item.getAmount() - quantity);

              item = item.clone();
              item.setAmount(quantity);
              toInventory.addItem(item);
            }
            break;

          case HOTBAR_SWAP:
          case HOTBAR_MOVE_AND_READD:
            otherItem = player.getInventory().getItem(event.getHotbarButton());

            item = event.getCurrentItem();
            if (item != null && item.getType() != Material.AIR) {
              // Move item onto hotbar
              item.setAmount(item.getAmount() - quantity);

              item = item.clone();
              item.setAmount(quantity);
              player.getInventory().setItem(event.getHotbarButton(), item);

              if (otherItem != null) {
                player.getInventory().addItem(otherItem);
              }
            } else if (otherItem != null && otherItem.getType() != Material.AIR) {
              // Move item off of hotbar
              otherItem.setAmount(otherItem.getAmount() - quantity);
              otherItem = otherItem.clone();
              otherItem.setAmount(quantity);
              event.setCurrentItem(otherItem);
            }
            break;
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void setIgnoreDropFlag(InventoryClickEvent event) {
    switch (event.getAction()) {
      case DROP_ALL_CURSOR:
      case DROP_ONE_CURSOR:
      case DROP_ALL_SLOT:
      case DROP_ONE_SLOT:
        // Make a note to ignore the PlayerDropItemEvent that will follow this one
        this.ignoreNextDropEvent = true;
        break;
    }
  }

  @EventHandler
  public void onPlayerDropItem(PlayerDropItemEvent event) {
    if (this.ignoreNextDropEvent) {
      this.ignoreNextDropEvent = false;
    } else {
      // If the ignore flag is clear, this drop was caused by something other than
      // an inventory click (e.g. drop key, death, etc), so an event has not yet been fired
      int initialQuantity = event.getItemDrop().getItemStack().getAmount();
      ItemStack stack = event.getItemDrop().getItemStack();
      PlayerItemTransferEvent transferEvent = new PlayerItemTransferEvent(
          event,
          ItemTransferEvent.Reason.DROP,
          event.getPlayer(),
          event.getPlayer().getInventory(),
          null,
          stack,
          event.getItemDrop(),
          initialQuantity,
          INVENTORY_VIEW.getCursor(event.getPlayer().getOpenInventory()));
      callEvent(transferEvent);

      if (!transferEvent.isCancelled() && transferEvent.getQuantity() < initialQuantity) {
        int diff = initialQuantity - transferEvent.getQuantity();
        stack.setAmount(stack.getAmount() - diff);
        stack = stack.clone();
        stack.setAmount(diff);
        event.getPlayer().getInventory().addItem(stack);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void collectToCursor(InventoryClickEvent event) {
    // If this hasn't been cancelled yet, cancel it so our implementation can take over
    if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
      event.setCancelled(true);
      this.collectToCursor = true;
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerInventoryClick(InventoryClickEvent event) {
    // Control-double-click on a stack, all similar stacks are moved to the cursor, up to the max
    // stack size
    // We cancel all of these and redo them ourselves.
    if (this.collectToCursor) {
      this.collectToCursor = false;

      if (!(event.getWhoClicked() instanceof Player player)) return;

      ItemStack cursor = event.getCursor().clone();
      var view = event.getView();
      var topInventory = INVENTORY_VIEW.getTopInventory(view);
      int totalSize = getViewSize(view, topInventory);

      for (int pass = 0; pass < 2; pass++) {
        for (int rawSlot = 0; rawSlot < totalSize; rawSlot++) {
          if (cursor.getAmount() >= cursor.getMaxStackSize()) {
            // If the gathered stack is full, we're done
            break;
          }

          ItemStack stack = INVENTORY_VIEW.getItem(view, rawSlot);
          // First pass takes incomplete stacks, second pass takes complete ones
          if (cursor.isSimilar(stack)
              && ((pass == 0 && stack.getAmount() < stack.getMaxStackSize())
                  || (pass == 1 && stack.getAmount() >= stack.getMaxStackSize()))) {
            // Calculate how much can be collected from this stack
            // If it is the output slot of a transaction preview, 0
            int quantity = (topInventory instanceof CraftingInventory && rawSlot == 0)
                    || (topInventory instanceof MerchantInventory && rawSlot == 2)
                ? 0
                : Math.min(stack.getAmount(), cursor.getMaxStackSize() - cursor.getAmount());
            Inventory localInventory = getLocalInventory(view, rawSlot);
            if (localInventory.getHolder() != player) {
              // If stack comes from an external inventory, fire a transfer event
              PlayerItemTransferEvent transferEvent = new PlayerItemTransferEvent(
                  event,
                  ItemTransferEvent.Reason.TAKE,
                  player,
                  localInventory,
                  null,
                  stack,
                  null,
                  quantity,
                  cursor);
              callEvent(transferEvent);
              if (transferEvent.isCancelled()) {
                // If the event is cancelled, don't transfer from this slot
                quantity = 0;
              } else {
                quantity = transferEvent.getQuantity();
              }
            }

            if (quantity > 0) {
              // Collect items from this stack to the cursor
              cursor.setAmount(cursor.getAmount() + quantity);
              if (quantity == stack.getAmount()) {
                INVENTORY_VIEW.setItem(view, rawSlot, null);
              } else {
                stack.setAmount(stack.getAmount() - quantity);
              }
            }
          }
        }
      }

      INVENTORY_VIEW.setCursor(view, cursor);
      player.updateInventory();
    }
  }

  private int getViewSize(InventoryView view, Inventory top) {
    // Modern view.countSlots() sums all slots (including armor & offhand), which out-of-bounds if
    // you try to later try to view.getItem(slot) with the highest numbers as they're not part of
    // the view. As a workaround, only use countSlots() when in the player's view (ie: the 2x2
    // Crafting view), otherwise hard-code the 36 slots of 9x4.
    if (top.getType().equals(InventoryType.CRAFTING)) return INVENTORY_VIEW.countSlots(view);
    return top.getSize() + 36;
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerDragInventory(InventoryDragEvent event) {
    // This is when you spread items evenly across slots by dragging
    if (!(event.getWhoClicked() instanceof Player)) {
      return;
    }
    Player player = (Player) event.getWhoClicked();

    ItemStack transferred = event.getOldCursor().clone();
    transferred.setAmount(0);
    Inventory externalInventory = null;

    for (Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
      Inventory inventory = getLocalInventory(event.getView(), entry.getKey());
      if (inventory.getHolder() != player) {
        // Add stacks to the total if they are dragged over an external inventory
        externalInventory = inventory;
        transferred.setAmount(transferred.getAmount() + entry.getValue().getAmount());
      }
    }

    if (externalInventory != null) {
      int initialQuantity = transferred.getAmount();
      PlayerItemTransferEvent transferEvent = new PlayerItemTransferEvent(
          event,
          ItemTransferEvent.Reason.PLACE,
          player,
          null,
          externalInventory,
          transferred,
          null,
          initialQuantity,
          event.getOldCursor());

      callEvent(transferEvent);

      if (initialQuantity != transferEvent.getQuantity()) {
        // If the quantity changes, we have to cancel the entire drag,
        // because bukkit does not let us modify the dragged stacks.
        event.setCancelled(true);
      }
    }
  }

  private static void callEvent(final ItemTransferEvent event) {
    Bukkit.getPluginManager().callEvent(event);
  }

  private static void dropFromPlayer(final Player player, final ItemStack stack) {
    final Item entity = player.getWorld().dropItem(player.getEyeLocation(), stack);
    entity.setVelocity(player.getLocation().getDirection().multiply(0.3));
  }

  private static Inventory getLocalInventory(final InventoryView view, final int rawSlot) {
    final int cookedSlot = INVENTORY_VIEW.convertSlot(view, rawSlot);
    if (cookedSlot == rawSlot) {
      return INVENTORY_VIEW.getTopInventory(view);
    } else {
      return INVENTORY_VIEW.getBottomInventory(view);
    }
  }

  private static Inventory getOtherInventory(final InventoryView view, final Inventory inventory) {
    if (INVENTORY_VIEW.getTopInventory(view) == inventory) {
      return INVENTORY_VIEW.getBottomInventory(view);
    } else {
      return INVENTORY_VIEW.getTopInventory(view);
    }
  }

  private static int getQuantityPlaceable(final ItemStack stack, final Inventory inventory) {
    int transferrable = 0;
    for (ItemStack slotStack : inventory.getContents()) {
      if (slotStack == null) {
        return stack.getAmount();
      } else if (slotStack.isSimilar(stack)) {
        transferrable += stack.getMaxStackSize() - slotStack.getAmount();
        if (transferrable >= stack.getAmount()) {
          return stack.getAmount();
        }
      }
    }
    return transferrable;
  }
}
