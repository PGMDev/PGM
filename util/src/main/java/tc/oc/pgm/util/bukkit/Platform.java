package tc.oc.pgm.util.bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import tc.oc.pgm.util.ClassLogger;
import tc.oc.pgm.util.nms.NMSHacksNoOp;
import tc.oc.pgm.util.nms.NMSHacksPlatform;
import tc.oc.pgm.util.nms.v1_8.NMSHacksSportPaper;
import tc.oc.pgm.util.nms.v1_9.NMSHacks1_9;
import tc.oc.pgm.util.reflect.ReflectionUtils;

public enum Platform {
  UNKNOWN("UNKNOWN", "UNKNOWN", NMSHacksNoOp.class, false),
  SPORTPAPER_1_8("SportPaper", "1.8", NMSHacksSportPaper.class, false),
  SPIGOT_1_8(
      "Spigot",
      "1.8", // NMSHacks1_8 causes issues with other versions, get it dynamically
      (Class<? extends NMSHacksPlatform>)
          ReflectionUtils.getClassFromName("tc.oc.pgm.util.nms.v1_8.NMSHacks1_8"),
      false),
  PAPER_1_8(
      "Paper",
      "1.8", // NMSHacks1_8 causes issues with other versions, get it dynamically
      (Class<? extends NMSHacksPlatform>)
          ReflectionUtils.getClassFromName("tc.oc.pgm.util.nms.v1_8.NMSHacks1_8"),
      false),
  SPIGOT_1_9("Spigot", "1.9", NMSHacks1_9.class, true),
  PAPER_1_9("Paper", "1.9", NMSHacks1_9.class, true);

  private static ClassLogger logger = ClassLogger.get(Platform.class);;
  public static Platform SERVER_PLATFORM = computeServerPlatform();

  private static Platform computeServerPlatform() {
    Server sv = Bukkit.getServer();
    String versionString = sv == null ? "" : sv.getVersion();
    for (Platform platform : Platform.values()) {
      if (versionString.contains(platform.variant)
          && versionString.contains("MC: " + platform.majorVersion)) {
        return platform;
      }
    }
    return UNKNOWN;
  }

  private final String variant;
  private final String majorVersion;
  private final Class<? extends NMSHacksPlatform> nmsHacksClass;
  private final boolean requiresProtocolLib;

  Platform(
      String variant,
      String majorVersion,
      Class<? extends NMSHacksPlatform> nmsHacksClass,
      boolean requiresProtocolLib) {
    this.variant = variant;
    this.majorVersion = majorVersion;
    this.nmsHacksClass = nmsHacksClass;
    this.requiresProtocolLib = requiresProtocolLib;
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
        Constructor<? extends NMSHacksPlatform> constructor = nmsHacksClass.getConstructor();
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
    return this.variant + " (" + this.majorVersion + ")";
  }
}
