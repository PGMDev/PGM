package tc.oc.pgm.maptag;

import static com.google.common.base.Preconditions.*;

import com.google.common.base.Joiner;
import com.google.common.collect.ForwardingSet;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.util.components.Components;

public class MapTagSet extends ForwardingSet<MapTag> {

  private final Set<MapTag> set;

  private MapTagSet(Collection<MapTag> mapTags) {
    this.set = sort(checkNotNull(mapTags));
  }

  @Override
  protected Set<MapTag> delegate() {
    return set;
  }

  @Override
  public String toString() {
    return Joiner.on(' ').join(set);
  }

  public Component createComponent(BiConsumer<MapTag, Component> onMapTagComponentCreate) {
    Component result = new PersonalizedText();
    MapTag[] mapTags = set.toArray(new MapTag[0]);
    for (int i = 0; i < mapTags.length; i++) {
      if (i != 0) {
        result.extra(Components.space());
      }

      MapTag mapTag = mapTags[i];
      Component component = mapTags[i].getComponentName();

      if (onMapTagComponentCreate != null) {
        onMapTagComponentCreate.accept(mapTag, component);
      }
      result.extra(component);
    }
    return result;
  }

  Set<MapTag> sort(Collection<MapTag> mapTags) {
    SortedSet<MapTag> sorted = new TreeSet<>(Comparator.naturalOrder());
    sorted.addAll(checkNotNull(mapTags));
    return sorted;
  }

  public static MapTagSet immutable(Collection<MapTag> mapTags) {
    checkNotNull(mapTags);
    if (mapTags.isEmpty()) {
      return Immutable.EMPTY;
    } else if (mapTags instanceof Immutable) {
      return (MapTagSet) mapTags;
    }
    return new Immutable(mapTags);
  }

  public static MapTagSet mutable() {
    return mutable(Collections.emptyList());
  }

  public static MapTagSet mutable(Collection<MapTag> mapTags) {
    return new MapTagSet(mapTags);
  }

  static class Immutable extends MapTagSet {

    private static final Immutable EMPTY = new Immutable(Collections.emptyList());

    private final String toString;

    private Immutable(Collection<MapTag> mapTags) {
      super(checkNotNull(mapTags));
      toString = super.toString();
    }

    @Override
    public String toString() {
      return toString;
    }

    @Override
    Set<MapTag> sort(Collection<MapTag> mapTags) {
      return ImmutableSet.copyOf(super.sort(mapTags));
    }
  }
}
