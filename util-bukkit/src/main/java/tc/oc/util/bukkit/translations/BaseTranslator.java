package tc.oc.util.bukkit.translations;

import static java.util.Objects.requireNonNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList.Builder;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.command.CommandSender;
import tc.oc.util.bukkit.translations.provider.TranslationProvider;

/** A base implementation of a {@link Translator} */
@SuppressWarnings("UnstableApiUsage")
public class BaseTranslator implements Translator {

  private final Logger logger;
  private final LoadingCache<String, TranslationProvider> providerCache;
  private final LocaleMatcher localeMatcher;
  private final Set<Locale> supportedLocales;
  private List<TranslationProvider> translationProviders;

  /**
   * Default constructor
   *
   * @param logger to log errors
   * @param providers that contain translations
   */
  public BaseTranslator(Logger logger, TranslationProvider... providers) {
    this.logger = logger;

    Builder<TranslationProvider> builder = new Builder<>();
    for (TranslationProvider set : providers) {
      builder.add(set);
    }

    this.translationProviders = builder.build();
    this.providerCache =
        CacheBuilder.newBuilder().build(new TranslationProviderLoader(this.translationProviders));
    this.supportedLocales = Locales.getSupportedLocales(this.translationProviders);
    this.localeMatcher = new LocaleMatcher(Locales.DEFAULT_LOCALE, this.supportedLocales);
  }

  /**
   * Constructs a {@link Translator} using the providers from other {@link Translator}
   *
   * @param logger to log errors
   * @param translators that contain providers
   */
  public BaseTranslator(Logger logger, Translator... translators) {
    this(
        logger,
        Arrays.stream(translators)
            .flatMap(translator -> translator.getTranslationProviders().stream())
            .toArray(TranslationProvider[]::new));
  }

  private String getString(String key, Locale locale, Object... args) throws ExecutionException {
    key = requireNonNull(key, "Key");
    TranslationProvider provider = requireNonNull(providerCache.get(key), "TranslationProvider");
    return provider.getString(key, localeMatcher.closestMatchFor(locale), args);
  }

  @Override
  public String translate(String key, @Nullable CommandSender sender, Object... args) {
    if (args != null && args.length < 1) {
      args = null;
    }

    try {
      return Optional.ofNullable(getString(key, Locales.getLocale(sender), args))
          .orElse("<translation '" + key + "' missing>");
    } catch (ExecutionException | NullPointerException exception) {
      return "<error retrieving translation for '" + key + "'>";
    } catch (IllegalArgumentException exception) {
      if (args != null) {
        return translate(key, sender, Arrays.copyOfRange(args, 1, args.length));
      } else {
        return "<error formatting string for '" + key + "'>";
      }
    }
  }

  @Override
  public String translate(
      Function<String, String> format, String key, @Nullable CommandSender sender, Object... args) {
    for (int i = 0; i < args.length; i++) {
      args[i] = args[i] + format.apply("");
    }
    return format.apply(this.translate(key, sender, args));
  }

  @Nullable
  @Override
  public String getPattern(String key, CommandSender sender) {
    try {
      return providerCache
          .get(key)
          .getPattern(key, localeMatcher.closestMatchFor(Locales.getLocale(sender)));
    } catch (ExecutionException e) {
      logger.log(Level.SEVERE, "Exception getting pattern for key " + key, e);
      return null;
    }
  }

  @Override
  public Set<Locale> getSupportedLocales() {
    return supportedLocales;
  }

  @Override
  public Set<TranslationProvider> getTranslationProviders() {
    return new HashSet<>(translationProviders);
  }
}
