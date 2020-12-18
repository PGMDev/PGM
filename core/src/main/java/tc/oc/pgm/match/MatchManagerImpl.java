package tc.oc.pgm.match;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Iterators;
import com.google.common.collect.Range;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.match.event.MatchUnloadEvent;
import tc.oc.pgm.api.match.factory.MatchFactory;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.ClassLogger;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextParser;

public class MatchManagerImpl implements MatchManager, Listener {

  private final Logger logger;
  private final Map<String, Match> matchById;
  private final Map<String, Match> matchByWorld;

  // If true, all non-match worlds are forcibly unloaded.
  // This assumption works well when PGM is the only thing running on a server,
  // but when mixed with other plugins/worlds may prove to be problematic.
  private final boolean unloadNonMatches;

  // Number of seconds to wait before destroying a previously unloaded match.
  // If not specified, defaults to 1/2 of the start time.
  private final long destroyDelaySecs;

  public MatchManagerImpl(Logger logger) {
    this.logger = ClassLogger.get(checkNotNull(logger), getClass());
    this.matchById = Collections.synchronizedMap(new LinkedHashMap<>());
    this.matchByWorld = new HashMap<>();

    final Config config = PGM.get().getConfiguration();
    this.unloadNonMatches =
        config.getExperiments().getOrDefault("unload-non-match-worlds", "false").equals("true");

    long delaySecs = (config.getStartTime().getSeconds() + 1) / 2;
    try {
      delaySecs =
          TextParser.parseInteger(
              config.getExperiments().getOrDefault("match-destroy-seconds", "").toString(),
              Range.atLeast(0));
    } catch (TextException e) {
      // No-op, since this is experimental
    }
    this.destroyDelaySecs = delaySecs;
  }

  @EventHandler
  public void onMatchLoad(MatchLoadEvent event) {
    final Match match = event.getMatch();

    matchById.put(checkNotNull(match).getId(), match);
    matchByWorld.put(checkNotNull(match.getWorld()).getName(), match);

    logger.info("Loaded match-" + match.getId() + " (" + match.getMap().getId() + ")");

    if (unloadNonMatches) {
      for (World world : PGM.get().getServer().getWorlds()) {
        onNonMatchUnload(world);
      }
    }
  }

  @EventHandler
  public void onMatchUnload(MatchUnloadEvent event) {
    final Match match = event.getMatch();

    matchById.remove(checkNotNull(match).getId());
    matchByWorld.remove(checkNotNull(match.getWorld()).getName());

    PGM.get()
        .getAsyncExecutor()
        .schedule(
            () -> {
              match.destroy();
              logger.info("Unloaded match-" + match.getId() + " (" + match.getMap().getId() + ")");
            },
            destroyDelaySecs,
            TimeUnit.SECONDS);
  }

  // TODO: Do not reference craft classes and move to NMSHacks
  private void onNonMatchUnload(World world) {
    final String name = world.getName();
    if (name.startsWith("match")) return;

    try {
      final Field server = CraftWorld.class.getDeclaredField("world");
      server.setAccessible(true);

      final Field dimension = WorldServer.class.getDeclaredField("dimension");
      dimension.setAccessible(true);

      final Field modifiers = Field.class.getDeclaredField("modifiers");
      modifiers.setAccessible(true);
      modifiers.setInt(dimension, dimension.getModifiers() & ~Modifier.FINAL);

      dimension.set(server.get(world), 11);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      // No-op, newer version of Java have disabled modifying final fields
    }

    if (PGM.get().getServer().unloadWorld(name, false)) {
      logger.info("Unloaded non-match " + name);
    }
  }

  @Override
  public MatchFactory createMatch(@Nullable String mapId) {
    // FIXME: "infinite" retry if a Match fails to load
    if (mapId == null) mapId = PGM.get().getMapOrder().popNextMap().getId();
    return new MatchFactoryImpl(mapId);
  }

  @Override
  public Match getMatch(@Nullable World world) {
    return matchByWorld.get(world == null ? null : world.getName());
  }

  @Override
  public Iterator<Match> getMatches() {
    return Iterators.unmodifiableIterator(matchById.values().iterator());
  }

  @Override
  public MatchPlayer getPlayer(@Nullable Player bukkit) {
    if (bukkit == null) return null;
    final Match match = getMatch(bukkit.getWorld());
    if (match == null) return null;
    return match.getPlayer(bukkit);
  }
}
