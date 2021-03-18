package tc.oc.pgm.util.collection;

import com.google.common.collect.ForwardingSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;

/**
 * Highly efficient immutable container for {@link Material}s. Useful for sharing large material
 * lists.
 */
public class ImmutableMaterialSet extends ForwardingSet<Material> {

  private final Set<Material> materials;

  private ImmutableMaterialSet(Set<Material> materials) {
    this.materials = materials;
  }

  private static final ImmutableMaterialSet EMPTY =
      new ImmutableMaterialSet(Collections.<Material>emptySet());

  public static ImmutableMaterialSet of(ImmutableMaterialSet materials) {
    return materials;
  }

  public static ImmutableMaterialSet of() {
    return EMPTY;
  }

  public static ImmutableMaterialSet of(Material... materials) {
    return new ImmutableMaterialSet(
        Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(materials))));
  }

  public static ImmutableMaterialSet of(Collection<Material> materials) {
    if (materials instanceof ImmutableMaterialSet) {
      return (ImmutableMaterialSet) materials;
    } else {
      return new ImmutableMaterialSet(Collections.unmodifiableSet(EnumSet.copyOf(materials)));
    }
  }

  @Override
  protected Set<Material> delegate() {
    return materials;
  }

  public static ImmutableMaterialSet.Builder builder() {
    return new ImmutableMaterialSet.Builder();
  }

  public static class Builder {
    private final EnumSet<Material> materials = EnumSet.noneOf(Material.class);
    private boolean built;

    private void assertUnbuilt() {
      if (built) throw new IllegalStateException("Already built");
    }

    public void add(Material material) {
      assertUnbuilt();
      materials.add(material);
    }

    public boolean isEmpty() {
      assertUnbuilt();
      return materials.isEmpty();
    }

    public ImmutableMaterialSet build() {
      built = true;
      return materials.isEmpty() ? EMPTY : new ImmutableMaterialSet(materials);
    }
  }
}
