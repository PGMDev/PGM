package tc.oc.pgm.platform.modern.modules.behavior.tempt;

import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.inventory.ItemMatcher;

public record TemptBehavior(
    @Nullable ItemMatcher holding, boolean follow, @Nullable Float range, boolean leaveHome) {}
