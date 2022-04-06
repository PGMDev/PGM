package tc.oc.pgm.api.feature;

import tc.oc.pgm.api.collection.ContextStore;

public class MatchFeatureContext extends ContextStore<Feature> {
  public String add(Feature feature) {
    super.add(feature.getId(), feature);
    return feature.getId();
  }

  public <T extends Feature> T get(String id, Class<T> type) {
    return (T) this.get(id);
  }
}
