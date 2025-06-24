package tc.oc.pgm.fireworks;

import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.Assert.assertTrue;
import static tc.oc.pgm.util.bukkit.BukkitUtils.parse;

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
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
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
import tc.oc.pgm.goals.ShowOption;
import tc.oc.pgm.regions.Bounds;
import tc.oc.pgm.util.block.BlockVectors;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.material.Materials;
import tc.oc.pgm.wool.PlayerWoolPlaceEvent;

@ListenerScope(MatchScope.LOADED)
public class FireworkMatchModule implements MatchModule, Listener {

  private final Match match;

  private static final int ROCKET_COUNT = 5; // Maximum rockets to launch at once, one per player
  private static final int INITIAL_DELAY = 2; // Seconds before starting to launch rockets
  private static final int FREQUENCY = 2; // Seconds between rocket launches
  private static final int ITERATION_COUNT = 5; // Amount of times rockets are launched
  private static final int ROCKET_POWER =
      2; // Power applied to rockets (how high they go), 1 = low, 2 = medium, 3 = high

  private static final EntityType FIREWORK_ENTITY =
      parse(EntityType::valueOf, "FIREWORK", "FIREWORK_ROCKET");

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
      this.colors = winners.stream()
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

          FireworkEffect effect = FireworkEffect.builder()
              .with(type)
              .withFlicker()
              .withColor(this.colors)
              .withFade(Color.BLACK)
              .build();

          spawnFirework(player.getLocation(), effect, ROCKET_POWER);
        }
      }
      this.iterations++;
    }
  }

  private static final int WOOL_PLACE_COUNT = 6;
  private static final double WOOL_PLACE_RADIUS = 2;

  @EventHandler(priority = EventPriority.MONITOR)
  public void onWoolPlace(final PlayerWoolPlaceEvent event) {
    if (PGM.get().getConfiguration().showFireworks()
        && event.getWool().hasShowOption(ShowOption.SHOW_EFFECTS)) {
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
    if (PGM.get().getConfiguration().showFireworks()
        && event.getCore().hasShowOption(ShowOption.SHOW_EFFECTS)) {
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
    if (PGM.get().getConfiguration().showFireworks()
        && event.getDestroyable().hasShowOption(ShowOption.SHOW_EFFECTS)) {
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
        && event.getControlPoint().hasShowOption(ShowOption.SHOW_EFFECTS)
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
    if (PGM.get().getConfiguration().showFireworks()
        && event.getGoal().hasShowOption(ShowOption.SHOW_EFFECTS)) {
      this.spawnFireworkDisplay(
          event.getMatch().getWorld(),
          event.getNet().getRegion(),
          event.getGoal().getDyeColor().getColor(),
          FLAG_CAPTURE_COUNT,
          FLAG_CAPTURE_RADIUS,
          ROCKET_POWER);
    }
  }

  private static final String FIREWORK_METADATA = "pgm-custom-firework";

  @EventHandler
  public void onPlayerDamage(EntityDamageByEntityEvent event) {
    if (event.getDamager().hasMetadata(FIREWORK_METADATA)) event.setCancelled(true);
  }

  public void spawnFireworkDisplay(
      Location center, Color color, int count, double radius, int power) {
    if (Double.isInfinite(radius)) return;
    FireworkEffect effect = FireworkEffect.builder()
        .with(Type.BURST)
        .withFlicker()
        .withColor(color)
        .withFade(Color.BLACK)
        .build();

    for (int i = 0; i < count; i++) {
      double angle = 2 * Math.PI / count * i;
      double dx = radius * Math.cos(angle);
      double dz = radius * Math.sin(angle);
      Location loc = getOpenSpaceAbove(center.clone().add(dx, 0, dz));

      if (loc != null) spawnFirework(loc, effect, power);
    }
  }

  public void spawnFireworkDisplay(
      World world, Region region, Color color, int count, double radiusMultiplier, int power) {
    final Bounds bound = region.getBounds();
    final double radius = bound.getSize().setY(0).multiply(0.5).length();
    final Location center = bound.getCenterPoint().toLocation(world);
    this.spawnFireworkDisplay(center, color, count, radiusMultiplier * radius, power);
  }

  public static Firework spawnFirework(Location location, FireworkEffect effect, int power) {
    assertNotNull(location, "location");
    assertNotNull(effect, "firework effect");
    assertTrue(power >= 0, "power must be positive");

    FireworkMeta meta = (FireworkMeta) Bukkit.getItemFactory().getItemMeta(Materials.FIREWORK);
    meta.setPower(power);
    meta.addEffect(effect);

    Firework firework = (Firework) location.getWorld().spawnEntity(location, FIREWORK_ENTITY);
    firework.setFireworkMeta(meta);
    firework.setMetadata(FIREWORK_METADATA, new FixedMetadataValue(PGM.get(), true));

    return firework;
  }

  public static Location getOpenSpaceAbove(Location location) {
    assertNotNull(location, "location");

    Block block = location.getBlock();

    int maxSearch = 25;
    while (block != null && block.getType().isOccluding() && maxSearch-- > 0)
      block = block.getRelative(BlockFace.UP);
    if (maxSearch < 0 || block == null) return null;

    maxSearch = 25;
    while (block != null && block.getType() != Material.AIR && maxSearch-- > 0)
      block = block.getRelative(BlockFace.UP);
    if (maxSearch < 0 || block == null) return null;

    // Returning original location ensure x & z are not affected.
    location.setY(block.getY() + 0.5d);
    return location;
  }
}
