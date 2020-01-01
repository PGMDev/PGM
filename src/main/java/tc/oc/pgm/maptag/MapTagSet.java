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

  public Component createComponent() {
    Component result = new PersonalizedText();
    MapTag[] mapTags = set.toArray(new MapTag[0]);
    for (int i = 0; i < mapTags.length; i++) {
      if (i != 0) {
        result.extra(Components.space());
      }
      result.extra(mapTags[i].getComponentName());
    }
    return result;
  }

  Set<MapTag> sort(Collection<MapTag> mapTags) {
    SortedSet<MapTag> sorted = new TreeSet<>(Comparator.naturalOrder());
    sorted.addAll(checkNotNull(mapTags));
    return sorted;
  }

  public static MapTagSet immutable(Collection<MapTag> mapTags) {
    return new Immutable(mapTags);
  }

  public static MapTagSet mutable() {
    return mutable(Collections.emptyList());
  }

  public static MapTagSet mutable(Collection<MapTag> mapTags) {
    return new MapTagSet(mapTags);
  }

  static class Immutable extends MapTagSet {
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
