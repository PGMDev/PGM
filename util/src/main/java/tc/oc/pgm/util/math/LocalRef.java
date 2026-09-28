package tc.oc.pgm.util.math;

/**
 * Address of a local variable, resolved at parse time: {@code depth} frames up from the current
 * {@link LocalFrame}, then index {@code slot} in that frame.
 */
public record LocalRef(int depth, int slot) {}
