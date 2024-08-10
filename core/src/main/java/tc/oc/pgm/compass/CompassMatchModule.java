package tc.oc.pgm.compass;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.Style.style;
import static tc.oc.pgm.util.text.TextTranslations.translateLegacy;

import com.google.common.collect.ImmutableList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerResetEvent;
import tc.oc.pgm.spawns.events.ParticipantKitApplyEvent;

@ListenerScope(MatchScope.RUNNING)
public class CompassMatchModule implements MatchModule, Tickable, Listener {

  private static final long REFRESH_TICKS = 20;
  private static final long REFRESH_TICKS_UNFOCUSED = 20 * 10;
  private final Match match;
  private final Map<UUID, Long> lastRefresh;
  private final ImmutableList<CompassTarget<?>> compassTargets;
  private final OrderStrategy orderStrategy;
  private final boolean showDistance;

  public CompassMatchModule(
      Match match,
      ImmutableList<CompassTarget<?>> compassTargets,
      OrderStrategy orderStrategy,
      boolean showDistance) {
    this.match = match;
    this.lastRefresh = new ConcurrentHashMap<>();
    this.compassTargets = compassTargets;
    this.orderStrategy = orderStrategy;
    this.showDistance = showDistance;
  }

  @Override
  public synchronized void tick(Match match, Tick tick) {
    for (Map.Entry<UUID, Long> lastRefreshEntry : lastRefresh.entrySet()) {
      long ticksSince = tick.tick - lastRefreshEntry.getValue();
      if (ticksSince >= REFRESH_TICKS) {
        UUID uuid = lastRefreshEntry.getKey();
        MatchPlayer player = this.match.getPlayer(uuid);
        if (player == null || player.getInventory() == null || !player.isAlive()) {
          lastRefresh.remove(uuid);
          continue;
        }
        if (!Material.COMPASS.equals(player.getInventory().getItemInHand().getType())
            && ticksSince < REFRESH_TICKS_UNFOCUSED) {
          continue;
        }

        lastRefresh.put(player.getId(), tick.tick);

        updatePlayerCompass(player, chooseCompassTarget(player), tick.tick);
      }
    }
  }

  private void updatePlayerCompass(
      MatchPlayer player, Optional<CompassTargetResult> compassResultOption, long tick) {
    compassResultOption.ifPresent(compassTargetResult ->
        player.getBukkit().setCompassTarget(compassTargetResult.getLocation()));

    PlayerInventory inventory = player.getInventory();
    if (inventory == null) {
      return;
    }

    ItemStack itemstack = inventory.getItemInHand();
    if (itemstack == null || !Material.COMPASS.equals(itemstack.getType())) {
      return;
    }

    ItemMeta itemMeta = itemstack.getItemMeta();

    Component itemName = compassResultOption
        .map(this::buildComponent)
        .orElse(translatable("compass.tracking.unknown", style(NamedTextColor.WHITE)));

    // Append space at front & end to keep alignment,
    // Randomly color the one at the end to force a change and make name not fade away
    itemMeta.setDisplayName(" " + translateLegacy(itemName, player) + "§" + (tick % 7) + " ");
    itemstack.setItemMeta(itemMeta);

    player.getInventory().setItemInHand(itemstack);
  }

  private Component buildComponent(CompassTargetResult compassResult) {
    Component resultComponent = compassResult.getComponent();

    TextComponent.Builder builder = text()
        .append(translatable("compass.tracking", style(NamedTextColor.GRAY)))
        .append(text(": ", style(NamedTextColor.WHITE)))
        .append(resultComponent);

    if (showDistance) {
      builder
          .append(text(" "))
          .append(translatable("compass.tracking.distance", style(NamedTextColor.AQUA), text((int)
              compassResult.getDistance())));
    }

    return builder.build();
  }

  private Optional<CompassTargetResult> chooseCompassTarget(MatchPlayer player) {
    Stream<CompassTargetResult> targetStream = compassTargets.stream()
        .map(compassTarget -> compassTarget.getResult(player))
        .flatMap(Optional::stream);
    return switch (orderStrategy) {
      case DEFINITION_ORDER -> targetStream.findFirst();
      case CLOSEST -> targetStream.min(CompassTargetResult::compareTo);
    };
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerReset(PlayerResetEvent event) {
    this.lastRefresh.put(event.getPlayer().getId(), -REFRESH_TICKS);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onItemDrop(ItemSpawnEvent event) {
    ItemStack itemStack = event.getEntity().getItemStack();
    if (Material.COMPASS.equals(itemStack.getType())) {
      event.getEntity().setItemStack(new ItemStack(Material.COMPASS, itemStack.getAmount()));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onItemMove(InventoryMoveItemEvent event) {
    if (!(event.getDestination() instanceof PlayerInventory)) {
      ItemStack itemStack = event.getItem();
      if (Material.COMPASS.equals(itemStack.getType())) {
        event.setItem(new ItemStack(Material.COMPASS, itemStack.getAmount()));
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onSpawn(ParticipantKitApplyEvent event) {
    this.lastRefresh.put(event.getPlayer().getId(), -REFRESH_TICKS);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onHeldItem(PlayerItemHeldEvent event) {
    ItemStack itemInHand = event.getPlayer().getItemInHand();
    if (itemInHand != null && Material.COMPASS.equals(itemInHand.getType())) {
      this.lastRefresh.put(event.getPlayer().getUniqueId(), -REFRESH_TICKS);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onQuit(PlayerQuitEvent event) {
    this.lastRefresh.remove(event.getPlayer().getUniqueId());
  }
}
