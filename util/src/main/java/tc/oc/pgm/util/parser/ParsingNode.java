package tc.oc.pgm.util.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ParsingNode {
  private int start = -1;
  private String base = "";
  private List<ParsingNode> children;
  private int end = -1;

  private ParsingNode() {}

  public static ParsingNode parse(String string) throws SyntaxException {
    StringReader reader = new StringReader(string);
    ParsingNode node = new ParsingNode();
    node.parse(reader);
    reader.done();
    return node;
  }

  public int getStart() {
    return start;
  }

  public int getEnd() {
    return end;
  }

  public void parse(StringReader str) {
    str.skipWhitespace();
    this.start = str.getPosition();
    this.base = str.readText();

    // Standalone leaf node! a raw value with no params
    if (!str.pollIf('(')) {
      this.children = null;
    } else {
      this.base = this.base.trim();
      this.children = new ArrayList<>();

      // 0 parameter function
      if (!str.pollIf(')')) {
        while (true) {
          ParsingNode node = new ParsingNode();
          node.parse(str);
          children.add(node);

          if (str.pollIf(',')) continue;
          if (str.pollIf(')')) break;

          throw new SyntaxException(
              "Expected one of ')' or ',' but found '" + str.peekNext() + "'", str.getPosition());
        }
      }
    }
    this.end = str.getPosition();
  }

  public String getBase() {
    return base;
  }

  public int getChildrenCount() {
    return children == null ? -1 : children.size();
  }

  public List<ParsingNode> getChildren() {
    return children;
  }

  public String toString() {
    return escape(base)
        + (children == null
            ? ""
            : children.stream()
                .map(ParsingNode::toString)
                .collect(Collectors.joining(", ", "(", ")")));
  }

  public static String escape(String val) {
    for (char c : val.toCharArray()) {
      if (c == '(' || c == ',' || c == ')' || c == ' ')
        return '"' + val.replaceAll("([\\\\\"])", "\\$1") + '"';
    }
    return val;
  }
}
