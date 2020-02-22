package tc.oc.util.bukkit.translations;

import org.bukkit.Bukkit;
import tc.oc.util.bukkit.translations.provider.TranslationProvider;

public final class AllTranslations extends BaseTranslator {

  private static AllTranslations instance;

  private AllTranslations() {
    super(Bukkit.getLogger(), new TranslationProvider("strings"));
  }

  public static AllTranslations get() {
    return instance == null ? instance = new AllTranslations() : instance;
  }
}
