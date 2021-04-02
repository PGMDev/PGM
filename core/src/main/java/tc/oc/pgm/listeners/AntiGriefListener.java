package tc.oc.pgm.listeners;

import static net.kyori.adventure.key.Key.key;
import static net.kyori.adventure.sound.Sound.sound;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;
import tc.oc.pgm.tnt.TNTMatchModule;
import tc.oc.pgm.tracker.Trackers;
import tc.oc.pgm.util.event.sport.player.PlayerAttackEntityEvent;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.MinecraftComponent;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.TextTranslations;

public class AntiGriefListener implements Listener {

  private static final Material DEFUSE_ITEM = Material.SHEARS;
  private static final int DEFUSE_SLOT = 4;

  private static final Sound DEFUSE_SOUND = sound(key("random.fizz"), Sound.Source.MASTER, 1, 1);

  private final MatchManager mm;

  public AntiGriefListener(MatchManager mm) {
    this.mm = mm;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void rightClickDefuse(final PlayerInteractEntityEvent event) {
    ItemStack hand = event.getPlayer().getItemInHand();
    if (hand == null || hand.getType() != DEFUSE_ITEM) return;

    this.participantDefuse(event.getPlayer(), event.getRightClicked());
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void leftClickDefuse(final PlayerAttackEntityEvent event) {
    this.participantDefuse(event.getPlayer(), event.getLeftClicked());
  }

  private void participantDefuse(Player player, Entity entity) {
    // check tnt
    if (!(entity instanceof TNTPrimed)) return;

    TNTMatchModule tntmm = mm.getMatch(player.getWorld()).getModule(TNTMatchModule.class);
    if (tntmm != null && !tntmm.getProperties().friendlyDefuse) return;

    MatchPlayer clicker = this.mm.getPlayer(player);
    if (clicker == null || !clicker.canInteract()) return;

    // check water
    Block block = entity.getLocation().getBlock();
    if (block != null
        && (block.getType() == Material.WATER || block.getType() == Material.STATIONARY_WATER)) {
      clicker.sendMessage(translatable("moderation.defuse.water", NamedTextColor.RED));
      return;
    }

    // check owner
    MatchPlayer owner = this.mm.getPlayer(Trackers.getOwner(entity));
    if (owner == null
        || (owner != clicker && owner.getParty() == clicker.getParty())) { // cannot defuse own TNT
      // defuse TNT
      entity.remove();
      if (owner != null) {
        this.notifyDefuse(
            clicker,
            entity,
            ChatColor.RED
                + TextTranslations.translate(
                    "moderation.defuse.player",
                    clicker.getBukkit(),
                    owner.getBukkit().getDisplayName(clicker.getBukkit()) + ChatColor.RED));

        ChatDispatcher.broadcastAdminChatMessage(
            translatable(
                "moderation.defuse.alert.player",
                NamedTextColor.GRAY,
                clicker.getName(NameStyle.FANCY),
                owner.getName(NameStyle.FANCY),
                MinecraftComponent.entity(entity.getType()).color(NamedTextColor.DARK_RED)),
            clicker.getMatch());
      } else {
        this.notifyDefuse(
            clicker,
            entity,
            ChatColor.RED
                + TextTranslations.translate("moderation.defuse.world", clicker.getBukkit()));

        ChatDispatcher.broadcastAdminChatMessage(
            translatable(
                "moderation.defuse.alert.world",
                NamedTextColor.GRAY,
                clicker.getName(NameStyle.FANCY),
                MinecraftComponent.entity(entity.getType()).color(NamedTextColor.DARK_RED)),
            clicker.getMatch());
      }
    }
  }

  private void notifyDefuse(MatchPlayer clicker, Entity entity, String message) {
    clicker.sendMessage(text(message));
    clicker.getMatch().playSound(DEFUSE_SOUND);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void checkDefuse(final PlayerInteractEvent event) {
    ItemStack hand = event.getPlayer().getItemInHand();
    if (hand == null || hand.getType() != DEFUSE_ITEM) return;

    MatchPlayer clicker = this.mm.getPlayer(event.getPlayer());
    if (clicker != null
        && clicker.isObserving()
        && clicker.getBukkit().hasPermission(Permissions.DEFUSE)) {
      if (event.getAction() == Action.RIGHT_CLICK_AIR) {
        this.obsTntDefuse(clicker, event.getPlayer().getLocation());
      } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
        this.obsTntDefuse(clicker, event.getClickedBlock().getLocation());
      }
    }
  }

  private void obsTntDefuse(MatchPlayer player, Location loc) {
    List<ParticipantState> owners = this.removeTnt(loc, 5.0);
    if (owners != null && !owners.isEmpty()) {
      player.sendMessage(
          translatable(
              "moderation.defuse.player",
              TextFormatter.nameList(owners, NameStyle.COLOR, NamedTextColor.WHITE)));
    }
  }

  // Original code borrowed from WorldEdit
  private List<ParticipantState> removeTnt(Location origin, double radius) {
    if (radius <= 0) return null;

    List<ParticipantState> owners = new ArrayList<>();
    double radiusSq = radius * radius;
    for (Entity ent : origin.getWorld().getEntities()) {
      if (origin.distanceSquared(ent.getLocation()) > radiusSq) continue;

      if (ent instanceof TNTPrimed) {
        ParticipantState player = Trackers.getOwner(ent);

        if (player != null) {
          owners.add(player);
        }
        ent.remove();
      }
    }

    List<ParticipantState> uniqueOwners = new ArrayList<>();
    for (ParticipantState player : owners) {
      if (!uniqueOwners.contains(player)) {
        uniqueOwners.add(player);
      }
    }
    return uniqueOwners;
  }

  @EventHandler
  public void giveKit(final ObserverKitApplyEvent event) {
    if (event.getPlayer().getParty() == null) return;
    if (!event.getPlayer().getParty().isObserving()
        || !event.getPlayer().getBukkit().hasPermission(Permissions.DEFUSE)) return;

    ItemStack shears = new ItemStack(DEFUSE_ITEM);

    // TODO: Update information if locale changes
    ItemMeta meta = shears.getItemMeta();
    meta.setDisplayName(
        ChatColor.RED
            + ChatColor.BOLD.toString()
            + TextTranslations.translate(
                "moderation.defuse.displayName", event.getPlayer().getBukkit()));
    meta.setLore(
        Collections.singletonList(
            ChatColor.GRAY
                + TextTranslations.translate(
                    "moderation.defuse.tooltip", event.getPlayer().getBukkit())));
    shears.setItemMeta(meta);

    event.getPlayer().getBukkit().getInventory().setItem(DEFUSE_SLOT, shears);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void cloneCraftingWindow(final PlayerInteractEvent event) {
    if (!event.isCancelled()
        && event.getAction() == Action.RIGHT_CLICK_BLOCK
        && event.getPlayer().getOpenInventory().getType()
            == InventoryType.CRAFTING /* nothing open */) {
      Block block = event.getClickedBlock();
      if (block != null
          && block.getType() == Material.WORKBENCH
          && !event.getPlayer().isSneaking()) {
        // create the window ourself
        event.setCancelled(true);
        event.getPlayer().openWorkbench(null, true); // doesn't check reachable
      }
    }
  }
}
