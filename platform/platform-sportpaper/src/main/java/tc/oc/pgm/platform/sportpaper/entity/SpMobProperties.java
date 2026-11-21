package tc.oc.pgm.platform.sportpaper.entity;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Skeleton;
import tc.oc.pgm.entity.MobProperties;
import tc.oc.pgm.util.platform.Supports;

@Supports(SPORTPAPER)
public class SpMobProperties extends MobProperties {

  public SpMobProperties() {
    register(ArmorStand.class, "gravity", BOOL, ArmorStand::setGravity);
    register(Horse.class, "variant", enumOf(Horse.Variant.class), Horse::setVariant);
    register(Horse.class, "carrying-chest", BOOL, Horse::setCarryingChest);
    register(
        Skeleton.class,
        "skeleton-type",
        enumOf(Skeleton.SkeletonType.class),
        Skeleton::setSkeletonType);
  }
}
