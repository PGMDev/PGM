package tc.oc.pgm.tracker;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.RangedInfo;

public final class Trackers {
  private Trackers() {}

  static @Nullable TrackerMatchModule getModule(World world) {
    Match match = PGM.get().getMatchManager().getMatch(world);
    return match == null ? null : match.getModule(TrackerMatchModule.class);
  }

  public static @Nullable ParticipantState getOwner(Entity entity) {
    TrackerMatchModule tmm = getModule(entity.getWorld());
    return tmm == null ? null : tmm.getEntityTracker().getOwner(entity);
  }

  public static @Nullable ParticipantState getOwner(Block block) {
    TrackerMatchModule tmm = getModule(block.getWorld());
    return tmm == null ? null : tmm.getBlockTracker().getOwner(block);
  }

  public static double distanceFromRanged(RangedInfo rangedInfo, @Nullable Location deathLocation) {
    if (rangedInfo.getOrigin() == null || deathLocation == null) return Double.NaN;

    // When players fall in the void, use y=0 as their death location
    if (deathLocation.getY() < 0) {
      deathLocation = deathLocation.clone();
      deathLocation.setY(0);
    }
    return deathLocation.distance(rangedInfo.getOrigin());
  }
}
