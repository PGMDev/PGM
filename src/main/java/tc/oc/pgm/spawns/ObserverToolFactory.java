package tc.oc.pgm.spawns;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import tc.oc.item.ItemBuilder;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.PGM;
import tc.oc.pgm.kits.FeatureKitParser;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.util.localization.LocaleMap;
import tc.oc.util.localization.Locales;
import tc.oc.util.logging.ClassLogger;
import tc.oc.xml.InvalidXMLException;

/** Creates some of the items for the observer hotbar */
public class ObserverToolFactory {

  private final Logger logger;
  private final PGM plugin;
  private final LocaleMap<ItemStack> howToBooks = new LocaleMap<>();

  public ObserverToolFactory(PGM plugin) {
    this.plugin = plugin;
    this.logger = ClassLogger.get(plugin.getLogger(), getClass());
    loadHowToBooks();
  }

  public ItemStack getTeleportTool(Player player) {
    return new ItemBuilder()
        .material(Material.COMPASS)
        .name(
            ChatColor.BLUE.toString()
                + ChatColor.BOLD
                + AllTranslations.get().translate("teleportTool.displayName", player))
        .get();
  }

  public @Nullable ItemStack getHowToBook(Player player) {
    return howToBooks.get(Locales.getLocale(player));
  }

  private void loadHowToBooks() {
    String baseFile = plugin.getConfig().getString("howto-book-file");
    if (baseFile == null) return;

    for (Locale locale : AllTranslations.get().getSupportedLocales()) {
      ItemStack book =
          loadHowToBook(new File(baseFile, locale.toLanguageTag().replace('-', '_') + ".xml"));
      if (book != null) {
        logger.fine("Loaded how-to book for locale " + locale);
        howToBooks.put(locale, book);
      }
    }
  }

  @Nullable
  private ItemStack loadHowToBook(File file) {
    try {
      if (!file.isFile()) return null;

      Document doc = new SAXBuilder().build(file);
      FeatureKitParser parser =
          new FeatureKitParser(
              new MapModuleContext(
                  PGM.get(),
                  plugin.getModuleRegistry(),
                  doc,
                  PGM.MAP_PROTO_SUPPORTED,
                  file.getParentFile()));
      return parser.parseBook(doc.getRootElement());
    } catch (JDOMException | IOException | InvalidXMLException e) {
      logger.log(Level.SEVERE, "Failed to parse how-to book from XML file " + file, e);
      return null;
    }
  }
}
