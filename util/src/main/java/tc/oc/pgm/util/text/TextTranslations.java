package tc.oc.pgm.util.text;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.renderer.TranslatableComponentRenderer;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

/** A singleton for accessing {@link MessageFormat} and {@link Component} translations. */
@SuppressWarnings("UnstableApiUsage")
public final class TextTranslations {
  private TextTranslations() {}

  // Locale of the source code .properties files
  private static final Locale SOURCE_LOCALE = Locale.US;

  // A control to ensure that .properties are loaded in UTF-8 format
  private static final UTF8Control SOURCE_CONTROL = new UTF8Control();

  // An list of all .properties files to load
  private static final List<String> SOURCE_NAMES =
      ImmutableList.of(
          "command",
          "death",
          "error",
          "gamemode",
          "join",
          "map",
          "match",
          "misc",
          "moderation",
          "ui");

  // A table of all keys mapped to their locale and message format (*not* thread safe)
  private static final SortedMap<String, Map<Locale, MessageFormat>> TRANSLATIONS_MAP =
      new Object2ObjectAVLTreeMap<>(String::compareToIgnoreCase);
  private static final Table<String, Locale, MessageFormat> TRANSLATIONS_TABLE =
      Tables.newCustomTable(
          TRANSLATIONS_MAP,
          () ->
              new Object2ObjectLinkedOpenHashMap<>(
                  Hash.DEFAULT_INITIAL_SIZE, Hash.FAST_LOAD_FACTOR));

  // A cache of locales that are close enough
  private static final Map<Locale, Locale> LOCALES =
      new Object2ObjectLinkedOpenHashMap<>(Hash.DEFAULT_INITIAL_SIZE, Hash.VERY_FAST_LOAD_FACTOR);

  // A component renderer that uses the nearest key
  private static final TranslatableComponentRenderer<Locale> RENDERER =
      TranslatableComponentRenderer.from(TextTranslations::getNearestKey);

  static {
    // If the source locale has no text translations, consider this a fatal error
    checkArgument(
        loadKeys(SOURCE_LOCALE) > 0,
        "no text translations found (are .properties files being included properly?)");
    // Attempt to pre-fetch the locale of the console, but if not present, is not a fatal error
    loadKeys(Locale.getDefault());
  }

  /**
   * Gets all translation keys.
   *
   * @return A sorted set of keys.
   */
  public static SortedSet<String> getKeys() {
    return (SortedSet<String>) TRANSLATIONS_MAP.keySet();
  }

  /**
   * Gets all locales with translations.
   *
   * @return A set of locales.
   */
  public static Set<Locale> getLocales() {
    return TRANSLATIONS_TABLE.columnKeySet();
  }

  /**
   * Gets the "nearest" locale with translations.
   *
   * <p>For example, if there are no translations for "en_CA", "en_US" should be close enough.
   *
   * @param locale A locale.
   * @return A locale with translations.
   */
  public static Locale getNearestLocale(Locale locale) {
    if (locale == SOURCE_LOCALE) return locale;

    Locale nearest = LOCALES.get(locale);
    if (nearest != null || loadKeys(locale) < 0) return nearest;

    int maxScore = 0;
    for (Locale other : getLocales()) {
      int score =
          (locale.getLanguage().equals(other.getLanguage()) ? 3 : 0)
              + (locale.getCountry().equals(other.getCountry()) ? 2 : 0)
              + (locale.getVariant().equals(other.getVariant()) ? 1 : 0);
      if (score > maxScore) {
        maxScore = score;
        nearest = other;
      }
    }

    LOCALES.put(locale, nearest);
    return nearest;
  }

  /**
   * Gets a translated message format.
   *
   * @param locale A locale.
   * @param key A translation key.
   * @return A message format, or null if not found.
   */
  @Nullable
  public static MessageFormat getKey(Locale locale, String key) {
    return TRANSLATIONS_TABLE.get(key, locale);
  }

  /**
   * Gets a translated message format, fallback is English.
   *
   * @param locale A locale.
   * @param key A translation key.
   * @return A message format, or null if not found.
   */
  @Nullable
  public static MessageFormat getNearestKey(Locale locale, String key) {
    final Locale nearestLocale = getNearestLocale(locale);
    final MessageFormat format = getKey(nearestLocale, key);
    if (format != null || nearestLocale == SOURCE_LOCALE) return format;

    // If the format is also missing from the source locale, it is likely an external
    // translation, typically one provided by Mojang for item and block translations.
    return getKey(SOURCE_LOCALE, key);
  }

  /**
   * Loads translation keys of a locale.
   *
   * @param locale A locale.
   * @return The number of keys found, or 0 if already loaded.
   */
  public static long loadKeys(Locale locale) {
    if (getLocales().contains(locale)) return 0;

    long keysFound = 0;
    for (String resourceName : SOURCE_NAMES) {
      // If the locale is not the source code locale,
      // then append the language tag to get the proper resource
      if (locale != SOURCE_LOCALE)
        resourceName += "_" + locale.toLanguageTag().replaceAll("-", "_");

      final ResourceBundle resource;
      try {
        resource = ResourceBundle.getBundle(resourceName, locale, SOURCE_CONTROL);
      } catch (MissingResourceException e) {
        continue;
      }

      for (String key : resource.keySet()) {
        String format = resource.getString(key);

        // Single quotes are a special keyword that need to be escaped in MessageFormat
        // Templates are not escaped, where as translations are escaped
        if (locale == SOURCE_LOCALE) format = format.replaceAll("'", "''");

        TRANSLATIONS_TABLE.put(key, locale, new MessageFormat(format, locale));
        keysFound++;
      }
    }

    // Clear locale cache when a new locale is loaded
    if (keysFound > 0) {
      LOCALES.clear();
    }

    return keysFound;
  }

  /**
   * Gets a translated text component.
   *
   * @param text The text.
   * @param locale A locale.
   * @return The translated text.
   */
  public static Component translate(Component text, Locale locale) {
    return RENDERER.render(text, locale);
  }

  /**
   * Gets a translated text in legacy format.
   *
   * @param text The text.
   * @param sender A command sender or null.
   * @return The translated legacy text.
   */
  @Deprecated
  public static String translateLegacy(Component text, @Nullable CommandSender sender) {
    return LegacyComponentSerializer.legacy()
        .serialize(translate(text, sender == null ? SOURCE_LOCALE : sender.getLocale()));
  }

  /**
   * Gets a translated legacy text.
   *
   * @param key A translation key.
   * @param sender A command sender, or null for the source locale.
   * @param args Optional array of arguments.
   * @return A legacy text.
   * @see #translate(Component, Locale) for the newer text system.
   */
  @Deprecated
  public static String translate(String key, @Nullable CommandSender sender, Object... args) {
    final Locale locale = sender == null ? SOURCE_LOCALE : sender.getLocale();
    final Component text =
        TranslatableComponent.of(
            key,
            Stream.of(args)
                .map(String::valueOf)
                .map(TextComponent::of)
                .collect(Collectors.toList()));

    return LegacyComponentSerializer.legacy().serialize(translate(text, locale));
  }
}
