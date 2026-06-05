package tc.oc.pgm.platform.modern.particle.shapes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.action.replacements.Replacement;
import tc.oc.pgm.filters.Filterable;

public class TextShape implements ParticleShape {

  private static final Map<Character, List<LineShape.Line>> characterMap;
  private final String text;
  public static final int CHARACTER_WIDTH = 5;
  public static final int CHARACTER_SPACING = 1;

  @Nullable
  Map<String, Replacement> replacements;

  private static final Pattern PATTERN = Pattern.compile("\\{(.+?)}");
  private static final PlainTextComponentSerializer SERIALIZER =
      PlainTextComponentSerializer.plainText();

  public TextShape(String text, @Nullable Map<String, Replacement> replacements) {
    this.text = text;
    this.replacements = replacements;
  }

  public <T extends Filterable<?>> String resolve(T scope) {
    if (replacements == null || replacements.isEmpty()) return text;

    StringBuffer result = new StringBuffer();
    Matcher matcher = PATTERN.matcher(text);
    while (matcher.find()) {
      Replacement r = replacements.get(matcher.group(1));
      String replacement =
          r != null ? SERIALIZER.serialize(r.get(scope).asComponent()) : matcher.group();
      matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(result);
    return result.toString();
  }

  static {
    Map<Character, int[]> nodeMap = new HashMap<>();

    //  UPPERCASE
    nodeMap.put('A', new int[] {0, 0, 0, 5, 1, 6, 3, 6, 1, 4, 3, 4, 4, 0, 4, 5});
    nodeMap.put(
        'B', new int[] {0, 0, 0, 6, 1, 0, 3, 0, 1, 5, 3, 5, 1, 6, 3, 6, 4, 1, 4, 3, 4, 5, 4, 5});
    nodeMap.put('C', new int[] {1, 6, 3, 6, 0, 1, 0, 5, 1, 0, 3, 0});
    nodeMap.put('D', new int[] {0, 0, 0, 6, 1, 6, 3, 6, 4, 1, 4, 5, 1, 0, 3, 0});
    nodeMap.put('E', new int[] {0, 0, 0, 6, 1, 6, 4, 6, 1, 3, 3, 3, 1, 0, 4, 0});
    nodeMap.put('F', new int[] {0, 0, 0, 5, 1, 4, 2, 4, 1, 6, 4, 6});
    nodeMap.put('G', new int[] {0, 1, 0, 5, 1, 0, 4, 0, 1, 6, 3, 6, 4, 1, 4, 4, 3, 4, 3, 4});
    nodeMap.put('H', new int[] {0, 0, 0, 6, 4, 0, 4, 6, 1, 4, 3, 4});
    nodeMap.put('I', new int[] {1, 0, 3, 0, 1, 6, 3, 6, 2, 1, 2, 5});
    nodeMap.put('J', new int[] {0, 1, 0, 1, 1, 0, 3, 0, 4, 1, 4, 6});
    nodeMap.put(
        'K', new int[] {0, 0, 0, 6, 4, 0, 4, 2, 3, 3, 3, 3, 1, 4, 2, 4, 3, 5, 3, 5, 4, 6, 4, 6});
    nodeMap.put('L', new int[] {0, 0, 0, 6, 1, 0, 4, 0});
    nodeMap.put('M', new int[] {0, 0, 0, 6, 4, 0, 4, 6, 1, 5, 1, 5, 2, 4, 2, 4, 3, 5, 3, 5});
    nodeMap.put('N', new int[] {0, 0, 0, 6, 4, 0, 4, 6, 1, 5, 1, 5, 2, 4, 2, 4, 3, 3, 3, 3});
    nodeMap.put('O', new int[] {1, 0, 3, 0, 0, 1, 0, 5, 4, 1, 4, 5, 1, 6, 4, 6});
    nodeMap.put('P', new int[] {0, 0, 0, 6, 1, 6, 3, 6, 1, 4, 3, 4, 4, 5, 4, 5});
    nodeMap.put(
        'Q', new int[] {0, 1, 0, 5, 1, 0, 2, 0, 1, 6, 3, 6, 4, 2, 4, 5, 3, 1, 3, 1, 4, 0, 4, 0});
    nodeMap.put('R', new int[] {0, 0, 0, 6, 1, 6, 3, 6, 1, 4, 3, 4, 4, 5, 4, 5, 4, 0, 4, 3});
    nodeMap.put(
        'S', new int[] {0, 1, 0, 1, 1, 0, 3, 0, 4, 1, 4, 3, 1, 4, 3, 4, 0, 5, 0, 5, 1, 6, 4, 6});
    nodeMap.put('T', new int[] {0, 6, 4, 6, 2, 0, 2, 5});
    nodeMap.put('U', new int[] {0, 1, 0, 6, 1, 0, 3, 0, 4, 1, 4, 6});
    nodeMap.put('V', new int[] {2, 0, 2, 0, 1, 1, 1, 2, 0, 3, 0, 6, 3, 1, 3, 2, 4, 3, 4, 6});
    nodeMap.put('W', new int[] {0, 0, 0, 6, 4, 0, 4, 6, 1, 1, 1, 1, 2, 2, 2, 2, 3, 1, 3, 1});
    nodeMap.put('X', new int[] {
      0, 0, 0, 2, 1, 3, 1, 3, 2, 4, 2, 4, 3, 3, 3, 3, 4, 0, 4, 2, 0, 6, 0, 6, 1, 5, 1, 5, 3, 5, 3,
      5, 4, 6, 4, 6
    });
    nodeMap.put('Y', new int[] {2, 0, 2, 4, 0, 6, 0, 6, 1, 5, 1, 5, 3, 5, 3, 5, 4, 6, 4, 6});
    nodeMap.put('Z', new int[] {
      0, 0, 4, 0, 0, 6, 4, 6, 0, 1, 0, 1, 1, 2, 1, 2, 2, 3, 2, 3, 3, 4, 3, 4, 4, 5, 4, 5
    });

    //  NUMBERS
    //    nodeMap.put('0', new int[]{ });
    nodeMap.put('1', new int[] {0, 0, 4, 0, 2, 1, 2, 6, 1, 5, 1, 5});
    nodeMap.put('2', new int[] {
      0, 0, 4, 0, 0, 1, 0, 1, 4, 1, 4, 1, 1, 2, 1, 2, 2, 3, 3, 3, 4, 4, 4, 5, 0, 5, 0, 5, 1, 6, 3, 6
    });
    nodeMap.put('3', new int[] {
      1, 0, 3, 0, 0, 1, 0, 1, 4, 1, 4, 2, 2, 3, 3, 3, 4, 4, 4, 5, 0, 5, 0, 5, 1, 6, 3, 6
    });
    nodeMap.put(
        '4', new int[] {4, 0, 4, 6, 0, 2, 3, 2, 0, 3, 0, 3, 1, 4, 1, 4, 2, 5, 2, 5, 3, 6, 3, 6});
    nodeMap.put(
        '5', new int[] {1, 0, 3, 0, 4, 1, 4, 3, 0, 1, 0, 1, 0, 4, 3, 4, 0, 5, 0, 5, 0, 6, 4, 6});
    //    nodeMap.put('6', new int[] { });
    //    nodeMap.put('7', new int[] { });
    //    nodeMap.put('8', new int[] { });
    //    nodeMap.put('9', new int[] { });
    Map<Character, List<LineShape.Line>> tempMap = new HashMap<>();

    for (Map.Entry<Character, int[]> entry : nodeMap.entrySet()) {
      char character = entry.getKey();
      int[] pos = entry.getValue();
      List<LineShape.Line> lines = new ArrayList<>();

      for (int i = 0; i < pos.length; i += 4) {
        Vector origin = new Vector(pos[i], 0, pos[i + 1]);
        Vector destination = new Vector(pos[i + 2], 0, pos[i + 3]);
        lines.add(new LineShape.Line(origin, destination));
      }
      tempMap.put(character, lines);
    }
    characterMap = Map.copyOf(tempMap);
  }

  @Override
  public List<Vector> getNodes(int amount) {
    List<Vector> nodes = new ArrayList<>();
    for (int charIndex = 0; charIndex < text.length(); charIndex++) {
      char c = text.charAt(charIndex);
      List<LineShape.Line> charLines = characterMap.get(c);
      if (charLines == null) continue;

      int xOffset = charIndex * (CHARACTER_WIDTH + CHARACTER_SPACING);
      int nodesPerLine = amount / charLines.size();

      for (LineShape.Line line : charLines) {
        Vector direction = line.destination().clone().subtract(line.origin());
        for (int i = 0; i <= nodesPerLine; i++) {
          double progress = (double) i / nodesPerLine;
          Vector node = line.origin().clone().add(direction.clone().multiply(progress));
          node.setX(node.getX() + xOffset);
          nodes.add(node);
        }
      }
    }
    return nodes;
  }
}

//  LOWERCASE
//    nodeMap.put('a', new int[]{ });
//    nodeMap.put('b', new int[]{ });
//    nodeMap.put('c', new int[]{ });
//    nodeMap.put('d', new int[]{ });
//    nodeMap.put('e', new int[]{ });
//    nodeMap.put('f', new int[]{ });
//    nodeMap.put('g', new int[]{ });
//    nodeMap.put('h', new int[]{ });
//    nodeMap.put('i', new int[]{ });
//    nodeMap.put('j', new int[]{ });
//    nodeMap.put('k', new int[]{ });
//    nodeMap.put('l', new int[]{ });
//    nodeMap.put('m', new int[]{ });
//    nodeMap.put('n', new int[]{ });
//    nodeMap.put('o', new int[]{ });
//    nodeMap.put('p', new int[]{ });
//    nodeMap.put('q', new int[]{ });
//    nodeMap.put('r', new int[]{ });
//    nodeMap.put('s', new int[]{ });
//    nodeMap.put('t', new int[]{ });
//    nodeMap.put('u', new int[]{ });
//    nodeMap.put('v', new int[]{ });
//    nodeMap.put('w', new int[]{ });
//    nodeMap.put('x', new int[]{ });
//    nodeMap.put('y', new int[]{ });
//    nodeMap.put('z', new int[]{ });

//  SYMBOLS
//    nodeMap.put('_', new int[]{ });
