package tc.oc.pgm.events;

import lombok.Getter;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchEvent;

@Getter
public class FeatureChangeEvent extends MatchEvent {
  private final Feature<?> feature;

  public FeatureChangeEvent(Match match, Feature<?> feature) {
    super(match);
    this.feature = feature;
  }

  private static final HandlerList handlers = new HandlerList();

  public static HandlerList getHandlerList() {
    return handlers;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }
}
