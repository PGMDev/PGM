package tc.oc.pgm.flag;

import static java.util.stream.IntStream.range;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerLeaveMatchEvent;
import tc.oc.pgm.flag.event.FlagStateChangeEvent;
import tc.oc.pgm.flag.state.Carried;
import tc.oc.pgm.flag.state.Spawned;
import tc.oc.pgm.util.inventory.ItemBuilder;
import tc.oc.pgm.util.nms.NMSHacks;

@ListenerScope(MatchScope.LOADED)
public class LegacyFlagBeamMatchModule implements MatchModule, Listener {

  public static class Factory implements MatchModuleFactory<LegacyFlagBeamMatchModule> {
    @Override
    public Collection<Class<? extends MatchModule>> getSoftDependencies() {
      return ImmutableList.of(FlagMatchModule.class); // Only needs to load if Flags are loaded
    }

    @Override
    public LegacyFlagBeamMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new LegacyFlagBeamMatchModule(match);
    }
  }

  private static final int UPDATE_DELAY = 0;
  private static final int UPDATE_FREQUENCY = 50;

  private final Match match;
  private final Map<Flag, Beam> beams;

  public LegacyFlagBeamMatchModule(Match match) {
    this.match = match;
    this.beams = new HashMap<>();
    FlagMatchModule module = match.needModule(FlagMatchModule.class);
    module.getFlags().forEach(f -> beams.put(f, new Beam(f)));
  }

  private Stream<Beam> beams() {
    return beams.values().stream();
  }

  @Override
  public void enable() {
    match
        .getExecutor(MatchScope.RUNNING)
        .scheduleAtFixedRate(
            () -> beams.values().forEach(Beam::update),
            UPDATE_DELAY,
            UPDATE_FREQUENCY,
            TimeUnit.MILLISECONDS);
  }

  @Override
  public void unload() {
    beams.values().forEach(Beam::hide);
    beams.clear();
  }

  private void showLater(MatchPlayer player) {
    match
        .getExecutor(MatchScope.LOADED)
        .schedule(() -> beams().forEach(b -> b.show(player)), 50, TimeUnit.MILLISECONDS);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
    MatchPlayer player = match.getPlayer(event.getPlayer());
    if (player == null) return;

    if (event.getWorld() == match.getWorld()) showLater(player);
    else beams().forEach(beam -> beam.hide(player));
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onPlayerJoin(PlayerJoinMatchEvent event) {
    showLater(event.getPlayer());
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onPlayerLeaveMatch(PlayerLeaveMatchEvent event) {
    beams().forEach(beam -> beam.hide(event.getPlayer()));
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onFlagStateChange(FlagStateChangeEvent event) {
    Beam beam = beams.get(event.getFlag());

    boolean wasSpawned = event.getOldState() instanceof Spawned,
        isSpawned = event.getNewState() instanceof Spawned;

    if (wasSpawned && !isSpawned) beam.hide();
    else if (!wasSpawned && isSpawned) beam.show();

    // Hide beam for carrier
    if (event.getNewState() instanceof Carried)
      beam.hide(((Carried) event.getNewState()).getCarrier());

    // Show beam to old carrier
    if (event.getOldState() instanceof Carried)
      beam.show(((Carried) event.getOldState()).getCarrier());
  }

  class Beam {
    private final Flag flag;
    private final NMSHacks.FakeEntity base, legacyBase;
    private final List<NMSHacks.FakeEntity> segments;

    private final Set<MatchPlayer> viewers = new HashSet<>();

    Beam(Flag flag) {
      this.flag = flag;

      ItemStack wool = new ItemBuilder().material(Material.WOOL).color(flag.getDyeColor()).build();
      this.base = new NMSHacks.FakeArmorStand(match.getWorld(), wool);
      this.legacyBase = new NMSHacks.FakeWitherSkull(match.getWorld());
      this.segments =
          range(0, 64) // ~100 blocks is the height which the particles appear to be reasonably
              // visible (similar amount to amount closest to the flag), we limit this to 64 blocks
              // to reduce load on the client
              .mapToObj(i -> new NMSHacks.FakeArmorStand(match.getWorld(), wool))
              .collect(Collectors.toList());
    }

    Optional<MatchPlayer> carrier() {
      return flag.getState() instanceof Carried
          ? Optional.of(((Carried) flag.getState()).getCarrier())
          : Optional.empty();
    }

    Optional<Location> location() {
      if (!flag.getLocation().isPresent()) {
        return Optional.empty();
      }

      Location location = flag.getLocation().get().clone();
      location.setPitch(0f);
      location.setYaw(0f);
      return Optional.of(location);
    }

    private NMSHacks.FakeEntity base(MatchPlayer player) {
      return player.isLegacy() ? legacyBase : base;
    }

    public void show() {
      match.getPlayers().forEach(this::show);
    }

    public void show(MatchPlayer player) {
      if (!flag.getDefinition().showBeam()) return;
      if (!player.isLegacy() && !PGM.get().getConfiguration().useLegacyFlagBeams()) return;
      if (!(flag.getState() instanceof Spawned)) return;

      if (carrier().map(player::equals).orElse(false) || !viewers.add(player)) return;

      Player bukkit = player.getBukkit();
      spawn(bukkit, base(player));
      segments.forEach(segment -> spawn(bukkit, segment));
      range(1, segments.size())
          .forEachOrdered(i -> segments.get(i - 1).ride(bukkit, segments.get(i).entity()));
      base(player).ride(bukkit, segments.get(0).entity());

      update(player);
    }

    private void spawn(Player player, NMSHacks.FakeEntity entity) {
      location().ifPresent(l -> entity.spawn(player, l));
    }

    public void update() {
      viewers.forEach(this::update);
    }

    public void update(MatchPlayer player) {
      Location loc =
          carrier().map(c -> c.getBukkit().getLocation()).orElseGet(() -> location().orElse(null));
      if (loc == null) return;
      loc = loc.clone().add(0, 2.75, 0);
      if (loc.getY() < -64) loc.setY(-64);
      loc.setPitch(0f);
      loc.setYaw(0f);
      base(player).teleport(player.getBukkit(), loc);
    }

    public void hide() {
      ImmutableSet.copyOf(viewers).forEach(this::hide);
      viewers.clear();
    }

    private void hide(MatchPlayer player) {
      if (!viewers.remove(player)) return;
      Player bukkit = player.getBukkit();
      for (int i = segments.size() - 1; i >= 0; i--) segments.get(i).destroy(bukkit);
      base(player).destroy(bukkit);
    }
  }
}
