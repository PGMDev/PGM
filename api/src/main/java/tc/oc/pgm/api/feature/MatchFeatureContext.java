package tc.oc.pgm.api.feature;

public interface MatchFeatureContext extends ContextStore<Feature> {
  String add(Feature feature);

  <T extends Feature> T get(String id, Class<T> type);
}
