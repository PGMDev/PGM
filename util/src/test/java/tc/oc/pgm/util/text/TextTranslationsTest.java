package tc.oc.pgm.util.text;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static org.junit.jupiter.api.Assertions.*;
import static tc.oc.pgm.util.text.TextTranslations.*;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Locale;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.TranslatableComponent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public final class TextTranslationsTest {

  private static final Locale US = Locale.US;

  private static final Pointers US_POINTERS =
      Pointers.builder().withStatic(Identity.LOCALE, US).build();

  private static final Pointered POINTERED =
      new Pointered() {
        @Override
        public @NotNull Pointers pointers() {
          return US_POINTERS;
        }
      };

  @ParameterizedTest
  @ValueSource(strings = {"ENGLISH", "CANADA", "UK", "ROOT"})
  void testGetNearestLocale(Locale locale) {
    assertEquals(US, getNearestLocale(locale), "nearest locale not resolved");
  }

  @Test
  void testGetLocales() {
    final Collection<Locale> locales = getLocales();
    final Locale defaultLocale = Locale.getDefault();

    assertTrue(locales.contains(US), "source code locale not loaded");
    assertTrue(
        (locales.contains(defaultLocale) || locales.contains(getNearestLocale(defaultLocale))),
        "system locale not loaded");
  }

  @Test
  void testGetKeys() {
    assertFalse(getKeys().isEmpty(), "no keys found");
  }

  @ParameterizedTest
  @CsvSource({"misc.console,Console", "misc.ownership,{0}''s {1}", "misc.snowman,☃"})
  void testGetKey(String key, String translation) {
    final MessageFormat format = getKey(US, key);

    assertNotNull(format, "translation missing");
    assertEquals(US, format.getLocale(), "wrong locale");
    assertEquals(translation, format.toPattern(), "translation wrong");
  }

  @Test
  void testTranslateOurs() {
    assertEquals(
        text("☃"),
        translate(translatable("misc.snowman"), POINTERED),
        "translation did not render");
  }

  @Test
  void testTranslateMojang() {
    final TranslatableComponent text = translatable("entity.Creeper.name");

    assertEquals(text, translate(text, POINTERED), "mojang translation did not pass-through");
  }
}
