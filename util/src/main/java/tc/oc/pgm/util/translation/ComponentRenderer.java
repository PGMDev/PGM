package tc.oc.pgm.util.translation;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.RowSortedTable;
import com.google.common.collect.TreeBasedTable;
import java.text.MessageFormat;
import java.util.*;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.renderer.TranslatableComponentRenderer;

/** Renders {@link Component}s based on a {@link Locale}. */
public final class ComponentRenderer extends TranslatableComponentRenderer<Locale> {

  public static final ComponentRenderer INSTANCE = new ComponentRenderer("strings", Locale.US);

  private final Locale defaultLocale;
  private final LoadingCache<Locale, Locale> localeCache;
  private final RowSortedTable<String, Locale, MessageFormat> formatTable;

  private ComponentRenderer(String resourceName, Locale defaultLocale) {
    this.defaultLocale = checkNotNull(defaultLocale);
    this.formatTable =
        TreeBasedTable.create(
            String::compareToIgnoreCase, Comparator.comparing(Locale::toLanguageTag));
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
        // Single quotes are a special escape keyword for MessageFormat, so they need to be escaped
        final String format = bundle.getString(key).replaceAll("'", "''");

        formatTable.put(key, locale, new MessageFormat(format, locale));
      }
    }
  }

  public SortedSet<String> getKeys() {
    return formatTable.rowKeySet();
  }

  @Override
  protected MessageFormat translation(Locale locale, String key) {
    try {
      locale = localeCache.get(locale);
      MessageFormat format = formatTable.get(key, locale);

      if (format == null) {
        if (defaultLocale.equals(locale)) {
          // Default locale has no translation, so throw an error
          throw new NullPointerException();
        }

        // Non-default locale has no translation, so switch to the default locale
        format = translation(defaultLocale, key);
        formatTable.put(key, locale, format);
      }

      return format;
    } catch (Throwable t) {
      return null; // Fallback to a client translation, provided by Mojang
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
