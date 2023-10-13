package tc.oc.pgm.util.parser;

import java.util.regex.Pattern;

public class StringReader {
    // "\\\\" compiles to string literal \\, which is one regex escaped \
    private static final Pattern ESCAPES_REGEX = Pattern.compile("\\\\(.)");

    private final String string;
    private int nextIdx;

    public StringReader(String string) {
        this.string = string;
    }

    /**
     * Reads a text (arbitrary string) as defined below:
     * Any character is eligible, except for '(' ',' ')' and any whitespace char.
     * You may use a quoted string "string" for any char except " being eligible.
     * Either of them may be escaped by using \, and \ can be escaped by \\.
     * @return The next word read in full.
     */
    public String readText() {
        boolean quoted = pollIf('"');

        for (int i = nextIdx; i < string.length(); i++) {
            char ch = string.charAt(i);
            if (ch == '\\') {
                // Skip the next char, it's been escaped
                i++;
            } else if (quoted ? ch == '"' : ch == '(' || ch == ',' || ch == ')' || ch == ' ') {
                if (nextIdx == i)
                    throw new SyntaxException("Invalid text, expected at least one char", nextIdx);

                String result = ESCAPES_REGEX.matcher(string.substring(nextIdx, i)).replaceAll("$1");
                nextIdx = i + (quoted ? 1 : 0);
                return result;
            }
        }

        if (quoted) {
            throw new SyntaxException("Unfinished quoted string", nextIdx);
        }

        String remaining = string.substring(nextIdx);
        nextIdx = string.length();
        return remaining;
    }

    public void done() {
        if (nextIdx < string.length())
            throw new SyntaxException("Unused characters after end", nextIdx);
    }

    public char peekNext() {
        if (nextIdx >= string.length()) return '\04'; // EOF
        return string.charAt(nextIdx);
    }

    public int getPosition() {
        return nextIdx;
    }

    public void skipWhitespace() {
        for (int i = nextIdx; i < string.length(); i++) {
            if (!Character.isWhitespace(string.charAt(i))) {
                nextIdx = i;
                break;
            }
        }
    }

    /**
     * Peek at the next char ignoring whitespace, if it's {@param ch}, poll it and return true
     * @param ch the char to maybe expect next
     * @return true if the next char is ch, and it was skipped, false otherwise
     */
    public boolean pollIf(char ch) {
        for (int i = nextIdx; i < string.length(); i++) {
            char currChar = string.charAt(i);
            if (currChar == ch) {
                nextIdx = i + 1;
                return true;
            } else if (!Character.isWhitespace(currChar)) {
                return false;
            }
        }
        return false;
    }

    public String substring(int start, int end) {
        return string.substring(start, end);
    }

}
