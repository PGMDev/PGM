package tc.oc.tablist;

import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSkinPartsChangeEvent;
import org.bukkit.plugin.Plugin;
import tc.oc.identity.PlayerIdentityChangeEvent;
import tc.oc.util.ClassLogger;
import tc.oc.util.collection.DefaultMapAdapter;
import tc.oc.util.collection.DefaultProvider;

/**
 * Custom player list display (for 1.8 and later)
 *
 * <p>Class Overview: TabManager God object that connects everything together, should only need one
 * of these TabView A single player's custom tab list TabEntry Abstract base for a piece of content
 * that goes in a tab list slot GenericTabEntry Adds a few more generic things to TabEntry
 * TextTabEntry TabEntry containing arbitrary text content PlayerTabEntry TabEntry showing a
 * player's name and skin BlankTabEntry A pool of these is used to fill in empty slots TabRender
 * Instantiated for each render, contains all the hacky stuff
 *
 * <p>A single TabEntry can be part of multiple TabViews simultaneously, and can show different
 * content for each view. The idea is that TabEntry subclasses can be quite smart and generate their
 * content dynamically. They have a dirty flag, so the content is not generated unless the TabEntry
 * has been invalidated. They can also respond to events by invalidating themselves, which is
 * automatically propagated to any TabViews that contain them.
 *
 * <p>Rendering is deferred and always happens through the TabManager.render() method. This will
 * check all views for dirtiness and render them. It is left to subclasses to call this method, so
 * they can render whenever they want, potentially deferring it for efficiency. However, all views
 * must be rendered together. It is not possible to render views individually, because this would
 * make the TabEntry dirty state very difficult to track.
 */
public class TabManager implements Listener {
  protected final Logger logger;
  protected final Plugin plugin;
  final DefaultMapAdapter<Player, TabView> enabledViews;

  final DefaultMapAdapter<Player, TabEntry> playerEntries;
  final Map<Integer, TabEntry> blankEntries =
      new DefaultMapAdapter<Integer, TabEntry>(new BlankTabEntry.Factory(), true);

  boolean dirty;

  public TabManager(
      Plugin plugin,
      @Nullable DefaultProvider<Player, ? extends TabView> viewProvider,
      @Nullable DefaultProvider<Player, ? extends TabEntry> playerEntryProvider) {

    if (viewProvider == null) viewProvider = new TabView.Factory();
    if (playerEntryProvider == null) playerEntryProvider = new PlayerTabEntry.Factory();

    this.logger = ClassLogger.get(plugin.getLogger(), getClass());
    this.plugin = plugin;
    this.enabledViews = new DefaultMapAdapter<>(viewProvider, true);
    this.playerEntries = new DefaultMapAdapter<>(playerEntryProvider, true);
  }

  public TabManager(Plugin plugin) {
    this(plugin, null, null);
  }

  public Plugin getPlugin() {
    return plugin;
  }

  public @Nullable TabView getViewOrNull(Player viewer) {
    return this.enabledViews.getOrNull(viewer);
  }

  public @Nullable TabView getView(Player viewer) {
    return this.enabledViews.get(viewer);
  }

  protected void removeView(TabView view) {
    if (this.enabledViews.remove(view.getViewer()) != null) view.disable();
  }

  public @Nullable TabEntry getPlayerEntryOrNull(Player player) {
    return this.playerEntries.getOrNull(player);
  }

  public TabEntry getPlayerEntry(Player player) {
    return this.playerEntries.get(player);
  }

  public TabEntry removePlayerEntry(Player player) {
    return this.playerEntries.remove(player);
  }

  protected TabEntry getBlankEntry(int index) {
    return this.blankEntries.get(index);
  }

  protected void invalidate() {
    this.dirty = true;
  }

  public void render() {
    if (this.dirty) {
      for (TabView view : this.enabledViews.values()) {
        view.render();
      }

      this.dirty = false;
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onQuit(PlayerQuitEvent event) {
    TabView view = this.getViewOrNull(event.getPlayer());
    if (view != null) {
      view.disable();
      this.removeView(view);
    }
    this.removePlayerEntry(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onRespawn(PlayerRespawnEvent event) {
    TabView view = this.getViewOrNull(event.getPlayer());
    if (view != null) view.onRespawn(event);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWorldChange(PlayerChangedWorldEvent event) {
    TabView view = this.getViewOrNull(event.getPlayer());
    if (view != null) view.onWorldChange(event);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onNickChange(PlayerIdentityChangeEvent event) {
    TabEntry entry = this.getPlayerEntryOrNull(event.getPlayer());
    if (entry instanceof PlayerTabEntry) {
      ((PlayerTabEntry) entry).onNickChange(event);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onSkinPartsChange(PlayerSkinPartsChangeEvent event) {
    TabEntry entry = this.getPlayerEntryOrNull(event.getPlayer());
    if (entry instanceof PlayerTabEntry) {
      ((PlayerTabEntry) entry).onSkinPartsChange(event);
    }
  }
}
