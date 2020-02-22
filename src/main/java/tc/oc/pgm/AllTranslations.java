package tc.oc.pgm;

import tc.oc.pgm.api.PGM;
import tc.oc.util.bukkit.translations.BaseTranslator;
import tc.oc.util.bukkit.translations.provider.TranslationProvider;

public final class AllTranslations extends BaseTranslator {

  private static AllTranslations instance;

  private AllTranslations() {
    super(PGM.get().getLogger(), new TranslationProvider("strings"));
  }

  public static AllTranslations get() {
    return instance == null ? instance = new AllTranslations() : instance;
  }
}
