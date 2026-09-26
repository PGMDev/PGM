package tc.oc.pgm.api.map;

import com.google.common.collect.Range;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.DataVersions;
import tc.oc.pgm.util.Version;
import tc.oc.pgm.util.platform.Platform;
import tc.oc.pgm.util.world.WorldFormat;

/**
 * Most bare-bones part of a map definition, which is defined as part of its variant. All other data
 * besides this, resides in {@link MapInfo}
 */
public interface VariantInfo {
  /**
   * The map variant id this info represents
   *
   * @return A variant id for the map, if any.
   */
  String getVariantId();

  /**
   * Get a unique id for the map.
   *
   * @return A unique id.
   */
  String getId();

  /**
   * Get a unique, human-readable name for the map.
   *
   * @return A name, alphanumeric with spaces allowed.
   */
  String getName();

  /**
   * If the map id has been customized to be something other than the normalized name.
   *
   * @return if the map id isn't just the normalized map name
   */
  boolean hasCustomId();

  /** @return custom world folder for this variant, if any */
  @Nullable
  String getWorldFolder();

  /** @return the on-disk layout of this variant's world folder */
  WorldFormat getWorldFormat();

  /** @return the data version of this variant's world, or {@link DataVersions#LEGACY} */
  int getWorldDataVersion();

  Range<Version> getServerVersions();

  default boolean isServerSupported() {
    return getServerVersions().contains(Platform.MINECRAFT_VERSION);
  }

  interface Forwarding extends VariantInfo {
    VariantInfo getVariant();

    @Override
    default String getVariantId() {
      return getVariant().getVariantId();
    }

    @Override
    default String getId() {
      return getVariant().getId();
    }

    @Override
    default String getName() {
      return getVariant().getName();
    }

    @Override
    default boolean hasCustomId() {
      return getVariant().hasCustomId();
    }

    @Override
    @Nullable
    default String getWorldFolder() {
      return getVariant().getWorldFolder();
    }

    @Override
    default WorldFormat getWorldFormat() {
      return getVariant().getWorldFormat();
    }

    @Override
    default int getWorldDataVersion() {
      return getVariant().getWorldDataVersion();
    }

    @Override
    default Range<Version> getServerVersions() {
      return getVariant().getServerVersions();
    }

    @Override
    default boolean isServerSupported() {
      return getVariant().isServerSupported();
    }
  }
}
