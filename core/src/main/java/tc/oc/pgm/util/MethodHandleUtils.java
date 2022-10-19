package tc.oc.pgm.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;

public final class MethodHandleUtils {

  private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
  private static final Map<Class<? extends Event>, MethodHandle> cachedHandles = new HashMap<>();

  private static final String[] filterableGetterNames =
      new String[] {"getPlayer", "getParty", "getMatch"};
  private static final MethodType[] filterableMethodTypes =
      new MethodType[] {
        MethodType.methodType(MatchPlayer.class),
        MethodType.methodType(Party.class),
        MethodType.methodType(Match.class)
      };

  // #getActor is mostly for SportPaper (PlayerAction).
  // #getPlayer covers events which does not provide Entity
  // as the lower boundary
  private static final String[] entityGetterNames =
      new String[] {"getPlayer", "getActor", "getActor"};
  private static final MethodType playerMethodType = MethodType.methodType(Player.class);
  private static final MethodType[] entityMethodTypes =
      new MethodType[] {playerMethodType, playerMethodType, MethodType.methodType(Entity.class)};

  public static MethodHandle getHandle(Class<? extends Event> event)
      throws NoSuchMethodException, IllegalAccessException {
    MethodHandle handle = cachedHandles.get(event);

    if (handle == null) {
      createExtractingMethodHandle(event);

      handle = cachedHandles.get(event);
      if (handle == null)
        throw new IllegalStateException(
            "Newly created MethodHandle for " + event + "not found in the cache");
    }
    return handle;
  }

  private static void createExtractingMethodHandle(Class<? extends Event> event)
      throws NoSuchMethodException, IllegalAccessException {
    MethodHandle result = null;

    for (int i = 0; result == null && i < filterableGetterNames.length; i++) {
      result = findMethod(event, filterableGetterNames[i], filterableMethodTypes[i]);
    }

    for (int i = 0; result == null && i < entityGetterNames.length; i++) {
      result = findMethod(event, entityGetterNames[i], entityMethodTypes[i]);
    }

    if (result == null)
      throw new NoSuchMethodException(
          "No method to extract a Filterable or Entity(Player) found on " + event);

    cachedHandles.put(event, result);
  }

  private static @Nullable MethodHandle findMethod(Class<?> clazz, String name, MethodType type)
      throws IllegalAccessException {
    try {
      return lookup.findVirtual(clazz, name, type);
    } catch (NoSuchMethodException e) {
      // No-Op
    }
    return null;
  }
}
