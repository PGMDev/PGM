package tc.oc.pgm.util.text;

import static org.junit.jupiter.api.Assertions.*;
import static tc.oc.pgm.util.text.TextTranslations.*;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Locale;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public final class TextTranslationsTest {

  private static final Locale US = Locale.US;

  @Test
  void testGetLocales() {
    final Collection<Locale> locales = getLocales();

    assertTrue(locales.contains(US), "source code locale not loaded");
    assertTrue(locales.contains(Locale.getDefault()), "system locale not loaded");
  }

  @ParameterizedTest
  @ValueSource(strings = {"ENGLISH", "CANADA", "UK", "ROOT"})
  void testGetNearestLocale(Locale locale) {
    assertEquals(US, getNearestLocale(locale), "nearest locale not resolved");
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
        TextComponent.of("☃"),
        translate(TranslatableComponent.of("misc.snowman"), US),
        "translation did not render");
  }

  @Test
  void testTranslateMojang() {
    final TranslatableComponent text = TranslatableComponent.of("entity.Creeper.name");

    assertEquals(text, translate(text, US), "mojang translation did not pass-through");
  }
}
