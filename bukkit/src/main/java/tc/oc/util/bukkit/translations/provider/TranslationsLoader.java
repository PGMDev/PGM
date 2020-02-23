package tc.oc.util.bukkit.translations.provider;

import com.google.common.cache.CacheLoader;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import javax.annotation.Nullable;

/** Loads a {@link Map} of translations from a certain {@link Locale} */
public class TranslationsLoader extends CacheLoader<Locale, Map<String, String>> {

  private TranslationProvider provider;

  public TranslationsLoader(TranslationProvider provider) {
    this.provider = provider;
  }

  @Override
  public Map<String, String> load(@Nullable Locale locale) {
    try {
      return provider.getTranslations(locale);
    } catch (MissingResourceException e) {
      return null;
    }
  }
}
