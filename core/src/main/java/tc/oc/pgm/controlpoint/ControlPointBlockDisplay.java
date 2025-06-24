package tc.oc.pgm.controlpoint;

import static tc.oc.pgm.util.material.ColorUtils.COLOR_UTILS;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.controlpoint.events.CapturingTimeChangeEvent;
import tc.oc.pgm.controlpoint.events.ControllerChangeEvent;
import tc.oc.pgm.filters.operator.AllFilter;
import tc.oc.pgm.filters.operator.InverseFilter;
import tc.oc.pgm.filters.query.BlockQuery;
import tc.oc.pgm.regions.FiniteBlockRegion;
import tc.oc.pgm.regions.SectorRegion;
import tc.oc.pgm.snapshot.SnapshotMatchModule;
import tc.oc.pgm.snapshot.WorldSnapshot;
import tc.oc.pgm.util.block.BlockVectors;

/** Displays the status of a ControlPoint by coloring blocks in specified regions */
public class ControlPointBlockDisplay implements Listener {
  protected final Match match;
  protected final ControlPoint controlPoint;

  protected final FiniteBlockRegion progressDisplayRegion;
  protected final FiniteBlockRegion controllerDisplayRegion;
  protected final WorldSnapshot snapshot;

  protected Competitor controllingTeam;

  public ControlPointBlockDisplay(Match match, ControlPoint controlPoint) {
    this.match = match;
    this.controlPoint = controlPoint;
    this.snapshot = match.moduleRequire(SnapshotMatchModule.class).getOriginalSnapshot();

    Filter visualMaterials = controlPoint.getDefinition().getVisualMaterials();
    Region progressDisplayRegion = controlPoint.getDefinition().getProgressDisplayRegion();
    Region controllerDisplayRegion = controlPoint.getDefinition().getControllerDisplayRegion();

    if (progressDisplayRegion == null) {
      this.progressDisplayRegion = null;
    } else {
      this.progressDisplayRegion = FiniteBlockRegion.fromWorld(
          progressDisplayRegion,
          match.getWorld(),
          visualMaterials,
          match.getMap().getProto());
      snapshot.saveRegion(progressDisplayRegion);
    }

    if (controllerDisplayRegion == null) {
      this.controllerDisplayRegion = null;
    } else {
      Filter controllerDisplayFilter = this.progressDisplayRegion == null
          ? visualMaterials
          : AllFilter.of(visualMaterials, new InverseFilter(progressDisplayRegion));

      this.controllerDisplayRegion = FiniteBlockRegion.fromWorld(
          controllerDisplayRegion,
          match.getWorld(),
          controllerDisplayFilter,
          match.getMap().getProto());
      snapshot.saveRegion(controllerDisplayRegion);
    }
  }

  /**
   * Change the controller display to the given team's color, or reset the display if team is null
   */
  public void setController(Competitor controllingTeam) {
    if (this.controllingTeam != controllingTeam && this.controllerDisplayRegion != null) {
      if (controllingTeam == null) {
        snapshot.placeBlocks(this.controllerDisplayRegion, null, false);
      } else {
        COLOR_UTILS.setColor(
            match.getWorld(),
            this.controllerDisplayRegion.getBlockVectors(),
            controllingTeam.getDyeColor());
      }
      this.controllingTeam = controllingTeam;
    }
  }

  private void setBlock(BlockVector pos, Competitor team) {
    final Block block = BlockVectors.blockAt(match.getWorld(), pos);
    if (this.controlPoint
        .getDefinition()
        .getVisualMaterials()
        .query(new BlockQuery(block))
        .isAllowed()) {
      if (team != null) {
        COLOR_UTILS.setColor(block, team.getDyeColor());
      } else {
        snapshot.getOriginalMaterial(pos).applyTo(block, false);
      }
    }
  }

  protected void setProgress(
      Competitor controllingTeam, Competitor capturingTeam, double capturingProgress) {
    if (this.progressDisplayRegion != null) {
      Vector center = this.progressDisplayRegion.getBounds().getCenterPoint();

      // capturingProgress can be zero, but it can never be one, so invert it to avoid
      // a zero-area SectorRegion that can cause glitchy rendering
      SectorRegion sectorRegion =
          new SectorRegion(center.getX(), center.getZ(), 0, (1 - capturingProgress) * 2 * Math.PI);

      for (BlockVector pos : this.progressDisplayRegion.getBlockVectors()) {
        if (sectorRegion.contains(pos)) {
          this.setBlock(pos, controllingTeam);
        } else {
          this.setBlock(pos, capturingTeam);
        }
      }
    }
  }

  public void render() {
    this.setController(this.controlPoint.getControllingTeam());
    this.setProgress(
        this.controlPoint.getControllingTeam(),
        this.controlPoint.getCapturingTeam(),
        this.controlPoint.getCompletion());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onTimeChange(CapturingTimeChangeEvent event) {
    if (this.controlPoint == event.getControlPoint()) {
      this.setProgress(
          event.getControlPoint().getControllingTeam(),
          event.getControlPoint().getCapturingTeam(),
          event.getControlPoint().getCompletion());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onControllerChange(ControllerChangeEvent event) {
    if (this.controlPoint == event.getControlPoint()) {
      this.setController(event.getNewController());
    }
  }
}
