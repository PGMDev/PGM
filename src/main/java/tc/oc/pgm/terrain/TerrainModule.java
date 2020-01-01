package tc.oc.pgm.terrain;

import java.io.File;
import java.util.Random;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.generator.ChunkGenerator;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.chunk.NullChunkGenerator;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.map.MapModuleFactory;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

@ModuleDescription(name = "Terrain")
public class TerrainModule extends MapModule {

  private final TerrainOptions options;

  public TerrainModule(TerrainOptions options) {
    this.options = options;
  }

  public TerrainOptions getOptions() {
    return options;
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

  public File getWorldFolder() {
    return options.worldFolder;
  }

  public static class Factory implements MapModuleFactory<TerrainModule> {
    @Override
    public TerrainModule parse(MapModuleContext context, Logger logger, Document doc)
        throws InvalidXMLException {
      File worldFolder = context.getBasePath();
      boolean vanilla = false;
      Long seed = null;

      for (Element elTerrain : doc.getRootElement().getChildren("terrain")) {
        vanilla = XMLUtils.parseBoolean(elTerrain.getAttribute("vanilla"), vanilla);
        worldFolder =
            XMLUtils.parseRelativeFolder(
                context.getBasePath(), Node.fromAttr(elTerrain, "world"), worldFolder);

        String seedText = elTerrain.getAttributeValue("seed");
        if (seedText != null) {
          try {
            seed = Long.parseLong(seedText);
          } catch (NumberFormatException e) {
            seed = (long) seedText.hashCode();
          }
        }
      }

      return new TerrainModule(new TerrainOptions(worldFolder, vanilla, seed));
    }
  }
}
