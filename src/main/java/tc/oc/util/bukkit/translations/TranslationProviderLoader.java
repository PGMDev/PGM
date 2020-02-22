package tc.oc.util.bukkit.translations;

import com.google.common.cache.CacheLoader;
import java.util.List;
import javax.annotation.Nullable;
import tc.oc.util.bukkit.translations.provider.TranslationProvider;
import tc.oc.util.localization.Locales;

/**
 * {@link CacheLoader} that finds the best match from a {@link List} of {@link TranslationProvider}s
 */
class TranslationProviderLoader extends CacheLoader<String, TranslationProvider> {

  private List<TranslationProvider> providers;

  TranslationProviderLoader(List<TranslationProvider> providers) {
    this.providers = providers;
  }

  @Override
  public TranslationProvider load(@Nullable String s) {
    TranslationProvider lastProvider = null;

    for (TranslationProvider provider : providers) {
      lastProvider = provider;

      if (provider.getString(s, Locales.DEFAULT_LOCALE) != null) {
        return provider;
      }
    }

    return lastProvider; // can't return null; if translation isn't found, pick any set
  }
}
