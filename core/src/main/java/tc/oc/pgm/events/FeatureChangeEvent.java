package tc.oc.pgm.events;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchEvent;
import tc.oc.pgm.features.Feature;

public class FeatureChangeEvent extends MatchEvent {
  private final Feature feature;

  public FeatureChangeEvent(Match match, Feature feature) {
    super(match);
    this.feature = feature;
  }

  public Feature getFeature() {
    return feature;
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
