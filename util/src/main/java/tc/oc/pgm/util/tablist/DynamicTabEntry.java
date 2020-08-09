package tc.oc.pgm.util.tablist;

import com.google.common.collect.Iterables;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Adds dirty tracking of {@link TabView}s. */
public abstract class DynamicTabEntry extends SimpleTabEntry {
  final Set<TabView> cleanViews = new HashSet<>();
  final Set<TabView> dirtyViews = new HashSet<>();

  public DynamicTabEntry(UUID uuid) {
    super(uuid);
  }

  public DynamicTabEntry() {}

  /** Mark all {@link TabView}s containing this entry dirty */
  public void invalidate() {
    if (cleanViews.isEmpty()) return;

    for (TabView view : cleanViews) {
      view.invalidateContent(this);
    }

    dirtyViews.addAll(cleanViews);
    cleanViews.clear();
  }

  @Override
  public boolean isDirty(TabView view) {
    return dirtyViews.contains(view);
  }

  @Override
  public void markClean(TabView view) {
    cleanViews.add(view);
    dirtyViews.remove(view);
  }

  @Override
  public void addToView(TabView view) {
    dirtyViews.add(view);
  }

  @Override
  public void removeFromView(TabView view) {
    cleanViews.remove(view);
    dirtyViews.remove(view);
  }

  public boolean hasViews() {
    return !(dirtyViews.isEmpty() && cleanViews.isEmpty());
  }

  public Iterable<TabView> getViews() {
    return Iterables.concat(cleanViews, dirtyViews);
  }

  /**
   * Re-adds this entry to all {@link TabView}s that contain it, which has the effect of updating
   * its skin.
   */
  public void refresh() {
    for (TabView view : getViews()) view.refreshEntry(this);
  }

  /**
   * Updates the metadata of the fake entity for this entry for all {@link TabView}s, which has the
   * effect of updating the state of the hat layer on the skin.
   */
  public void updateFakeEntity() {
    for (TabView view : getViews()) view.updateFakeEntity(this);
  }
}
