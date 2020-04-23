package tc.oc.pgm.util.translation;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.annotation.Nullable;

/**
 * Provides {@link MessageFormat} translations based on {@link Locale}.
 *
 * @see #getFormat(Locale, String)
 */
public class MessageFormatProvider {

  private final Locale defaultLocale;
  private final LoadingCache<Locale, Locale> localeCache;
  private final Table<String, Locale, MessageFormat> formatTable;

  public MessageFormatProvider(String resourceName, Locale defaultLocale) {
    this.defaultLocale = checkNotNull(defaultLocale);
    this.formatTable = HashBasedTable.create();
    this.localeCache =
        CacheBuilder.newBuilder()
            .build(
                new CacheLoader<Locale, Locale>() {
                  @Override
                  public Locale load(@Nullable Locale locale) {
                    return findNearestLocale(locale);
                  }
                });

    final UTF8Control utf8 = new UTF8Control();
    for (Locale locale : Locale.getAvailableLocales()) {
      final String lang = locale.toLanguageTag().replaceAll("-", "_");
      final ResourceBundle bundle;
      try {
        final String path = locale.equals(defaultLocale) ? resourceName : resourceName + "_" + lang;
        bundle = ResourceBundle.getBundle(path, locale, utf8);
      } catch (MissingResourceException e) {
        continue;
      }

      for (String key : bundle.keySet()) {
        formatTable.put(key, locale, new MessageFormat(bundle.getString(key), locale));
      }
    }
  }

  /**
   * Get a {@link MessageFormat} for a given key and locale.
   *
   * @param locale The locale.
   * @param key The translation key.
   * @return A message format.
   */
  public MessageFormat getFormat(Locale locale, String key) {
    try {
      locale = localeCache.get(locale);
      MessageFormat format = formatTable.get(key, locale);

      if (format == null) {
        if (defaultLocale.equals(locale)) {
          // Default locale has no translation, so throw an error
          throw new NullPointerException();
        }

        // Non-default locale has no translation, so switch to the default locale
        format = getFormat(defaultLocale, key);
        formatTable.put(key, locale, format);
      }

      return format;
    } catch (Throwable t) {
      // Extra safe-guard since translations are a critical path
      return new MessageFormat("<missing translation \"" + key + "\">", locale);
    }
  }

  private Locale findNearestLocale(@Nullable Locale query) {
    if (query == null) return defaultLocale;
    if (formatTable.containsColumn(query)) return query;

    Locale matched = defaultLocale;
    int maxScore = -1;

    for (Locale locale : formatTable.columnKeySet()) {
      final int score = NEAREST_LOCALE.compare(query, locale);
      if (score > maxScore) {
        maxScore = score;
        matched = locale;
      }
    }

    return matched;
  }

  private static final Comparator<Locale> NEAREST_LOCALE =
      (Locale o1, Locale o2) -> {
        if (!o1.getLanguage().equals(o2.getLanguage())) return -1;
        if (!o1.getCountry().equals(o2.getCountry())) return 0;
        if (!o1.getVariant().equals(o2.getVariant())) return 1;
        return 2;
      };
}
