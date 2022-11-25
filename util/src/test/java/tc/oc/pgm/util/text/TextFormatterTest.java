package tc.oc.pgm.util.text;

import static net.kyori.adventure.text.Component.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static tc.oc.pgm.util.text.TextFormatter.*;
import static tc.oc.pgm.util.text.TextTranslations.translate;

import com.google.common.collect.ImmutableList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public final class TextFormatterTest {

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
  @CsvSource(
      value = {
        "0;''",
        "1;alice",
        "2;alice and bob",
        "3;alice, bob, and jerry",
        "4;alice, bob, jerry, and nancy"
      },
      delimiter = ';')
  void testList(int size, String expected) {
    final List<String> people = ImmutableList.of("alice", "bob", "jerry", "nancy");

    final List<Component> text = new LinkedList<>();
    for (String person : people.subList(0, size)) {
      text.add(text(person));
    }
    final Component actual = translate(list(text, NamedTextColor.WHITE), POINTERED);

    assertEquals(expected, PlainTextComponentSerializer.plainText().serialize(actual));
  }
}
