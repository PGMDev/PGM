package tc.oc.pgm.util.world;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.bukkit.World;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.platform.Platform;

public interface WorldStorage {
  WorldStorage WORLD_STORAGE = Platform.get(WorldStorage.class);

  @Nullable
  World createWorld(WorldCreationInfo info);

  File getWorldDirectory(String worldName, WorldFormat format);

  List<File> getWorldDirectories();

  int getWorldDataVersion(Path worldDir, WorldFormat format);

  List<String> getDiscardedFiles(WorldFormat format);
}
