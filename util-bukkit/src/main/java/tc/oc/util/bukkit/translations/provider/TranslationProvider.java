package tc.oc.util.bukkit.translations.provider;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import java.text.MessageFormat;
import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import tc.oc.util.bukkit.translations.Locales;

/** A class retrieves translations from a {@link ResourceBundle} */
@SuppressWarnings("UnstableApiUsage")
public class TranslationProvider {

  private final LoadingCache<Locale, Map<String, String>> cachedLocales;
  private String name;

  public TranslationProvider(String name) {
    this(name, Locales.DEFAULT_LOCALE);
  }

  public TranslationProvider(String name, @Nullable Locale... initialLocales) {
    this.name = checkNotNull(name, "Name");
    this.cachedLocales = CacheBuilder.newBuilder().build(new TranslationsLoader(this));

    if (initialLocales != null) {
      for (Locale locale : initialLocales) {
        if (locale != null) {
          this.cachedLocales.refresh(locale);
        }
      }
    }
  }

  /**
   * Retrieves a pattern identified by a certain key in a certain {@link Locale}
   *
   * @param key where the pattern is located
   * @param locale where pattern is located
   * @return pattern
   */
  @Nullable
  public String getPattern(String key, Locale locale) {
    try {
      key = checkNotNull(key, "Key");
      return checkNotNull(this.cachedLocales.get(locale).get(key)).replace("'", "''");
    } catch (ExecutionException | NullPointerException ignored) {
      return null;
    }
  }

  /**
   * Retrieves a string by a certain key that is located in a certain {@link Locale}.
   *
   * <p>Arguments can be passed to the string so they are replaced
   *
   * @param key where string is located
   * @param locale where the string is located
   * @param arguments to pass to the string
   * @return {@link String} with the replaced arguments
   */
  @Nullable
  public String getString(String key, Locale locale, @Nullable Object... arguments) {
    String translation = this.getPattern(key, locale);
    if (translation == null) {
      return null;
    }
    return arguments != null ? MessageFormat.format(translation, arguments) : translation;
  }

  /**
   * Returns whether a certain {@link Locale} has a certain key
   *
   * @return if the locale has the key
   */
  public boolean hasKey(Locale locale, String key) {
    return getTranslations(locale).containsKey(key);
  }

  /**
   * Returns whether the {@link Locales#DEFAULT_LOCALE} has a certain key
   *
   * @return if the {@link Locales#DEFAULT_LOCALE} has the key
   */
  public boolean hasKey(String key) {
    return hasKey(Locales.DEFAULT_LOCALE, key);
  }

  /**
   * Returns a {@link Map} containing all the translations from a certain {@link Locale}
   *
   * @param locale to get translations from
   * @return {@link Map} of translations
   */
  public Map<String, String> getTranslations(Locale locale) {
    ResourceBundle bundle = ResourceBundle.getBundle(getName(), locale);
    return Collections.list(bundle.getKeys()).stream()
        .map(key -> new SimpleImmutableEntry<>(key, bundle.getString(key)))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  /**
   * Gets a list of keys from a certain {@link Locale}
   *
   * @param locale to get keys from
   * @return {@link Set} of {@link String}s with the keys
   */
  public Set<String> getKeys(Locale locale) {
    return getTranslations(locale).keySet();
  }

  /**
   * Gets a list of keys from the {@link Locales#DEFAULT_LOCALE}
   *
   * @return {@link Set} of {@link String}s with the keys
   */
  public Set<String> getKeys() {
    return getKeys(Locales.DEFAULT_LOCALE);
  }

  /**
   * Gets the name of the {@link TranslationProvider}
   *
   * @return name
   */
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    try {
      return this.name + "={" + cachedLocales.get(Locales.DEFAULT_LOCALE) + "}";
    } catch (ExecutionException e) {
      return this.name;
    }
  }
}
