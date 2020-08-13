package tc.oc.pgm.flag;

import static java.util.stream.IntStream.range;

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
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
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerJoinPartyEvent;
import tc.oc.pgm.events.PlayerLeaveMatchEvent;
import tc.oc.pgm.flag.event.FlagStateChangeEvent;
import tc.oc.pgm.flag.state.Carried;
import tc.oc.pgm.flag.state.Spawned;
import tc.oc.pgm.util.inventory.ItemBuilder;
import tc.oc.pgm.util.nms.NMSHacks;

@ListenerScope(MatchScope.LOADED)
public class LegacyFlagBeamMatchModule implements MatchModule, Listener {

  private static final int UPDATE_DELAY = 0;
  private static final int UPDATE_FREQUENCY = 50;

  private UpdateTask task;
  private final Match match;
  private final Map<MatchPlayer, Map<Flag, Beam>> beams;

  public LegacyFlagBeamMatchModule(Match match) {
    this.match = match;
    this.beams = new HashMap<>();
  }

  protected Stream<Flag> flags() {
    FlagMatchModule module = match.getModule(FlagMatchModule.class);
    return module == null ? Stream.empty() : module.getFlags().stream();
  }

  protected void retrackFlag(Flag flag) {
    match.getParticipants().forEach(p -> retrackFlag(flag, p));
  }

  protected void retrackFlag(Flag flag, MatchPlayer player) {
    untrackFlag(flag, player);
    trackFlag(flag, player);
  }

  protected void trackFlag(Flag flag) {
    match.getParticipants().forEach(p -> trackFlag(flag, p));
  }

  protected void trackFlag(Flag flag, MatchPlayer player) {
    Map<Flag, Beam> flags = beams.containsKey(player) ? beams.get(player) : new HashMap<>();
    if (flags.containsKey(flag) // beam duplication check
        || !flag.getDefinition().showBeam() // considers the flag definition's flag beam setting.
        || (!player.isLegacy() // version greater than 1.7 &
            && !PGM.get()
                .getConfiguration()
                .useLegacyFlagBeams())) { // we shouldn't show to >1.7 players
      return;
    }

    flags.put(flag, new Beam(flag, player.getBukkit()));

    beams.put(player, flags);
  }

  protected void untrackFlag(Flag flag) {
    match.getParticipants().forEach(p -> untrackFlag(flag, p));
  }

  protected void untrackFlag(Flag flag, MatchPlayer player) {
    if (beams.containsKey(player)) {
      Beam beam = beams.get(player).get(flag);
      if (beam != null) {
        beams.get(player).remove(flag).hide();
      }
    }
  }

  @Override
  public void enable() {
    flags().filter(flag -> flag.getState() instanceof Spawned).forEach(this::trackFlag);
    this.task = new UpdateTask();
  }

  @Override
  public void disable() {
    flags().forEach(this::untrackFlag);
    beams.clear();
    this.task.stop();
  }

  // retrackFlags when players join a party so players are able to see the wool beams
  // player join match event doesn't work at times.
  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onPlayerJoinParty(PlayerJoinPartyEvent event) {
    flags()
        .filter(flag -> flag.getState() instanceof Spawned)
        .forEach(flag -> retrackFlag(flag, event.getPlayer()));
  }

  // retrackFlags when players switch worlds
  // player join match event doesn't work at times.
  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
    try {
      flags()
          .filter(flag -> flag.getState() instanceof Spawned)
          .forEach(flag -> retrackFlag(flag, match.getParticipant(event.getPlayer())));
    } catch (NullPointerException e) {
      /* ignore */
    }
  }

  // retrackFlags when match starts to ensure all players can see flag beams
  // player join match event doesn't work at times.
  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onMatchStart(MatchStartEvent event) {
    flags().filter(flag -> flag.getState() instanceof Spawned).forEach(flag -> retrackFlag(flag));
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onPlayerJoinMatch(PlayerJoinMatchEvent event) {
    flags()
        .filter(flag -> flag.getState() instanceof Spawned)
        .forEach(flag -> trackFlag(flag, event.getPlayer()));
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onPlayerLeaveMatch(PlayerLeaveMatchEvent event) {
    flags()
        .filter(flag -> flag.getState() instanceof Spawned)
        .forEach(flag -> untrackFlag(flag, event.getPlayer()));
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onFlagStateChange(FlagStateChangeEvent event) {
    Flag flag = event.getFlag();
    untrackFlag(flag);
    if (event.getNewState() instanceof Spawned) {
      trackFlag(flag);
    }
  }

  private class UpdateTask implements Runnable {

    private final Future<?> task;

    private UpdateTask() {
      this.task =
          match
              .getExecutor(MatchScope.RUNNING)
              .scheduleAtFixedRate(this, UPDATE_DELAY, UPDATE_FREQUENCY, TimeUnit.MILLISECONDS);
    }

    public void stop() {
      this.task.cancel(true);
    }

    @Override
    public void run() {
      ImmutableList.copyOf(beams.values()).forEach(flags -> flags.forEach((f, b) -> b.update()));
    }
  }

  class Beam {

    final Flag flag;
    final Player bukkit;
    final List<NMSHacks.FakeZombie> segments;

    Beam(Flag flag, Player bukkit) {
      this.flag = flag;
      this.bukkit = bukkit;
      this.segments =
          range(0, 64) // ~100 blocks is the height which the particles appear to be reasonably
              // visible (similar amount to amount closest to the flag), we limit this to 64 blocks
              // to reduce load on the client
              .mapToObj(i -> new NMSHacks.FakeZombie(match.getWorld(), true, false))
              .collect(Collectors.toList());
      show();
    }

    Optional<Player> carrier() {
      return Optional.ofNullable(
          flag.getState() instanceof Carried
              ? ((Carried) flag.getState()).getCarrier().getBukkit()
              : null);
    }

    Optional<Location> location() {
      if (!flag.getLocation().isPresent()) {
        return Optional.empty();
      }

      Location location = flag.getLocation().get().clone();
      location.setPitch(0);
      return Optional.of(location);
    }

    ItemStack wool() {
      return new ItemBuilder()
          .material(Material.WOOL)
          .enchant(Enchantment.DURABILITY, 1)
          .color(flag.getDyeColor())
          .build();
    }

    void show() {
      if (carrier().map(carrier -> carrier.equals(bukkit)).orElse(false)) return;
      segments.forEach(
          segment -> {
            location().ifPresent(l -> segment.spawn(bukkit, l.clone()));
            segment.wear(bukkit, 4, wool()); // Head slot
          });
      range(1, segments.size())
          .forEachOrdered(
              i -> {
                segments.get(i - 1).ride(bukkit, segments.get(i).entity());
              });
      update();
    }

    void update() {
      Optional<Player> carrier = carrier();
      NMSHacks.FakeZombie base = segments.get(0);
      if (carrier.isPresent()) {
        base.mount(bukkit, carrier.get());
      } else {
        base.entity().eject();
        location().ifPresent(l -> base.teleport(bukkit, l));
      }
    }

    void hide() {
      for (int i = segments.size() - 1; i >= 0; i--) {
        segments.get(i).destroy(bukkit);
      }
    }
  }
}
