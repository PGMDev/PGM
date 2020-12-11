package tc.oc.pgm.fireworks;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.meta.FireworkMeta;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.controlpoint.events.ControllerChangeEvent;
import tc.oc.pgm.core.CoreLeakEvent;
import tc.oc.pgm.destroyable.DestroyableDestroyedEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.flag.event.FlagCaptureEvent;
import tc.oc.pgm.regions.Bounds;
import tc.oc.pgm.util.block.BlockVectors;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.wool.PlayerWoolPlaceEvent;

@ListenerScope(MatchScope.LOADED)
public class FireworkMatchModule implements MatchModule, Listener {

  private final Match match;

  private static final int ROCKET_COUNT = 5; // Maximum rockets to launch at once, one per player
  private static final int INITIAL_DELAY = 2; // Seconds before starting to launch rockets
  private static final int FREQUENCY = 2; // Seconds between rocket launches
  private static final int ITERATION_COUNT = 15; // Amount of times rockets are launched
  private static final int ROCKET_POWER =
      2; // Power applied to rockets (how high they go), 1 = low, 2 = medium, 3 = high

  public static List<FireworkEffect.Type> FIREWORK_TYPES =
      ImmutableList.<FireworkEffect.Type>builder()
          .add(Type.BALL)
          .add(Type.BALL_LARGE)
          .add(Type.BURST)
          .add(Type.STAR)
          .build();

  public FireworkMatchModule(Match match) {
    this.match = match;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchEnd(final MatchFinishEvent event) {
    if (!PGM.get().getConfiguration().showFireworks()) return;
    match
        .getExecutor(MatchScope.LOADED)
        .scheduleAtFixedRate(
            new FireworkRunner(match, event.getWinners()),
            INITIAL_DELAY,
            FREQUENCY,
            TimeUnit.SECONDS);
  }

  private static class FireworkRunner implements Runnable {
    private final Set<Color> colors;
    private final Collection<Competitor> winners;
    private int iterations = 0;
    private final Match match;

    public FireworkRunner(Match match, Collection<Competitor> winners) {
      this.match = match;
      this.winners = winners;
      this.colors =
          winners.stream()
              .map(winner -> BukkitUtils.colorOf(winner.getColor()))
              .collect(Collectors.toSet());
    }

    @Override
    public void run() {
      if (this.iterations < ITERATION_COUNT) {
        // Build this list fresh every time, because MatchPlayers can unload, but Competitors can't.
        final List<MatchPlayer> players =
            winners.stream().flatMap(c -> c.getPlayers().stream()).collect(Collectors.toList());
        Collections.shuffle(players);

        for (int i = 0; i < players.size() && i < ROCKET_COUNT; i++) {
          MatchPlayer player = players.get(i);

          Type type = FIREWORK_TYPES.get(match.getRandom().nextInt(FIREWORK_TYPES.size()));

          FireworkEffect effect =
              FireworkEffect.builder()
                  .with(type)
                  .withFlicker()
                  .withColor(this.colors)
                  .withFade(Color.BLACK)
                  .build();

          spawnFirework(player.getBukkit().getLocation(), effect, ROCKET_POWER);
        }
      }
      this.iterations++;
    }
  }

  private static final int WOOL_PLACE_COUNT = 6;
  private static final double WOOL_PLACE_RADIUS = 2;

  @EventHandler(priority = EventPriority.MONITOR)
  public void onWoolPlace(final PlayerWoolPlaceEvent event) {
    if (PGM.get().getConfiguration().showFireworks() && event.getWool().isVisible()) {
      this.spawnFireworkDisplay(
          BlockVectors.center(event.getBlock()),
          event.getWool().getDyeColor().getColor(),
          WOOL_PLACE_COUNT,
          WOOL_PLACE_RADIUS,
          ROCKET_POWER);
    }
  }

  private static final int CORE_LEAK_COUNT = 8;
  private static final double CORE_LEAK_RADIUS = 1.5;

  @EventHandler(priority = EventPriority.MONITOR)
  public void onCoreLeak(final CoreLeakEvent event) {
    if (PGM.get().getConfiguration().showFireworks() && event.getCore().isVisible()) {
      this.spawnFireworkDisplay(
          event.getMatch().getWorld(),
          event.getCore().getCasingRegion(),
          event.getCore().getColor(),
          CORE_LEAK_COUNT,
          CORE_LEAK_RADIUS,
          ROCKET_POWER);
    }
  }

  private static final int DESTROYABLE_BREAK_COUNT = 4;
  private static final double DESTROYABLE_BREAK_RADIUS = 3;

  @EventHandler(priority = EventPriority.MONITOR)
  public void onDestroyableBreak(final DestroyableDestroyedEvent event) {
    if (PGM.get().getConfiguration().showFireworks() && event.getDestroyable().isVisible()) {
      this.spawnFireworkDisplay(
          event.getMatch().getWorld(),
          event.getDestroyable().getBlockRegion(),
          event.getDestroyable().getColor(),
          DESTROYABLE_BREAK_COUNT,
          DESTROYABLE_BREAK_RADIUS,
          ROCKET_POWER);
    }
  }

  private static final int CONTROL_POINT_COUNT = 8;
  private static final double CONTROL_POINT_RADIUS = 1;

  @EventHandler(priority = EventPriority.MONITOR)
  public void onHillCapture(final ControllerChangeEvent event) {
    if (PGM.get().getConfiguration().showFireworks()
        && event.getControlPoint().isVisible()
        && event.getNewController() != null) {
      this.spawnFireworkDisplay(
          event.getMatch().getWorld(),
          event.getControlPoint().getCaptureRegion(),
          BukkitUtils.colorOf(event.getNewController().getColor()),
          CONTROL_POINT_COUNT,
          CONTROL_POINT_RADIUS,
          ROCKET_POWER);
    }
  }

  private static final int FLAG_CAPTURE_COUNT = 6;
  private static final double FLAG_CAPTURE_RADIUS = 1;

  @EventHandler(priority = EventPriority.MONITOR)
  public void onFlagCapture(final FlagCaptureEvent event) {
    if (PGM.get().getConfiguration().showFireworks() && event.getGoal().isVisible()) {
      this.spawnFireworkDisplay(
          event.getMatch().getWorld(),
          event.getNet().getRegion(),
          event.getGoal().getDyeColor().getColor(),
          FLAG_CAPTURE_COUNT,
          FLAG_CAPTURE_RADIUS,
          ROCKET_POWER);
    }
  }

  public void spawnFireworkDisplay(
      Location center, Color color, int count, double radius, int power) {
    FireworkEffect effect =
        FireworkEffect.builder()
            .with(Type.BURST)
            .withFlicker()
            .withColor(color)
            .withFade(Color.BLACK)
            .build();

    for (int i = 0; i < count; i++) {
      double angle = 2 * Math.PI / count * i;
      double dx = radius * Math.cos(angle);
      double dz = radius * Math.sin(angle);
      Location baseLocation = center.clone().add(dx, 0, dz);

      Block block = baseLocation.getBlock();
      if (block == null || !block.getType().isOccluding()) {
        spawnFirework(getOpenSpaceAbove(baseLocation), effect, power);
      }
    }
  }

  public void spawnFireworkDisplay(
      World world, Region region, Color color, int count, double radiusMultiplier, int power) {
    final Bounds bound = region.getBounds();
    final double radius = bound.getMax().subtract(bound.getMin()).multiply(0.5).length();
    final Location center = bound.getMin().getMidpoint(bound.getMax()).toLocation(world);
    this.spawnFireworkDisplay(center, color, count, radiusMultiplier * radius, power);
  }

  public static Firework spawnFirework(Location location, FireworkEffect effect, int power) {
    Preconditions.checkNotNull(location, "location");
    Preconditions.checkNotNull(effect, "firework effect");
    Preconditions.checkArgument(power >= 0, "power must be positive");

    FireworkMeta meta = (FireworkMeta) Bukkit.getItemFactory().getItemMeta(Material.FIREWORK);
    meta.setPower(power);
    meta.addEffect(effect);

    Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
    firework.setFireworkMeta(meta);

    return firework;
  }

  public static Location getOpenSpaceAbove(Location location) {
    Preconditions.checkNotNull(location, "location");

    Location result = location.clone();
    while (true) {
      Block block = result.getBlock();
      if (block == null || block.getType() == Material.AIR) break;

      result.setY(result.getY() + 1);
    }

    return result;
  }
}
