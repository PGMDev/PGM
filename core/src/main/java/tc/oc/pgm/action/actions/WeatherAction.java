package tc.oc.pgm.action.actions;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.modules.WeatherMatchModule;
import tc.oc.pgm.modules.WeatherMatchModule.WeatherType;

public class WeatherAction extends AbstractAction<Match> {
  private static final Map<WeatherType, WeatherAction> INSTANCES;

  static {
    var values = new EnumMap<WeatherType, WeatherAction>(WeatherType.class);
    for (WeatherType component : WeatherType.values()) {
      values.put(component, new WeatherAction(component));
    }
    INSTANCES = Collections.unmodifiableMap(values);
  }

  private final WeatherType state;

  private WeatherAction(WeatherType state) {
    super(Match.class);
    this.state = state;
  }

  public static WeatherAction of(WeatherType state) {
    return INSTANCES.get(state);
  }

  public void trigger(Match match) {
    match.moduleRequire(WeatherMatchModule.class).setWeather(state);
  }
}
