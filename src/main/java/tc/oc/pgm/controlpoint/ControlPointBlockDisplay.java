package tc.oc.pgm.controlpoint;

import com.google.common.collect.Lists;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.controlpoint.events.CapturingTimeChangeEvent;
import tc.oc.pgm.controlpoint.events.ControllerChangeEvent;
import tc.oc.pgm.filters.Filter;
import tc.oc.pgm.filters.query.BlockQuery;
import tc.oc.pgm.regions.FiniteBlockRegion;
import tc.oc.pgm.regions.Region;
import tc.oc.pgm.regions.SectorRegion;
import tc.oc.pgm.renewable.BlockImage;
import tc.oc.pgm.teams.Team;
import tc.oc.server.BukkitUtils;

import java.util.List;

/** Displays the status of a ControlPoint by coloring blocks in specified regions */
public class ControlPointBlockDisplay implements Listener {
  protected final Match match;
  protected final ControlPoint controlPoint;

  protected final FiniteBlockRegion progressDisplayRegion;
  protected final BlockImage progressDisplayImage;
  protected final FiniteBlockRegion controllerDisplayRegion;
  protected final BlockImage controllerDisplayImage;

  protected Team controllingTeam;

  public ControlPointBlockDisplay(Match match, ControlPoint controlPoint) {
    this.match = match;
    this.controlPoint = controlPoint;

    Filter visualMaterials = controlPoint.getDefinition().getVisualMaterials();
    Region progressDisplayRegion = controlPoint.getDefinition().getProgressDisplayRegion();
    Region controllerDisplayRegion = controlPoint.getDefinition().getControllerDisplayRegion();

    if (progressDisplayRegion == null) {
      this.progressDisplayRegion = null;
      this.progressDisplayImage = null;
    } else {
      this.progressDisplayRegion =
          FiniteBlockRegion.fromWorld(progressDisplayRegion, match.getWorld(), visualMaterials, match.getMap().getProto());
      this.progressDisplayImage =
          new BlockImage(match.getWorld(), this.progressDisplayRegion.getBounds());
      this.progressDisplayImage.save();
    }

    if (controllerDisplayRegion == null) {
      this.controllerDisplayRegion = null;
      this.controllerDisplayImage = null;
    } else {
      FiniteBlockRegion unfilteredControllerDisplayRegion =
          FiniteBlockRegion.fromWorld(controllerDisplayRegion, match.getWorld(), visualMaterials, match.getMap().getProto());

      // Ensure the controller and progress display regions do not overlap. The progress display has
      // priority.
      List<Block> filteredControllerBlocks =
          Lists.newArrayList(unfilteredControllerDisplayRegion.getBlocks());
      if (this.progressDisplayRegion != null) {
        filteredControllerBlocks.removeAll(this.progressDisplayRegion.getBlocks());
      }
      this.controllerDisplayRegion = new FiniteBlockRegion(filteredControllerBlocks);
      this.controllerDisplayImage =
          new BlockImage(match.getWorld(), this.controllerDisplayRegion.getBounds());
      this.controllerDisplayImage.save();
    }
  }

  /**
   * Change the controller display to the given team's color, or reset the display if team is null
   */
  @SuppressWarnings("deprecation")
  public void setController(Team controllingTeam) {
    if (this.controllingTeam != controllingTeam && this.controllerDisplayRegion != null) {
      if (controllingTeam == null) {
        for (Block block : this.controllerDisplayRegion.getBlocks()) {
          this.controllerDisplayImage.restore(block);
        }
      } else {
        byte blockData = BukkitUtils.chatColorToDyeColor(controllingTeam.getColor()).getWoolData();
        for (Block block : this.controllerDisplayRegion.getBlocks()) {
          block.setData(blockData);
        }
      }
      this.controllingTeam = controllingTeam;
    }
  }

  @SuppressWarnings("deprecation")
  private void setBlock(Block block, Team team) {
    if (this.controlPoint
        .getDefinition()
        .getVisualMaterials()
        .query(new BlockQuery(block))
        .isAllowed()) {
      if (team != null) {
        block.setData(BukkitUtils.chatColorToDyeColor(team.getColor()).getWoolData());
      } else {
        this.progressDisplayImage.restore(block);
      }
    }
  }

  protected void setProgress(Team controllingTeam, Team capturingTeam, double capturingProgress) {
    if (this.progressDisplayRegion != null) {
      Vector center = this.progressDisplayRegion.getBounds().getCenterPoint();

      // capturingProgress can be zero, but it can never be one, so invert it to avoid
      // a zero-area SectorRegion that can cause glitchy rendering
      SectorRegion sectorRegion =
          new SectorRegion(center.getX(), center.getZ(), 0, (1 - capturingProgress) * 2 * Math.PI);

      for (Block block : this.progressDisplayRegion.getBlocks()) {
        if (sectorRegion.contains(block.getLocation().toVector())) {
          this.setBlock(block, controllingTeam);
        } else {
          this.setBlock(block, capturingTeam);
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
