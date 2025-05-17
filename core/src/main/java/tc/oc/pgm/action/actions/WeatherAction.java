package tc.oc.pgm.action.actions;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.modules.WeatherMatchModule;

public class WeatherAction extends AbstractAction<Match> {

  private final WeatherMatchModule.WeatherType state;

  public WeatherAction(WeatherMatchModule.WeatherType state) {
    super(Match.class);
    this.state = state;
  }

  public void trigger(Match match) {
    match.getModule(WeatherMatchModule.class).setWeather(state);
  }
}
