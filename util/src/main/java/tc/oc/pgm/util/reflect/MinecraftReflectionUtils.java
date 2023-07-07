package tc.oc.pgm.util.reflect;

import org.bukkit.Bukkit;

public interface MinecraftReflectionUtils {
  String VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

  static Class<?> getBukkitClass(String classPath) {
    return ReflectionUtils.getClassFromName("org.bukkit." + classPath);
  }

  static Class<?> getCraftBukkitClass(String classPath) {
    return ReflectionUtils.getClassFromName("org.bukkit.craftbukkit." + VERSION + "." + classPath);
  }

  static Class<?> getNMSClass(String classPath) {
    return ReflectionUtils.getClassFromName("net.minecraft.server." + VERSION + "." + classPath);
  }
}
