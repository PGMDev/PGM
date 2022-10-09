package tc.oc.pgm.util.bukkit;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.AuthorNagException;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;

public interface Events {

  static void callEvent(@NotNull Event event, @NotNull EventPriority priority) {
    assertNotNull(event, "event");
    assertNotNull(priority, "priority");

    // CraftBukkit does not expose the event calling logic in a flexible
    // enough way, so we have to do a bit of copy and paste.
    //
    // The following is copied from SimplePluginManager#fireEvent with
    // modifications
    for (RegisteredListener registration : event.getHandlers().getRegisteredListeners()) {
      if (!registration.getPlugin().isEnabled()) {
        continue;
      }

      // skip over registrations that are not in the correct priority
      if (registration.getPriority() != priority) {
        continue;
      }

      try {
        registration.callEvent(event);
      } catch (AuthorNagException ex) {
        Plugin plugin = registration.getPlugin();

        if (plugin.isNaggable()) {
          plugin.setNaggable(false);

          Bukkit.getLogger()
              .log(
                  Level.SEVERE,
                  String.format(
                      "Nag author(s): '%s' of '%s' about the following: %s",
                      plugin.getDescription().getAuthors(),
                      plugin.getDescription().getFullName(),
                      ex.getMessage()));
        }
      } catch (Throwable ex) {
        Bukkit.getLogger()
            .log(
                Level.SEVERE,
                "Could not pass event "
                    + event.getEventName()
                    + " to "
                    + registration.getPlugin().getDescription().getFullName(),
                ex);
      }
    }
  }

  static HandlerList getEventListeners(Class<? extends Event> type) {
    try {
      Method method = getRegistrationClass(type).getDeclaredMethod("getHandlerList");
      method.setAccessible(true);
      return (HandlerList) method.invoke(null);
    } catch (Exception e) {
      throw new IllegalPluginAccessException(e.toString());
    }
  }

  static Class<? extends Event> getRegistrationClass(Class<? extends Event> clazz) {
    try {
      clazz.getDeclaredMethod("getHandlerList");
      return clazz;
    } catch (NoSuchMethodException e) {
      if (clazz.getSuperclass() != null
          && !clazz.getSuperclass().equals(Event.class)
          && Event.class.isAssignableFrom(clazz.getSuperclass())) {
        return getRegistrationClass(clazz.getSuperclass().asSubclass(Event.class));
      } else {
        throw new IllegalPluginAccessException(
            "Unable to find handler list for event " + clazz.getName());
      }
    }
  }

  static boolean isCancelled(Event event) {
    return event instanceof Cancellable && ((Cancellable) event).isCancelled();
  }
}
