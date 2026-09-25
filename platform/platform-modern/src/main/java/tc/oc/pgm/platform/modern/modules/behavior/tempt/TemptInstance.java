package tc.oc.pgm.platform.modern.modules.behavior.tempt;

import io.papermc.paper.entity.LookAnchor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.pathfinder.Path;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.platform.modern.modules.behavior.PathWalking;
import tc.oc.pgm.platform.modern.modules.behavior.home.HomeBehavior;
import tc.oc.pgm.platform.modern.modules.mannequin.Mannequin;
import tc.oc.pgm.util.inventory.ItemMatcher;

public class TemptInstance {

  public static final Float DEFAULT_TEMPT_RANGE = 5.0f;

  private final Mannequin mannequin;
  private final TemptBehavior behavior;
  private final @Nullable HomeBehavior home;
  private final Zombie ghost;

  private final @Nullable ItemMatcher holding;
  private final boolean follow;
  private @Nullable MatchPlayer temptTarget;
  private final double rangeSq;
  private @Nullable Path currentPath;
  private long nextRepathTick;

  public TemptInstance(
      Mannequin mannequin, TemptBehavior behavior, @Nullable HomeBehavior home, Zombie ghost) {
    this.mannequin = mannequin;
    this.behavior = behavior;
    this.home = home;
    this.ghost = ghost;
    this.holding = behavior.holding();
    this.follow = behavior.follow();
    this.rangeSq = behavior.range() != null
        ? behavior.range() * behavior.range()
        : DEFAULT_TEMPT_RANGE * DEFAULT_TEMPT_RANGE;
  }

  public boolean matches(Entity entity) {
    return mannequin.getEntityId().equals(entity.getUniqueId());
  }

  public boolean isTempted() {
    return temptTarget != null;
  }

  private boolean isValidTemptTarget(MatchPlayer player) {
    // Follow bypasses holding filter and follows the first valid player
    if (follow) return true;

    if (holding == null) return false;
    var item = player.getBukkit().getInventory().getItemInMainHand();
    return holding.matches(item);
  }

  private @Nullable MatchPlayer findTemptTarget(Match match) {
    // Mannequin cannot change tempter mid-process
    if (temptTarget != null) {
      Player bukkit = temptTarget.getBukkit();
      if (bukkit != null
          && !temptTarget.isDead()
          && isValidTemptTarget(temptTarget)
          && bukkit.getLocation().distanceSquared(mannequin.getLocation()) <= rangeSq
          && (home == null || home.region().contains(bukkit.getLocation()))) {
        return temptTarget;
      }
    }
    // Look for new tempter
    var manLoc = mannequin.getLocation();
    MatchPlayer nearest = null;
    double eligible = rangeSq;

    for (MatchPlayer player : match.getParticipants()) {
      Player bukkit = player.getBukkit();
      if (bukkit == null || player.isDead() || !isValidTemptTarget(player)) continue;

      double dis = bukkit.getLocation().distanceSquared(manLoc);
      if (dis > eligible) continue;

      // If the mannequin has a home, it can only be tempted by players inside of it.
      // leave-home allows a tempted mannequin to follow out of its home (if it has one)
      if (home != null && !home.region().contains(bukkit.getLocation()) && !behavior.leaveHome())
        continue;

      eligible = dis;
      nearest = player;
    }
    return nearest;
  }

  public void tick(Match match, Tick now) {
    temptTarget = findTemptTarget(match);

    if (temptTarget == null) {
      currentPath = null;
      return;
    }

    Player bukkit = temptTarget.getBukkit();
    if (bukkit == null) return;

    // The following logic is duplicated eventually refactor into a public method and clean up

    if (now.tick >= nextRepathTick || currentPath == null || currentPath.isDone()) {
      var manLoc = mannequin.getLocation();
      ghost.setPos(manLoc.getX(), manLoc.getY(), manLoc.getZ());
      ghost.setOnGround(true);
      currentPath = ghost
          .getNavigation()
          .createPath(
              new BlockPos(
                  bukkit.getLocation().getBlockX(),
                  bukkit.getLocation().getBlockY(),
                  bukkit.getLocation().getBlockZ()),
              0);
      nextRepathTick = now.tick + 8;
    }

    PathWalking.step(mannequin, currentPath, 0.15, false); // Walk speed + cant open doors
    mannequin.getEntity().lookAt(bukkit.getEyeLocation(), LookAnchor.EYES);
  }

  public boolean leaveHome() {
    return behavior.leaveHome();
  }
}
