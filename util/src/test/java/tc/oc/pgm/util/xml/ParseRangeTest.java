package tc.oc.pgm.util.xml;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Namespace;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tc.oc.pgm.util.Pair;

public final class ParseRangeTest {

  private static Node dummyNode(String value) {
    Document doc = new Document();
    InheritingElement el = new InheritingElement("a");
    Attribute attr = new Attribute("range", value, Namespace.NO_NAMESPACE);
    el.setAttribute(attr);
    doc.setRootElement(el);
    return new Node(attr);
  }

  @Test
  public void testParsedMathematicalRanges() throws InvalidXMLException {
    final List<Pair<String, Range<Integer>>> mathematicalFormatRanges = Lists.newArrayList(
        Pair.of("[1,5]", Range.closed(1, 5)),
        Pair.of("[1,oo)", Range.atLeast(1)),
        Pair.of("(-oo,5]", Range.atMost(5)),
        Pair.of("(1,20)", Range.open(1, 20)),
        Pair.of("(1,oo)", Range.greaterThan(1)),
        Pair.of("(-oo,80)", Range.lessThan(80)),
        Pair.of("(-oo,oo)", Range.all()),
        Pair.of("(1,5]", Range.openClosed(1, 5)),
        Pair.of("[1,5)", Range.closedOpen(1, 5)));

    for (Pair<String, Range<Integer>> mathematicalFormatRange : mathematicalFormatRanges) {
      Range<Integer> parsedRange =
          XMLUtils.parseNumericRange(dummyNode(mathematicalFormatRange.getLeft()), Integer.class);

      Assertions.assertEquals(mathematicalFormatRange.getRight(), parsedRange);
    }
  }

  @Test
  public void testParsedVanillaRanges() throws InvalidXMLException {
    final List<Pair<String, Range<Double>>> vanillaFormatRanges = Lists.newArrayList(
        Pair.of("1..5", Range.closed(1D, 5D)),
        Pair.of("  1  ..5    ", Range.closed(1D, 5D)),
        Pair.of("1..", Range.atLeast(1D)),
        Pair.of("..5", Range.atMost(5D)),
        Pair.of("-oo..1", Range.atMost(1D)),
        Pair.of("1..oo", Range.atLeast(1D)),
        Pair.of("-oo..oo", Range.all()),
        Pair.of("-oo ..", Range.all()),
        Pair.of(" ..oo", Range.all()),
        Pair.of(".5..5", Range.closed(0.5D, 5D)),
        Pair.of("0.5  ..5 ", Range.closed(0.5D, 5D)),
        Pair.of("..", Range.all()));

    for (Pair<String, Range<Double>> vanillaFormatRange : vanillaFormatRanges) {
      Range<Double> parsedRange =
          XMLUtils.parseNumericRange(dummyNode(vanillaFormatRange.getLeft()), Double.class);

      Assertions.assertEquals(vanillaFormatRange.getRight(), parsedRange);
    }
  }

  @Test
  public void testFaultyVanillaRanges() {
    final List<String> faultyRanges = Lists.newArrayList(
        "oo..1",
        "oo..oo",
        "5..1",
        "oo..oo",
        "-oo..-oo",
        "oo..-oo",
        "4..-oo",
        "===..5",
        "3..###",
        "3..wow",
        "amazing..3");

    for (String faultyRange : faultyRanges) {
      AtomicReference<Range<Integer>> faultyResult = new AtomicReference<>(null);
      Assertions.assertThrows(
          InvalidXMLException.class,
          () -> faultyResult.set(XMLUtils.parseNumericRange(dummyNode(faultyRange), Integer.class)),
          () -> "\"" + faultyRange + "\"" + " parsed faulty to result: " + faultyResult.get());
    }
  }

  @Test
  public void testBoundedRanges() {
    final List<String> boundedRanges =
        Lists.newArrayList("1..5", "[0,3]", "-1..2", "[-5,8]", "5", "(3, 5)", "(-4, 8]", "[2, 10)");

    final List<String> nonBoundedRanges =
        Lists.newArrayList("-oo..5", "(-oo,8]", "5..oo", "(5,oo)", "-oo,8]");

    for (String boundedRange : boundedRanges) {
      Assertions.assertDoesNotThrow(
          () -> XMLUtils.parseBoundedNumericRange(dummyNode(boundedRange), Integer.class));
    }

    for (String nonBoundedRange : nonBoundedRanges) {
      AtomicReference<Range<Integer>> faultyResult = new AtomicReference<>(null);
      Assertions.assertThrows(
          InvalidXMLException.class,
          () -> faultyResult.set(
              XMLUtils.parseBoundedNumericRange(dummyNode(nonBoundedRange), Integer.class)),
          () -> "\"" + nonBoundedRange + "\"" + " parsed faulty to result: " + faultyResult.get());
    }
  }
}
