package tc.oc.pgm.map;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.util.Random;
import org.bukkit.World;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.WorldInfo;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class WorldInfoImpl implements WorldInfo {
  private static final Random random = new Random();
  private final long seed; // 0 means random every time
  private final boolean terrain;
  private final int environment;

  public WorldInfoImpl() {
    this(0L, false, 0);
  }

  public WorldInfoImpl(long seed, boolean terrain, int environment) {
    this.seed = seed;
    this.terrain = terrain;
    this.environment = environment > 1 || environment < -1 ? 0 : environment;
  }

  public WorldInfoImpl(Element element) throws InvalidXMLException {
    this(
        parseSeed(assertNotNull(element).getAttributeValue("seed")),
        XMLUtils.parseBoolean(element.getAttribute("vanilla"), false),
        XMLUtils.parseEnum(
                Node.fromLastChildOrAttr(element, "environment"),
                World.Environment.class,
                "environment",
                World.Environment.NORMAL)
            .ordinal());
  }

  @Override
  public long getSeed() {
    return seed == 0 ? random.nextLong() : seed;
  }

  @Override
  public boolean hasTerrain() {
    return terrain;
  }

  @Override
  public int getEnvironment() {
    return environment;
  }

  private static long parseSeed(@Nullable String value) {
    if (value == null || value.equalsIgnoreCase("random")) return 0L;
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      return value.hashCode();
    }
  }
}
