package tc.oc.pgm.util.translations;

import org.bukkit.Bukkit;
import tc.oc.pgm.util.translations.provider.TranslationProvider;

public final class AllTranslations extends BaseTranslator {

  private static AllTranslations instance;

  private AllTranslations() {
    super(
        Bukkit.getLogger(),
        new TranslationProvider("command"),
        new TranslationProvider("death"),
        new TranslationProvider("gamemode"),
        new TranslationProvider("join"),
        new TranslationProvider("map"),
        new TranslationProvider("match"),
        new TranslationProvider("misc"),
        new TranslationProvider("moderation"),
        new TranslationProvider("observer"));
  }

  public static AllTranslations get() {
    return instance == null ? instance = new AllTranslations() : instance;
  }
}
