package tc.oc.pgm.util.bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import tc.oc.pgm.util.ClassLogger;
import tc.oc.pgm.util.nms.NMSHacksNoOp;
import tc.oc.pgm.util.nms.NMSHacksPlatform;
import tc.oc.pgm.util.nms.v1_10_12.NMSHacks1_10_12;
import tc.oc.pgm.util.nms.v1_8.NMSHacks1_8;
import tc.oc.pgm.util.nms.v1_8.NMSHacksSportPaper;
import tc.oc.pgm.util.nms.v1_9.NMSHacks1_9;

public enum Platform {
  UNKNOWN("UNKNOWN", "UNKNOWN", "UNKNOWN", () -> NMSHacksNoOp.class, false),
  SPORTPAPER_1_8("SportPaper", "1.8", "1.8", () -> NMSHacksSportPaper.class, false),
  SPIGOT_1_8("Spigot", "1.8", "1.8", () -> NMSHacks1_8.class, false),
  PAPER_1_8("Paper", "1.8", "1.8", () -> NMSHacks1_8.class, false),
  SPIGOT_1_9("Spigot", "1.9", "1.9", () -> NMSHacks1_9.class, true),
  PAPER_1_9("Paper", "1.9", "1.9", () -> NMSHacks1_9.class, true),
  SPIGOT_1_10_12("Spigot", "1.10", "1.12", () -> NMSHacks1_10_12.class, true),
  PAPER_1_10_12("Paper", "1.10", "1.12", () -> NMSHacks1_10_12.class, true);

  private static ClassLogger logger = ClassLogger.get(Platform.class);;
  public static Platform SERVER_PLATFORM = computeServerPlatform();

  private static Platform computeServerPlatform() {
    Server sv = Bukkit.getServer();
    String versionString = sv == null ? "" : sv.getVersion();
    for (Platform platform : Platform.values()) {
      for (String supportedMajorVersion : platform.getSupportedMajorVersions()) {
        if (versionString.contains(platform.variant)
            && versionString.contains("MC: " + supportedMajorVersion)) {
          return platform;
        }
      }
    }
    return UNKNOWN;
  }

  private final String variant;
  private final String majorVersionFirst, majorVersionLast;
  private final Supplier<Class<? extends NMSHacksPlatform>> nmsHacksSupplier;
  private final boolean requiresProtocolLib;

  Platform(
      String variant,
      String majorVersionFirst,
      String majorVersionLast,
      Supplier<Class<? extends NMSHacksPlatform>> nmsHacksSupplier,
      boolean requiresProtocolLib) {
    this.variant = variant;
    this.majorVersionFirst = majorVersionFirst;
    this.majorVersionLast = majorVersionLast;
    this.nmsHacksSupplier = nmsHacksSupplier;
    this.requiresProtocolLib = requiresProtocolLib;
  }

  private Set<String> getSupportedMajorVersions() {
    if (majorVersionFirst.equals(majorVersionLast))
      return new HashSet<>(Collections.singleton(majorVersionFirst));

    Set<String> versions = new HashSet<>();

    int min = Integer.parseInt(majorVersionFirst.split("\\.")[1]);
    int max = Integer.parseInt(majorVersionLast.split("\\.")[1]);

    for (int i = min; i <= max; i++) {
      versions.add("1." + i);
    }
    return versions;
  }

  public NMSHacksPlatform getNMSHacks() {
    if (this == UNKNOWN) {
      Bukkit.getServer().getPluginManager().disablePlugin(BukkitUtils.getPlugin());
      throw new UnsupportedOperationException("UNKNOWN Platform!");
    }
    if (this.requiresProtocolLib
        && !Bukkit.getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
      Bukkit.getServer().getPluginManager().disablePlugin(BukkitUtils.getPlugin());
      throw new UnsupportedOperationException(
          "ProtocolLib is required for PGM to run on " + this.toString());
    }
    if (this == SERVER_PLATFORM) {
      try {
        logger.info("Detected server: " + this.toString());
        Constructor<? extends NMSHacksPlatform> constructor =
            nmsHacksSupplier.get().getConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
      } catch (InvocationTargetException
          | InstantiationException
          | IllegalAccessException
          | NoSuchMethodException e) {
        Bukkit.getServer().getPluginManager().disablePlugin(BukkitUtils.getPlugin());
        throw new RuntimeException(e);
      }
    } else {
      Bukkit.getServer().getPluginManager().disablePlugin(BukkitUtils.getPlugin());
      throw new UnsupportedOperationException("getNMSHacks called for incorrect platform!");
    }
  }

  @Override
  public String toString() {
    return this.variant
        + " ("
        + (this.majorVersionFirst.equals(this.majorVersionLast)
            ? this.majorVersionFirst
            : this.majorVersionFirst + "-" + this.majorVersionLast)
        + ")";
  }
}
