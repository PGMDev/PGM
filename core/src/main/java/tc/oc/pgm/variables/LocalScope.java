package tc.oc.pgm.variables;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.math.LocalFrame;
import tc.oc.pgm.util.math.LocalRef;

/**
 * Parse-time view of the local variables declared by an action and its enclosing actions. Resolves
 * names to {@link LocalRef}s, with inner declarations shadowing outer ones and globals.
 */
public final class LocalScope {
  public static final String INDEX_ERROR = "Local variables cannot contain an index.";

  private final @Nullable LocalScope parent;
  private final int size;
  private final ImmutableMap<String, LocalRef> refs;

  public LocalScope(@Nullable LocalScope parent, List<String> names) {
    this.parent = parent;
    this.size = names.size();

    Map<String, LocalRef> refs = new HashMap<>();
    for (int slot = 0; slot < names.size(); slot++) {
      refs.put(names.get(slot), LocalRef.of(0, slot));
    }
    if (parent != null) {
      parent.refs.forEach(
          (name, ref) -> refs.putIfAbsent(name, LocalRef.of(ref.depth() + 1, ref.slot())));
    }
    this.refs = ImmutableMap.copyOf(refs);
  }

  public @Nullable LocalRef lookup(String name) {
    return refs.get(name);
  }

  public ImmutableMap<String, LocalRef> visible() {
    return refs;
  }

  // Root scopes never link to the caller's frame, so a callee can't reach into it
  public LocalFrame createFrame(@Nullable LocalFrame parent) {
    if (this.parent != null && parent == null)
      throw new IllegalStateException("Nested local scope entered without its enclosing frame");
    return new LocalFrame(size, this.parent == null ? null : parent);
  }
}
