package tc.oc.pgm.variables;

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.math.LocalFrame;

public final class LocalScope {
  public static final String INDEX_ERROR = "Local variables cannot contain an index.";

  private final @Nullable LocalScope parent;
  private final ImmutableList<String> names;

  public LocalScope(@Nullable LocalScope parent, List<String> names) {
    this.parent = parent;
    this.names = ImmutableList.copyOf(names);
  }

  public @Nullable LocalRef lookup(String name) {
    int depth = 0;
    for (var scope = this; scope != null; scope = scope.parent, depth++) {
      int slot = scope.names.indexOf(name);
      if (slot >= 0) return new LocalRef(name, depth, slot);
    }
    return null;
  }

  public Map<String, LocalRef> visible() {
    Map<String, LocalRef> result = new HashMap<>();
    int depth = 0;
    for (var scope = this; scope != null; scope = scope.parent, depth++) {
      for (int slot = 0; slot < scope.names.size(); slot++) {
        result.putIfAbsent(scope.names.get(slot), new LocalRef(scope.names.get(slot), depth, slot));
      }
    }
    return result;
  }

  // Root scopes never link to the caller's frame, so a callee can't reach into it
  public LocalFrame createFrame(@Nullable LocalFrame parent) {
    return new LocalFrame(names.size(), this.parent == null ? null : parent);
  }
}
