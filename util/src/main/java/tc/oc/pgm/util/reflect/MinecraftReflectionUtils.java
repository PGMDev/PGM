package tc.oc.pgm.util.reflect;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import org.bukkit.Bukkit;

public interface MinecraftReflectionUtils {
  String VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

  NBTEditor.MinecraftVersion MINECRAFT_VERSION = NBTEditor.getMinecraftVersion();

  double VERSION_NUMBER = parseVersionNumber();

  static double parseVersionNumber() {
    String[] split = VERSION.split("_");

    double version = Double.parseDouble(split[1]);
    ;

    if (split.length > 2) {
      version += 0.1 * Double.parseDouble(split[2].substring(1));
    }

    return version;
  }

  static Class<?> getBukkitClass(String classPath) {
    return ReflectionUtils.getClassFromName("org.bukkit." + classPath);
  }

  static Class<?> getCraftBukkitClass(String classPath) {
    return ReflectionUtils.getClassFromName("org.bukkit.craftbukkit." + VERSION + "." + classPath);
  }

  static Class<?> getNMSClassOriginal(String classPath, String newPath) {
    if (MINECRAFT_VERSION.lessThanOrEqualTo(NBTEditor.MinecraftVersion.v1_16)) {
      return ReflectionUtils.getClassFromName("net.minecraft.server." + VERSION + "." + classPath);
    } else {
      return ReflectionUtils.getClassFromName("net.minecraft." + newPath);
    }
  }

  static Class<?> getNMSClassLegacy(String classPath) {
    return ReflectionUtils.getClassFromName("net.minecraft.server." + VERSION + "." + classPath);
  }

  static Class<?> getNMSClassNew(String classPath) {
    return ReflectionUtils.getClassFromName("net.minecraft." + classPath);
  }
}
