package tc.oc.pgm.features;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.util.collection.ContextStore;

public class MatchFeatureContext extends ContextStore<Feature<?>> {

  private final Map<StateHolder<?>, Object> states = new IdentityHashMap<>();

  public String add(Feature<?> feature) {
    super.add(feature.getId(), feature);
    return feature.getId();
  }

  @SuppressWarnings("unchecked")
  public <T extends Feature<?>> T get(String id, Class<T> type) {
    return (T) this.get(id);
  }

  public <T> void registerState(StateHolder<T> stateHolder, T state) {
    if (states.putIfAbsent(stateHolder, state) != null) {
      throw new IllegalStateException("State already registered: " + stateHolder);
    }
  }

  @SuppressWarnings("unchecked")
  public <T> T getState(StateHolder<T> stateHolder) {
    return (T) assertNotNull(states.get(stateHolder), "state");
  }

  public boolean hasState(StateHolder<?> stateHolder) {
    return states.containsKey(stateHolder);
  }

  public Map<StateHolder<?>, Object> getStates() {
    return Collections.unmodifiableMap(states);
  }
}
