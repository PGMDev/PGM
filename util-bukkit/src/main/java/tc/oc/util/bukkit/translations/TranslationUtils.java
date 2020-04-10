package tc.oc.util.bukkit.translations;

import com.google.common.collect.Collections2;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.bukkit.command.CommandSender;
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.component.Components;
import tc.oc.util.bukkit.component.types.PersonalizedTranslatable;
import tc.oc.util.bukkit.named.NameStyle;
import tc.oc.util.bukkit.named.Named;
import tc.oc.util.bukkit.translations.provider.TranslationProvider;

public class TranslationUtils {

  private static final Translator TRANSLATOR = AllTranslations.get();

  /**
   * Lookup a localized pattern for the given key and viewer, returning an error message if the
   * viewer's locale does not contain the key.
   *
   * @param key pattern to find
   * @param sender command sender
   * @return Pattern
   */
  public static String safePattern(String key, CommandSender sender) {
    String pattern = TRANSLATOR.getPattern(key, sender);
    return pattern != null ? pattern : "<missing pattern " + key + ">";
  }

  /**
   * Checks if a certain key exists in a certain {@link Locale}
   *
   * @param key to find
   * @return key exists
   */
  public static boolean hasKey(Locale locale, String key) {
    for (TranslationProvider translations : TRANSLATOR.getTranslationProviders()) {
      if (translations.hasKey(locale, key)) return true;
    }
    return false;
  }

  /**
   * Checks if a certain key exists in the {@link Locales#DEFAULT_LOCALE}
   *
   * @param key to find
   * @return key exists
   */
  public static boolean hasKey(String key) {
    return hasKey(Locales.DEFAULT_LOCALE, key);
  }

  /**
   * Returns a {@link NavigableSet} set of {@link String}s which contain all keys that start with a
   * certain prefix inside a certain {@link Locale}
   *
   * @param locale containing the keys
   * @param prefix used to find keys
   * @return {@link NavigableSet} of keys
   */
  public static NavigableSet<String> getKeys(Locale locale, @Nullable String prefix) {
    NavigableSet<String> keys = new TreeSet<>();
    for (TranslationProvider translations : TRANSLATOR.getTranslationProviders()) {
      for (String key : translations.getKeys(locale)) {
        if (prefix == null || key.startsWith(prefix)) keys.add(key);
      }
    }
    return keys;
  }

  /**
   * Returns a {@link NavigableSet} set of {@link String}s which contain all keys that start with a
   * certain prefix inside the {@link Locales#DEFAULT_LOCALE} locale
   *
   * @param prefix to look up keys
   * @return {@link NavigableSet} of keys
   */
  public static NavigableSet<String> getKeys(@Nullable String prefix) {
    return getKeys(Locales.DEFAULT_LOCALE, prefix);
  }

  /**
   * Combines a list of {@link Component} into a single {@link Component}
   *
   * @param elements to combine
   * @return combined {@link Component}
   */
  public static Component combineComponents(Collection<? extends Component> elements) {
    switch (elements.size()) {
      case 0:
        return Components.blank();
      case 1:
        return elements.iterator().next();
      case 2:
        return new PersonalizedTranslatable("misc.list.pair", elements.toArray());
      default:
        Iterator<? extends Component> iter = elements.iterator();
        Component a = new PersonalizedTranslatable("misc.list.start", iter.next(), iter.next());
        Component b = iter.next();
        while (iter.hasNext()) {
          a = new PersonalizedTranslatable("misc.list.middle", a, b);
          b = iter.next();
        }
        return new PersonalizedTranslatable("misc.list.end", a, b);
    }
  }

  /**
   * Combines a list of {@link Component} into a single {@link Component}
   *
   * @param elements to combine
   * @return combined {@link Component}
   */
  public static Component combineComponents(Component... elements) {
    return combineComponents(Arrays.asList(elements));
  }

  /**
   * Combines a {@link Collection} of type {@link T} in to a {@link Component} using a {@link
   * Function} to transform {@link T} to a base component
   *
   * @param collection to combine
   * @param function to transform the type
   * @return combined {@link Component}
   */
  public static <T> Component combineComponents(
      Collection<T> collection, Function<T, Component> function) {
    return combineComponents(Collections2.transform(collection, function::apply));
  }

  /**
   * Combines a {@link Collection} of {@link Named} and applies a {@link NameStyle} to each name
   *
   * @param style to apply
   * @param names to combine
   * @return {@link Component} with the names with the style applied
   */
  public static Component nameList(NameStyle style, Collection<? extends Named> names) {
    switch (names.size()) {
      case 0:
        return Components.blank();
      case 1:
        return names.iterator().next().getStyledName(style);
      default:
        return combineComponents(
            Collections2.transform(names, named -> named.getStyledName(style)));
    }
  }

  /**
   * Combines a {@link Collection} of elements into a localized list
   *
   * @param viewer command sender
   * @param format format to apply
   * @param elementFormat element format
   * @param elements elements to apply format to
   * @return String with the list of elements combined and their new format
   */
  public static String legacyList(
      CommandSender viewer,
      Function<String, String> format,
      Function<String, String> elementFormat,
      Collection<?> elements) {
    switch (elements.size()) {
      case 0:
        return "";
      case 1:
        return elementFormat.apply(elements.iterator().next().toString());
      case 2:
        Iterator<?> pair = elements.iterator();
        return TRANSLATOR.translate(
            format,
            "misc.list.pair",
            viewer,
            elementFormat.apply(pair.next().toString()),
            elementFormat.apply(pair.next().toString()));
      default:
        Iterator<?> iter = elements.iterator();
        String a =
            TRANSLATOR.translate(
                format,
                "misc.list.start",
                viewer,
                elementFormat.apply(iter.next().toString()),
                elementFormat.apply(iter.next().toString()));
        String b = elementFormat.apply(iter.next().toString());
        while (iter.hasNext()) {
          a = TRANSLATOR.translate(format, "misc.list.middle", viewer, a, b);
          b = elementFormat.apply(iter.next().toString());
        }
        return TRANSLATOR.translate(format, "misc.list.end", viewer, a, b);
    }
  }
}
