package tc.oc.pgm.spawns;

import com.google.common.base.Optional;
import org.bukkit.Location;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.features.FeatureDefinition;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.points.PointProvider;

public class Spawn implements FeatureDefinition {
  protected final SpawnAttributes attributes;
  protected final PointProvider pointProvider;

  public Spawn(SpawnAttributes attributes, PointProvider pointProvider) {
    this.attributes = attributes;
    this.pointProvider = pointProvider;
  }

  public Optional<Kit> getKit() {
    return Optional.fromNullable(this.attributes.kit);
  }

  public boolean allows(MatchPlayer player) {
    return this.attributes.filter.query(player.getQuery()).isAllowed();
  }

  // assume the caller has already called .matches()
  public Location getSpawn(MatchPlayer player) {
    Location location = this.pointProvider.getPoint(player.getMatch(), player.getBukkit());
    if (location == null) {
      player.getMatch().needModule(SpawnMatchModule.class).reportFailedSpawn(this, player);
    }
    return location;
  }

  public void applyKit(MatchPlayer player) {
    Optional<Kit> kit = getKit();
    if (kit.isPresent()) {
      player.applyKit(kit.get(), false);
    }
  }
}
