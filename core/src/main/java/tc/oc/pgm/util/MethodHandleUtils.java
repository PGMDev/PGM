package tc.oc.pgm.util;

import com.google.common.collect.ImmutableList;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
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

  private static final List<HandleFinder> FILTERABLE_GETTERS = ImmutableList.of(
      new HandleFinder(MatchPlayer.class, "getPlayer"),
      new HandleFinder(Party.class, "getParty"),
      new HandleFinder(Match.class, "getMatch"),
      new HandleFinder(Player.class, "getPlayer", "getActor"),
      new HandleFinder(LivingEntity.class, "getEntity", "getActor"),
      new HandleFinder(Entity.class, "getEntity", "getActor"),
      new HandleFinder(HumanEntity.class, "getWhoClicked"));

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

  private record HandleFinder(MethodType type, String... names) {
    private HandleFinder(Class<?> type, String... names) {
      this(MethodType.methodType(type), names);
    }

    private @Nullable MethodHandle find(Class<?> clazz) {
      for (String name : names) {
        try {
          return LOOKUP.findVirtual(clazz, name, this.type);
        } catch (NoSuchMethodException | IllegalAccessException e) {
          // No-Op
        }
      }
      return null;
    }
  }
}
