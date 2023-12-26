package tc.oc.pgm.util;

import com.google.common.collect.ImmutableList;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;

public final class MethodHandleUtils {

  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
  private static final Map<Class<? extends Event>, MethodHandle> CACHED_HANDLES = new HashMap<>();

  private static final List<HandleFinder> FILTERABLE_GETTERS =
      ImmutableList.of(
          new HandleFinder(MatchPlayer.class, "getPlayer"),
          new HandleFinder(Party.class, "getParty"),
          new HandleFinder(Match.class, "getMatch"),
          new HandleFinder(Player.class, "getPlayer"),
          new HandleFinder(Player.class, "getActor"),
          new HandleFinder(Entity.class, "getActor"),
          new HandleFinder(LivingEntity.class, "getEntity"));

  public static MethodHandle getHandle(Class<? extends Event> event) throws NoSuchMethodException {
    MethodHandle handle = CACHED_HANDLES.computeIfAbsent(event, MethodHandleUtils::findHandle);

    if (handle == null)
      throw new NoSuchMethodException(
          "No method to extract a Filterable or Player found on " + event);

    return handle;
  }

  private static MethodHandle findHandle(Class<? extends Event> event) {
    for (HandleFinder finder : FILTERABLE_GETTERS) {
      MethodHandle handle = finder.find(event);
      if (handle != null) return handle;
    }
    return null;
  }

  private static class HandleFinder {
    private final MethodType type;
    private final String name;

    private HandleFinder(Class<?> returnType, String name) {
      this.type = MethodType.methodType(returnType);
      this.name = name;
    }

    private @Nullable MethodHandle find(Class<?> clazz) {
      try {
        return LOOKUP.findVirtual(clazz, this.name, this.type);
      } catch (NoSuchMethodException | IllegalAccessException e) {
        // No-Op
      }
      return null;
    }
  }
}
