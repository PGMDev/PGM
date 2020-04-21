package tc.oc.pgm.util.translations;

import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.util.translations.provider.TranslationProvider;

/**
 * Interface that is provides the capability of translating strings from a list of multiple {@link
 * TranslationProvider}
 */
public interface Translator {

  /**
   * Looks up for the best match in the {@link TranslationProvider}s and then translates it
   * replacing its arguments
   *
   * @param key of translation
   * @param sender who the message is being displayed
   * @param args arguments of the translation
   * @return translation with its respective arguments replaced
   */
  String translate(String key, @Nullable CommandSender sender, Object... args);

  /**
   * Looks up for the best match in the {@link TranslationProvider}s and then translates it
   * replacing its arguments
   *
   * <p>This translate method uses a {@link Function} to adapt a translation into a certain format
   *
   * @param format format to apply
   * @param key of translation
   * @param sender who the message is being displayed
   * @param args arguments of the translation
   * @return translation with its respective arguments replaced
   */
  String translate(
      Function<String, String> format, String key, @Nullable CommandSender sender, Object... args);

  /**
   * Looks up a localized pattern for the given key and audience, returning null if the viewer's
   * locale does not contain the key.
   *
   * @param key to find pattern
   * @param sender to find pattern
   * @return Pattern found
   */
  @Nullable
  String getPattern(String key, CommandSender sender);

  /**
   * Returns a {@link Set} with all of the supported {@link Locale}s
   *
   * @return {@link Set>} of {@link Locale}
   */
  Set<Locale> getSupportedLocales();

  /**
   * Returns a {@link Set} with all of the {@link TranslationProvider}s
   *
   * @return {@link Set} of {@link TranslationProvider}s
   */
  Set<TranslationProvider> getTranslationProviders();
}
