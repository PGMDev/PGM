package tc.oc.pgm.regions;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;

public enum RFAScope {
  PLAYER_ENTER("enter"),
  PLAYER_LEAVE("leave"),
  BLOCK_PLACE("block", "block-place"),
  BLOCK_BREAK("block", "block-break"),
  USE("use"),
  EFFECT(),
  BLOCK_PLACE_AGAINST("block-place-against"),
  BLOCK_PHYSICS("block-physics");

  public final String[] tags;

  RFAScope(String... tags) {
    this.tags = tags;
  }

  public static final SetMultimap<String, RFAScope> byTag;

  static {
    ImmutableSetMultimap.Builder<String, RFAScope> builder = ImmutableSetMultimap.builder();
    for (RFAScope scope : values()) {
      for (String tag : scope.tags) {
        builder.put(tag, scope);
      }
    }
    byTag = builder.build();
  }
}
