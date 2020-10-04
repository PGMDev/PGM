package tc.oc.pgm.filters;

import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.util.material.MaterialMatcher;

public class OffsetFilter extends TypedFilter<PlayerQuery> {

  private final String[] relative;
  private final MaterialMatcher materials;

  public OffsetFilter(String[] relative, MaterialMatcher materials) {
    this.relative = relative;
    this.materials = materials;
  }

  @Override
  public Class<? extends PlayerQuery> getQueryType() {
    return PlayerQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(PlayerQuery query) {
    if (query.getPlayer() == null) return QueryResponse.ABSTAIN;
    Location location = query.getLocation();
    Location lookingLocation =
        query.getPlayer().getBukkit().getTargetBlock((Set<Material>) null, 100).getLocation();

    double x = parseRelativeLocation(relative[0], location.getX(), lookingLocation.getX());
    double y = parseRelativeLocation(relative[1], location.getY(), lookingLocation.getY());
    double z = parseRelativeLocation(relative[2], location.getZ(), lookingLocation.getZ());
    return QueryResponse.fromBoolean(
        materials.matches(
            query
                .getMatch()
                .getWorld()
                .getBlockAt(new Location(query.getMatch().getWorld(), x, y, z))
                .getType()));
  }

  private double parseRelativeLocation(
      String coordinate, double locationCoordinate, double eyeCoordinate) {
    boolean relative = false;
    if (coordinate.startsWith("~") || coordinate.startsWith("^")) relative = true;
    double coordinateNumber = Double.parseDouble(relative ? coordinate.substring(1) : coordinate);
    if (coordinate.startsWith("~")) return locationCoordinate + coordinateNumber;
    if (coordinate.startsWith("^")) return eyeCoordinate + coordinateNumber;
    return coordinateNumber;
  }
}
