package tc.oc.pgm.util.bossbar;

import com.google.common.collect.Iterables;
import java.util.ArrayDeque;
import java.util.Deque;
import net.kyori.text.Component;
import org.bukkit.entity.Player;

/**
 * A {@link BossBarSource} that combines a mutable stack of child bars, and displays the topmost
 * visible bar. Invalidating any of the child bars causes the stack itself to be invalidated.
 *
 * <p>TODO: Invalidating any child bar causes the stack to re-render for all viewers, regardless of
 * whether the invalid child is visible to them or not. If we ever use viewer-specific bars, or
 * taller stacks of bars, we should probably improve this to only re-render visible bars that are
 * invalidated.
 */
public class BossBarStack extends DynamicBossBar implements BossBarObserver {

  // Ordered most visible to least visible
  private final Deque<BossBarSource> bars = new ArrayDeque<>();

  private BossBarSource getTop() {
    return Iterables.getFirst(bars, BlankBossBar.INSTANCE);
  }

  public BossBarSource getTop(Player viewer) {
    for (BossBarSource bar : bars) {
      if (bar.isVisible(viewer)) return bar;
    }
    return BlankBossBar.INSTANCE;
  }

  public boolean contains(BossBarSource bar) {
    return bars.contains(bar);
  }

  public void push(BossBarSource bar) {
    if (bar != getTop()) {
      bars.remove(bar);
      bars.addFirst(bar);
      bar.addObserver(this);
      invalidate();
    }
  }

  public void remove(BossBarSource bar) {
    if (bars.remove(bar)) {
      bar.removeObserver(this);
      invalidate();
    }
  }

  @Override
  public void invalidate(BossBarSource bar) {
    if (bars.contains(bar)) invalidate();
  }

  @Override
  public boolean isVisible(Player viewer) {
    for (BossBarSource bar : bars) {
      if (bar.isVisible(viewer)) return true;
    }
    return false;
  }

  @Override
  public Component getText(Player viewer) {
    return getTop(viewer).getText(viewer);
  }

  @Override
  public float getMeter(Player viewer) {
    return getTop(viewer).getMeter(viewer);
  }
}
