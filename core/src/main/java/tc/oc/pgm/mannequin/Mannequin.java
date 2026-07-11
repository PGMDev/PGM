package tc.oc.pgm.mannequin;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface Mannequin {

  String getId();
  void teleport(Location location);
  void despawn();
  void setPose(MannequinPose pose);
  void setGlowing(@Nullable Color color);
}
