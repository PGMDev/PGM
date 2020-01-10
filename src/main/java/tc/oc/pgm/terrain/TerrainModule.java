package tc.oc.pgm.terrain;

import java.util.Random;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.generator.ChunkGenerator;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.chunk.NullChunkGenerator;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;

public class TerrainModule implements MapModule {

  private final TerrainOptions options;

  public TerrainModule(TerrainOptions options) {
    this.options = options;
  }

  public @Nullable ChunkGenerator getChunkGenerator() {
    return options.vanilla ? null : new NullChunkGenerator();
  }

  public long getSeed() {
    if (options.seed == null) {
      return new Random().nextLong();
    } else {
      return options.seed;
    }
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return null;
  }

  public static class Factory implements MapModuleFactory<TerrainModule> {
    @Override
    public TerrainModule parse(MapContext context, Logger logger, Document doc)
        throws InvalidXMLException {
      boolean vanilla = false;
      Long seed = null;

      for (Element elTerrain : doc.getRootElement().getChildren("terrain")) {
        vanilla = XMLUtils.parseBoolean(elTerrain.getAttribute("vanilla"), vanilla);
        String seedText = elTerrain.getAttributeValue("seed");
        if (seedText != null) {
          try {
            seed = Long.parseLong(seedText);
          } catch (NumberFormatException e) {
            seed = (long) seedText.hashCode();
          }
        }
      }

      return new TerrainModule(new TerrainOptions(vanilla, seed));
    }
  }
}
