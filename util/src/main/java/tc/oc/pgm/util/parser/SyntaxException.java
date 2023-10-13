package tc.oc.pgm.util.parser;

public class SyntaxException extends RuntimeException {
  private final int startIdx;
  private final int endIdx;

  public SyntaxException(String message, int nextIdx) {
    super(message);
    this.startIdx = nextIdx;
    this.endIdx = -1;
  }

  public SyntaxException(String message, ParsingNode node) {
    super(message);
    this.startIdx = node.getStart();
    this.endIdx = node.getEnd();
  }

  public int getStartIdx() {
    return startIdx;
  }

  public int getEndIdx() {
    return endIdx;
  }
}
