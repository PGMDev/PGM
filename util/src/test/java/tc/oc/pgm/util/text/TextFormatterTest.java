package tc.oc.pgm.util.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tc.oc.pgm.util.text.TextFormatter.*;

import com.google.common.collect.ImmutableList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.plain.PlainComponentSerializer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public final class TextFormatterTest {

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
      text.add(TextComponent.of(person));
    }
    final Component actual = TextTranslations.translate(list(text, TextColor.WHITE), Locale.US);

    assertEquals(expected, PlainComponentSerializer.INSTANCE.serialize(actual));
  }
}
