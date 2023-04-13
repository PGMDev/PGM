package tc.oc.pgm.util.tablist;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.bukkit.ViaUtils;
import tc.oc.pgm.util.text.TextTranslations;

/**
 * A single player's tab list. When this view is enabled, it creates a scoreboard team for each slot
 * and creates an entry for each team. The team names are used to order the list. The view is always
 * full of entries. When an entry is removed, it is replaced by a blank one. The player's list is
 * not disabled, but because it is always full of fake entries, the real entries are pushed off the
 * bottom and cannot be seen. The fake team names all start with a '\u0001' character, so they will
 * always come before any real teams.
 */
public class TabView {

  private static final int WIDTH = 4, HEIGHT = 20;
  private final int size, headerSlot, footerSlot;

  // The single player seeing this view
  private final Player viewer;

  protected @Nullable TabManager manager;

  // True when any slots/header/footer have been changed but not rendered
  protected final TabViewDirtyTracker dirtyTracker;
  private final TabEntry[] slots, rendered;
  private Component header, footer;

  // Only used for legacy players, initialized on enable
  protected @Nullable TabDisplay display = null;

  public TabView(Player viewer) {
    this.viewer = viewer;
    this.size = WIDTH * HEIGHT;
    this.headerSlot = this.size;
    this.footerSlot = this.headerSlot + 1;

    this.dirtyTracker = new TabViewDirtyTracker();

    // Two extra slots for header/footer
    this.slots = new TabEntry[this.size + 2];
    this.rendered = new TabEntry[this.size + 2];
  }

  private void assertEnabled() {
    if (manager == null)
      throw new IllegalStateException(getClass().getSimpleName() + " is not enabled");
  }

  public Player getViewer() {
    return viewer;
  }

  public int getWidth() {
    return WIDTH;
  }

  public int getHeight() {
    return HEIGHT;
  }

  public int getSize() {
    return this.size;
  }

  public TabViewDirtyTracker getDirtyTracker() {
    return dirtyTracker;
  }

  /** Take control of the viewer's player list */
  public void enable(TabManager manager) {
    if (this.manager != null) disable();
    this.manager = manager;

    if (ViaUtils.getProtocolVersion(viewer) <= ViaUtils.VERSION_1_7)
      this.display = new TabDisplay(viewer, WIDTH);

    this.setup();
    this.dirtyTracker.enable(manager.getDirty());
  }

  /** Tear down the display and return control the the viewer's player list to settings */
  public void disable() {
    if (this.manager != null) {
      this.manager.removeView(this);
      this.tearDown();
      this.manager = null;
    }
  }

  protected void invalidateContent(TabEntry entry) {
    int slot = getSlot(entry);
    if (slot == this.headerSlot) dirtyTracker.invalidateHeader();
    else if (slot == this.footerSlot) dirtyTracker.invalidateFooter();
    else if (slot >= 0) dirtyTracker.invalidateContent();
  }

  protected int getSlot(TabEntry entry) {
    for (int i = 0; i < slots.length; i++) {
      if (entry == slots[i]) return i;
    }
    return -1;
  }

  private void setSlot(int slot, @Nullable TabEntry entry) {
    assertEnabled();

    if (entry == null) {
      entry = this.manager.getBlankEntry(slot);
    }

    TabEntry oldEntry = this.slots[slot];
    if (oldEntry != entry) {
      oldEntry.removeFromView(this);

      int oldIndex = getSlot(entry);

      if (oldIndex != -1) {
        TabEntry blankEntry = this.manager.getBlankEntry(oldIndex);
        this.slots[oldIndex] = blankEntry;
        blankEntry.addToView(this);
      } else {
        entry.addToView(this);
      }

      this.slots[slot] = entry;

      if (slot < this.size) {
        dirtyTracker.invalidateLayoutAndContent();
      } else if (slot == this.headerSlot) {
        dirtyTracker.invalidateHeader();
      } else if (slot == this.footerSlot) {
        dirtyTracker.invalidateFooter();
      }
    }
  }

  public void setSlot(int x, int y, @Nullable TabEntry entry) {
    this.setSlot(this.slotIndex(x, y), entry);
  }

  public void setHeader(@Nullable TabEntry entry) {
    this.setSlot(this.headerSlot, entry);
  }

  public void setFooter(@Nullable TabEntry entry) {
    this.setSlot(this.footerSlot, entry);
  }

  private int slotIndex(int x, int y) {
    return x * HEIGHT + y;
  }

  public void render() {
    if (this.manager == null) return;

    if (this.display != null) {
      renderLegacy();
      return;
    }

    TabRender render = new TabRender(this);
    this.renderLayout(render);
    this.renderContent(render);
    this.markSlotsClean();
    this.renderHeaderFooter(render, false);
    render.finish();
    dirtyTracker.validatePriority();
  }

  private void renderLegacy() {
    if (this.manager == null || display == null) return;

    if (dirtyTracker.isLayout() || dirtyTracker.isContent()) {
      dirtyTracker.validateLayout();
      dirtyTracker.validateContent();

      // X & Y are transposed in legacy versions, gotta convert
      for (int x = 0; x < WIDTH; x++) {
        for (int y = 0; y < HEIGHT; y++) {
          int i = slotIndex(x, y);

          if (this.slots[i] == this.rendered[i] && !this.slots[i].isDirty(this)) continue;
          this.rendered[i] = this.slots[i];

          this.display.set(
              x, y, TextTranslations.translateLegacy(this.rendered[i].getContent(this), viewer));
        }
      }
    }
    this.markSlotsClean();
  }

  public void renderPing() {
    if (this.manager == null || this.display != null) return;

    TabRender render = new TabRender(this);

    // Build the update packet from entries with updated ping that are not being added or removed
    for (int i = 0; i < this.size; i++) {
      TabEntry slot = this.slots[i];
      if (slot instanceof PlayerTabEntry) render.updatePing(slot, i);
    }

    render.finish();
  }

  public void renderLayout(TabRender render) {
    if (this.manager == null || this.display != null) return;

    if (dirtyTracker.isLayout()) {
      dirtyTracker.validateLayout();

      // First search for entries that have been added, removed, or moved
      Map<TabEntry, Integer> removals = new HashMap<>();
      Map<TabEntry, Integer> additions = new HashMap<>();

      for (int index = 0; index < this.size; index++) {
        TabEntry oldEntry = this.rendered[index];
        TabEntry newEntry = this.rendered[index] = this.slots[index];

        if (oldEntry != newEntry) {
          // There is a different entry in this slot

          Integer oldIndex = removals.remove(newEntry);
          if (oldIndex == null) {
            // We have not seen the new entry yet, so assume it's being added
            additions.put(newEntry, index);
          } else {
            // We already saw the new entry removed from another slot, so it's actually being moved
            render.changeSlot(newEntry, oldIndex, index);
          }

          Integer newIndex = additions.remove(oldEntry);
          if (newIndex == null) {
            // We have not seen the old entry yet, so assume it's being removed
            removals.put(oldEntry, index);
          } else {
            // We already saw the old entry added to another slot, so it's actually being moved
            render.changeSlot(oldEntry, index, newIndex);
          }
        }
      }

      // Build the removal packet
      for (Map.Entry<TabEntry, Integer> removal : removals.entrySet()) {
        render.removeEntry(removal.getKey(), removal.getValue());
      }

      // Build the addition packet (this also adds to the update packet)
      for (Map.Entry<TabEntry, Integer> addition : additions.entrySet()) {
        render.addEntry(addition.getKey(), addition.getValue());
      }
    }
  }

  public void renderContent(TabRender render) {
    if (this.manager == null || this.display != null) return;

    if (dirtyTracker.isContent()) {
      dirtyTracker.validateContent();

      // Build the update packet from entries with new content that are not being added or removed
      for (int i = 0; i < this.size; i++) {
        if (this.slots[i].isDirty(this)) {
          render.updateEntry(this.slots[i], i);
        }
      }
    }
  }

  public void markSlotsClean() {
    for (TabEntry entry : slots) {
      entry.markClean(this);
    }
  }

  public void renderHeaderFooter() {
    TabRender render = new TabRender(this);
    renderHeaderFooter(render, false);
    render.finish();
  }

  public void renderHeaderFooter(TabRender render, boolean force) {
    if (this.manager == null || this.display != null) return;

    if (force || dirtyTracker.isHeaderOrFooter()) {
      if (force || dirtyTracker.isHeader()) {
        dirtyTracker.validateHeader();
        header = (this.rendered[this.headerSlot] = this.slots[this.headerSlot]).getContent(this);
      }
      if (force || dirtyTracker.isFooter()) {
        dirtyTracker.validateFooter();
        footer = (this.rendered[this.footerSlot] = this.slots[this.footerSlot]).getContent(this);
      }

      render.setHeaderFooter(header, footer);
      this.slots[this.headerSlot].markClean(this);
      this.slots[this.footerSlot].markClean(this);
    }
  }

  private void setup() {
    assertEnabled();

    for (int slot = 0; slot < this.slots.length; slot++) {
      this.slots[slot] = this.manager.getBlankEntry(slot);
      this.slots[slot].addToView(this);
    }

    if (display != null) {
      display.setup();
      System.arraycopy(this.slots, 0, this.rendered, 0, this.size);

      return;
    }

    TabRender render = new TabRender(this);

    for (int index = 0; index < this.size; index++) {
      render.createSlot(this.rendered[index] = this.slots[index], index);
    }

    this.renderHeaderFooter(render, true);

    render.finish();
  }

  private void tearDown() {
    if (this.manager == null) return;

    if (this.display != null) {
      Arrays.fill(this.slots, null);
      Arrays.fill(this.rendered, null);
      display.tearDown();

      return;
    }

    TabRender render = new TabRender(this);

    render.setHeaderFooter(
        this.manager.getBlankEntry(this.headerSlot).getContent(this),
        this.manager.getBlankEntry(this.footerSlot).getContent(this));

    for (int index = 0; index < this.size; index++) {
      render.destroySlot(this.rendered[index], index);
      this.slots[index] = this.rendered[index] = null;
    }

    render.finish();
  }

  protected void refreshEntry(TabEntry entry) {
    if (this.manager == null || this.display != null) return;

    TabRender render = new TabRender(this);
    int slot = getSlot(entry);
    if (slot < this.size) {
      render.refreshEntry(entry, slot);
    } else {
      this.renderHeaderFooter(render, true);
    }
    render.finish();
  }

  protected void updateFakeEntity(TabEntry entry) {
    if (this.manager == null || this.display != null) return;

    TabRender render = new TabRender(this);
    render.updateFakeEntity(entry, false);
    render.finish();
  }

  private void respawnFakeEntities() {
    if (this.manager == null || this.display != null) return;

    this.viewer
        .getServer()
        .getScheduler()
        .runTask(
            this.manager.getPlugin(),
            new Runnable() {
              @Override
              public void run() {
                TabRender render = new TabRender(TabView.this);
                for (TabEntry entry : TabView.this.rendered) {
                  render.updateFakeEntity(entry, true);
                }
                render.finish();
              }
            });
  }

  protected void onRespawn(PlayerRespawnEvent event) {
    if (this.viewer == event.getPlayer()) this.respawnFakeEntities();
  }

  protected void onWorldChange(PlayerChangedWorldEvent event) {
    if (this.viewer == event.getPlayer()) this.respawnFakeEntities();
  }
}
