package tc.oc.pgm.rotation;

import java.io.File;
import java.util.List;

public interface RotationManager {
  File getRotationsFile();

  void loadRotations();

  void evaluatePlayerCount();

  void setEvaluatingPlayerCount(boolean evaluatingPlayerCount);

  boolean isEvaluatingPlayerCount();

  void setActiveRotation(Rotation rotation);

  Rotation getActiveRotation();

  void setRotations(List<Rotation> rotations);

  List<Rotation> getRotations();

  Rotation getRotationByName(String name);
}
