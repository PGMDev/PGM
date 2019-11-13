package tc.oc.pgm.ghostsquadron;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.bukkit.Material;

public class GhostSquadron {
  public static final int REVEAL_STANDARD_DURATION = 20; // ticks
  public static final int MAX_FIRE_TICKS = 40; // ticks

  public static final Set<Material> ALLOWED_DROPS =
      ImmutableSet.<Material>builder().add(Material.ARROW).add(Material.FIREBALL).build();

  public static final Set<Material> BREAKABLE_BLOCKS =
      ImmutableSet.<Material>builder().add(Material.WEB).build();

  public static final int ARROW_REVEAL_DURATION = 15; // ticks

  public static final double LANDMINE_ACTIVATION_DISTANCE = 1.5;
  public static final double LANDMINE_ACTIVATION_DISTANCE_SQ =
      Math.pow(LANDMINE_ACTIVATION_DISTANCE, 2);
  public static final int LANDMINE_SPACING = 1;

  public static final double SPIDEY_SENSE_RADIUS = 5.0;
  public static final double SPIDER_SENSE_RADIUS_SQ = Math.pow(SPIDEY_SENSE_RADIUS, 2);
  public static final int SPIDEY_SENSE_COOLDOWN = 3000; // milliseconds

  public static final double TRACKER_FOOTSTEP_SPACING = 1.0;
  public static final double TRACKER_FOOTSTEP_DY = 0.2;
  public static final int TRACKER_REVEAL_DURATION = 10;

  private GhostSquadron() {}
}
