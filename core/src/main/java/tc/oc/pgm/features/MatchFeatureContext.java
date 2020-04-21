package tc.oc.pgm.features;

import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.util.collection.ContextStore;

public class MatchFeatureContext extends ContextStore<Feature> {

  public String add(Feature feature) {
    super.add(feature.getId(), feature);
    return feature.getId();
  }

  public <T extends Feature> T get(String id, Class<T> type) {
    return (T) this.get(id);
  }
}
