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
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.events.PlayerResetEvent;
import tc.oc.pgm.spawns.events.ParticipantKitApplyEvent;

public class CompassMatchModule implements MatchModule, Tickable, Listener {

  private static final long REFRESH_TICKS = 20 * 2;
  private final Match match;
  private final Map<UUID, Long> lastRefresh;
  private final ImmutableList<CompassTarget> compassTargets;
  private final OrderStrategy orderStrategy;
  private final boolean showDistance;

  public CompassMatchModule(
      Match match,
      ImmutableList<CompassTarget> compassTargets,
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
      if (tick.tick - lastRefreshEntry.getValue() >= REFRESH_TICKS) {
        refreshPlayer(lastRefreshEntry.getKey(), tick.tick);
      }
    }
  }

  private void refreshPlayer(UUID uuid, long tick) {
    MatchPlayer player = match.getPlayer(uuid);
    if (player == null || !player.isAlive()) {
      lastRefresh.remove(uuid);
      return;
    } else {
      lastRefresh.put(uuid, tick);
    }

    updatePlayerCompass(player, chooseCompassTarget(player));
  }

  private void updatePlayerCompass(
      MatchPlayer player, Optional<CompassTargetResult> compassResult) {
    compassResult.ifPresent(
        compassTargetResult ->
            player.getBukkit().setCompassTarget(compassTargetResult.getLocation()));

    PlayerInventory inventory = player.getInventory();
    if (inventory == null) {
      return;
    }
    ItemStack[] contents = inventory.getContents();
    if (contents == null) {
      return;
    }

    for (ItemStack content : contents) {
      if (content != null && Material.COMPASS.equals(content.getType())) {
        ItemMeta itemMeta = content.getItemMeta();

        Component itemNameComponent;
        if (compassResult.isPresent()) {
          Component resultComponent = compassResult.get().getComponent();

          TextComponent.Builder builder =
              text()
                  .append(
                      translatable(
                          "compass.tracking", style(NamedTextColor.GRAY, TextDecoration.BOLD)))
                  .append(text(": ", style(NamedTextColor.WHITE, TextDecoration.BOLD)))
                  .append(resultComponent);

          if (showDistance) {
            builder
                .append(text(" "))
                .append(
                    translatable(
                        "compass.tracking.distance",
                        style(NamedTextColor.AQUA, TextDecoration.BOLD),
                        text((int) compassResult.get().getDistance())));
          }

          itemNameComponent = builder.build();
        } else {
          itemNameComponent =
              translatable(
                  "compass.tracking.unknown", style(NamedTextColor.WHITE, TextDecoration.BOLD));
        }

        itemMeta.setDisplayName(translateLegacy(itemNameComponent, player));
        content.setItemMeta(itemMeta);
      }
    }
    inventory.setContents(contents);
  }

  private Optional<CompassTargetResult> chooseCompassTarget(MatchPlayer player) {
    Optional<CompassTargetResult> result = Optional.empty();
    Stream<CompassTargetResult> targetStream =
        compassTargets.stream()
            .map((compassTarget -> compassTarget.getResult(match, player)))
            .filter(Optional::isPresent)
            .map(Optional::get);
    switch (orderStrategy) {
      case FIRST_DEFINED:
        result = targetStream.findFirst();
        break;
      case CLOSEST:
        result = targetStream.min(CompassTargetResult::compareTo);
        break;
    }
    return result;
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
}
