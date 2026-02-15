package tc.oc.pgm.modules;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;

@ListenerScope(MatchScope.LOADED)
public class WeatherMatchModule implements MatchModule, Listener {

  public enum WeatherType {
    CLEAR,
    RAIN,
    THUNDER
  }

  private final Match match;
  private boolean shouldChange = false;

  public WeatherMatchModule(Match match) {
    this.match = match;
  }

  public void setWeather(WeatherMatchModule.WeatherType state) {
    shouldChange = true;
    World world = match.getWorld();
    switch (state) {
      case WeatherType.CLEAR -> world.setStorm(false);
      case WeatherType.RAIN -> {
        world.setStorm(true);
        world.setThundering(false);
        world.setThunderDuration(0);
      }
      case WeatherType.THUNDER -> {
        world.setStorm(true);
        world.setThundering(true);
      }
    }
    shouldChange = false;
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onWeatherChange(final WeatherChangeEvent event) {
    if (match.getWorld() == event.getWorld() && !shouldChange) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onThunderChange(ThunderChangeEvent event) {
    if (match.getWorld() == event.getWorld() && !shouldChange) {
      event.setCancelled(true);
    }
  }
}
