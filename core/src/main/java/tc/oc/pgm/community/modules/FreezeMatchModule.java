package tc.oc.pgm.community.modules;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.ObserverInteractEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.listeners.ChatDispatcher;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;
import tc.oc.pgm.util.UsernameFormatUtils;
import tc.oc.pgm.util.bukkit.OnlinePlayerMapAdapter;
import tc.oc.pgm.util.chat.Sound;
import tc.oc.pgm.util.component.Components;
import tc.oc.pgm.util.component.types.PersonalizedText;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextTranslations;

@ListenerScope(MatchScope.LOADED)
public class FreezeMatchModule implements MatchModule, Listener {

  private static final Material TOOL_MATERIAL = Material.ICE;
  private static final int TOOL_SLOT_NUM = 6;

  private final Match match;
  private final Freeze freeze;

  public FreezeMatchModule(Match match) {
    this.match = match;
    this.freeze = new Freeze();
  }

  public void setFrozen(
      @Nullable CommandSender freezer, MatchPlayer freezee, boolean frozen, boolean silent) {
    freeze.setFrozen(freezer, freezee, frozen, silent);
  }

  public List<MatchPlayer> getFrozenPlayers() {
    return freeze.frozenPlayers.values().stream().collect(Collectors.toList());
  }

  public int getOfflineFrozenCount() {
    return freeze.getOfflineCount();
  }

  public String getOfflineFrozenNames() {
    return freeze.getOfflineFrozenNames();
  }

  public boolean isFrozen(MatchPlayer player) {
    return freeze.isFrozen(player.getBukkit());
  }

  private ItemStack getFreezeTool(CommandSender viewer) {
    ItemStack stack = new ItemStack(TOOL_MATERIAL);
    ItemMeta meta = stack.getItemMeta();
    meta.setDisplayName(
        ChatColor.WHITE
            + ChatColor.BOLD.toString()
            + TextTranslations.translate("moderation.freeze.itemName", viewer));
    meta.addItemFlags(ItemFlag.values());
    meta.setLore(
        Collections.singletonList(
            ChatColor.GRAY
                + TextTranslations.translate("moderation.freeze.itemDescription", viewer)));
    stack.setItemMeta(meta);
    return stack;
  }

  @EventHandler
  public void giveKit(final ObserverKitApplyEvent event) {
    Player player = event.getPlayer().getBukkit();
    if (player.hasPermission(Permissions.STAFF)) {
      player.getInventory().setItem(TOOL_SLOT_NUM, getFreezeTool(player));
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onObserverToolFreeze(final ObserverInteractEvent event) {
    if (event.getPlayer().isDead()) return;

    if (freeze.isFrozen(event.getPlayer().getBukkit())) {
      event.setCancelled(true);
    } else {
      if (event.getClickedItem() != null
          && event.getClickedItem().getType() == TOOL_MATERIAL
          && event.getPlayer().getBukkit().hasPermission(Permissions.STAFF)
          && event.getClickedPlayer() != null) {

        event.setCancelled(true);

        freeze.setFrozen(
            event.getPlayer().getBukkit(),
            event.getClickedPlayer(),
            !freeze.isFrozen(event.getClickedEntity()),
            event.getPlayer().isVanished());
      }
    }
  }

  private static final List<String> ALLOWED_CMDS = Lists.newArrayList("/msg", "/r", "/tell");

  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerCommand(final PlayerCommandPreprocessEvent event) {
    if (freeze.isFrozen(event.getPlayer()) && !event.getPlayer().hasPermission(Permissions.STAFF)) {
      boolean allow =
          ALLOWED_CMDS.stream()
              .filter(cmd -> event.getMessage().startsWith(cmd))
              .findAny()
              .isPresent();

      if (!allow) {
        // Don't allow commands except for those related to chat.
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onPlayerMove(final PlayerMoveEvent event) {
    if (freeze.isFrozen(event.getPlayer())) {
      Location old = event.getFrom();
      old.setPitch(event.getTo().getPitch());
      old.setYaw(event.getTo().getYaw());
      event.setTo(old);
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onVehicleMove(final VehicleMoveEvent event) {
    if (!event.getVehicle().isEmpty() && freeze.isFrozen(event.getVehicle().getPassenger())) {
      event.getVehicle().setVelocity(new Vector(0, 0, 0));
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onVehicleEnter(final VehicleEnterEvent event) {
    if (freeze.isFrozen(event.getEntered())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onVehicleExit(final VehicleExitEvent event) {
    if (freeze.isFrozen(event.getExited())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onVehicleDamage(final VehicleDamageEvent event) {
    if (freeze.isFrozen(event.getAttacker())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockBreak(final BlockBreakEvent event) {
    if (freeze.isFrozen(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockPlace(final BlockPlaceEvent event) {
    if (freeze.isFrozen(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBucketFill(final PlayerBucketFillEvent event) {
    if (freeze.isFrozen(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBucketEmpty(final PlayerBucketEmptyEvent event) {
    if (freeze.isFrozen(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW) // ignoreCancelled doesn't seem to work well here
  public void onPlayerInteract(final PlayerInteractEvent event) {
    if (freeze.isFrozen(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onInventoryClick(final InventoryClickEvent event) {
    if (event.getWhoClicked() instanceof Player) {
      if (freeze.isFrozen(event.getWhoClicked())) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onPlayerDropItem(final PlayerDropItemEvent event) {
    if (freeze.isFrozen(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityDamge(final EntityDamageByEntityEvent event) {
    if (freeze.isFrozen(event.getDamager())) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    if (freeze.isCached(event.getPlayer().getUniqueId())) {
      freeze.removeCachedPlayer(event.getPlayer().getUniqueId());
      freeze.setFrozen(Bukkit.getConsoleSender(), match.getPlayer(event.getPlayer()), true, true);
    }
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    if (freeze.isFrozen(event.getPlayer())) {
      freeze.cachePlayer(event.getPlayer());
    }
  }

  public class Freeze {

    private final Sound FREEZE_SOUND = new Sound("mob.enderdragon.growl", 1f, 1f);
    private final Sound THAW_SOUND = new Sound("mob.enderdragon.growl", 1f, 2f);

    private final OnlinePlayerMapAdapter<MatchPlayer> frozenPlayers;
    private final Cache<UUID, String> offlineFrozenCache =
        CacheBuilder.newBuilder().expireAfterWrite(3, TimeUnit.MINUTES).build();

    public Freeze() {
      this.frozenPlayers = new OnlinePlayerMapAdapter<MatchPlayer>(PGM.get());
    }

    private void cachePlayer(Player player) {
      this.offlineFrozenCache.put(player.getUniqueId(), player.getName());
    }

    private void removeCachedPlayer(UUID uuid) {
      this.offlineFrozenCache.invalidate(uuid);
    }

    private boolean isCached(UUID uuid) {
      return this.offlineFrozenCache.getIfPresent(uuid) != null;
    }

    private String getOfflineFrozenNames() {
      return offlineFrozenCache.asMap().values().stream()
          .map(name -> ChatColor.DARK_AQUA + name)
          .collect(Collectors.joining(ChatColor.GRAY + ", "));
    }

    private int getOfflineCount() {
      return Math.toIntExact(offlineFrozenCache.size());
    }

    public boolean isFrozen(Entity player) {
      return player instanceof Player && frozenPlayers.containsKey(player);
    }

    public void setFrozen(
        @Nullable CommandSender freezer, MatchPlayer freezee, boolean frozen, boolean silent) {
      freezee.setFrozen(frozen);

      Component senderName = UsernameFormatUtils.formatStaffName(freezer, freezee.getMatch());

      if (frozen) {
        freeze(freezee, senderName, silent);
      } else {
        thaw(freezee, senderName, silent);
      }
    }

    private void freeze(MatchPlayer freezee, Component senderName, boolean silent) {
      frozenPlayers.put(freezee.getBukkit(), freezee);

      removeEntities(freezee.getBukkit().getLocation(), 10);

      Component freeze = TranslatableComponent.of("moderation.freeze.frozen");
      Component by = TranslatableComponent.of("misc.by").args(senderName);

      TextComponent.Builder freezeTitle = TextComponent.builder();
      freezeTitle.append(freeze);
      if (!silent) {
        freezeTitle.append(" ").append(by);
      }

      // TODO: Migrate this once showTitle is upgraded
      tc.oc.pgm.util.component.Component oldTitle =
          new PersonalizedText(
              TextTranslations.translateLegacy(
                  freezeTitle.color(TextColor.RED).build(), freezee.getBukkit()));

      freezee.showTitle(Components.blank(), oldTitle, 5, 9999, 5);
      freezee.playSound(FREEZE_SOUND);

      ChatDispatcher.broadcastAdminChatMessage(
          TranslatableComponent.of("moderation.freeze.broadcast.frozen", TextColor.GRAY)
              .args(senderName, freezee.getName(NameStyle.FANCY)),
          match);
    }

    private void thaw(MatchPlayer freezee, Component senderName, boolean silent) {
      frozenPlayers.remove(freezee.getBukkit());

      Component thawed = TranslatableComponent.of("moderation.freeze.unfrozen");
      Component by = TranslatableComponent.of("misc.by").args(senderName);

      TextComponent.Builder thawedTitle = TextComponent.builder().append(thawed);
      if (!silent) {
        thawedTitle.append(" ").append(by);
      }

      freezee.getBukkit().hideTitle();
      freezee.playSound(THAW_SOUND);
      freezee.sendMessage(thawedTitle.color(TextColor.GREEN).build());

      ChatDispatcher.broadcastAdminChatMessage(
          TranslatableComponent.of("moderation.freeze.broadcast.thaw", TextColor.GRAY)
              .args(senderName, freezee.getName(NameStyle.FANCY)),
          match);
    }

    // Borrowed from WorldEdit
    private void removeEntities(Location origin, double radius) {
      if (radius <= 0) return;

      double radiusSq = radius * radius;
      for (Entity ent : origin.getWorld().getEntities()) {
        if (origin.distanceSquared(ent.getLocation()) > radiusSq) continue;

        if (ent instanceof TNTPrimed) {
          ent.remove();
        }
      }
    }
  }
}
