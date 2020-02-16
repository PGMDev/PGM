package tc.oc.pgm.bossbar;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInitialSpawnEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import tc.oc.bossbar.BossBar;
import tc.oc.bossbar.BossBarStack;
import tc.oc.bossbar.BossBarView;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerLeaveMatchEvent;
import tc.oc.world.NMSHacks;

@ListenerScope(MatchScope.LOADED)
public class BossBarMatchModule implements MatchModule, Listener {

  private final int entityId;
  private final Map<Player, BossBarView> views = new HashMap<>();
  private final BossBarStack stack = new BossBarStack();

  public BossBarMatchModule(Match match) {
    this.entityId = NMSHacks.allocateEntityId(match);
  }

  protected void removeView(Player viewer) {
    BossBarView view = views.remove(viewer);
    if (view != null) view.setBar(null);
  }

  /** Is the given bar currently anywhere in the display stack? */
  public boolean containsBossBar(BossBar bar) {
    return stack.contains(bar);
  }

  /**
   * Push the given bar on top of the display stack. If it is already somewhere else in the stack,
   * it will be moved to the top. A bar cannot be in multiple places in the stack simultaneously.
   *
   * <p>At any given time, the bar that is actually visible will be the highest one in the stack
   * that returns true from {@link BossBar#isVisible}. This logic is applied seperately for each
   * viewer, thus different viewers can see different bars.
   */
  public void pushBossBar(BossBar bar) {
    stack.push(bar);
  }

  /**
   * If the given bar is not currently in the display stack (i.e. {@link #containsBossBar} returns
   * false) then put it on top of the stack by calling {@link #pushBossBar}. If it is already in the
   * stack, do nothing.
   */
  public void pushBossBarIfAbsent(BossBar bar) {
    if (!containsBossBar(bar)) pushBossBar(bar);
  }

  /** Remove the given bar from anywhere in the display stack. */
  public void removeBossBar(BossBar bar) {
    stack.remove(bar);
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinMatchEvent event) {
    BossBarView view = new BossBarView(PGM.get(), event.getPlayer().getBukkit(), entityId);
    view.setBar(stack);
    views.put(event.getPlayer().getBukkit(), view);
  }

  @EventHandler
  public void onPlayerLeave(PlayerLeaveMatchEvent event) {
    // If the match is cycling, we need to destroy the boss entities, or they will still be there in
    // the next match
    removeView(event.getPlayer().getBukkit());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerMove(PlayerMoveEvent event) {
    BossBarView view = views.get(event.getPlayer());
    if (view != null) view.onPlayerMove(event);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerMove(PlayerTeleportEvent event) {
    BossBarView view = views.get(event.getPlayer());
    if (view != null) view.onPlayerMove(event);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerRespawn(PlayerInitialSpawnEvent event) {
    BossBarView view = views.get(event.getPlayer());
    if (view != null) view.onPlayerRespawn(event);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerRespawn(PlayerRespawnEvent event) {
    BossBarView view = views.get(event.getPlayer());
    if (view != null) view.onPlayerRespawn(event);
  }
}
