package tc.oc.pgm.util.world;

import org.bukkit.World;

/**
 * Everything needed to load a match world.
 *
 * @param name the world name, also its dimension key path
 * @param environment the world's environment
 * @param terrain whether to generate terrain, rather than leaving un-generated chunks empty
 * @param seed the world seed, only meaningful with terrain
 * @param format the on-disk layout of the world folder
 * @param dataVersion the world's data version, or {@link tc.oc.pgm.util.DataVersions#LEGACY}
 */
public record WorldCreationInfo(
    String name,
    World.Environment environment,
    boolean terrain,
    long seed,
    WorldFormat format,
    int dataVersion) {}
