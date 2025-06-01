package tc.oc.pgm.util;

/**
 * An int enum of Minecraft versions to their respective data versions
 *
 * @see <a href="https://minecraft.wiki/w/Data_version#Java_Edition">the list of data versions on
 *     the Minecraft Wiki</a>
 */
public final class DataVersions {
  private DataVersions() {}
  /** Unknown/legacy (<1.9) version */
  public static final int LEGACY = -1;

  /** 1.13 */
  public static final int V1_13 = 1519;

  /** 1.18 Experimental Snapshot 1 */
  public static final int V1_18_EXP_1 = 2825;
}
