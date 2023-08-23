package tc.oc.pgm.map.source;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.nio.file.Path;
import org.jetbrains.annotations.Nullable;

public class MapRoot {
  // Repository host, if any (eg: 'github.com' or 'gitlab.com')
  private final @Nullable String remoteHost;

  // Display name for the map factory, eg: 'maps' or 'PGMDev/Maps'
  private final String displayName;

  // Repository base url, if any (eg: 'https://github.com/PGMDev/Maps/blob/master/')
  private final @Nullable String baseUrl;

  // If the repository is private
  private final boolean isPrivate;

  // The actual folder in the filesystem the map factory is mounted at
  private final Path base;

  public MapRoot(Path base) {
    this(base, null, null, null, true);
  }

  public MapRoot(
      Path base,
      @Nullable String remoteHost,
      @Nullable String displayName,
      @Nullable String baseUrl,
      boolean isPrivate) {
    this.base = assertNotNull(base);
    this.remoteHost = remoteHost;
    this.displayName = displayName == null ? base.getFileName().toString() : displayName;
    this.baseUrl = baseUrl;
    this.isPrivate = isPrivate;
  }

  public @Nullable String getRemoteHost() {
    return remoteHost;
  }

  public String getDisplayName() {
    return displayName;
  }

  public @Nullable String getBaseUrl() {
    return baseUrl;
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public Path getBase() {
    return base;
  }
}
