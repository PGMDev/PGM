package tc.oc.named;

import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import tc.oc.component.Component;
import tc.oc.identity.Identity;

/** Renders player names from {@link Identity}s and {@link NameType}s */
public interface NameRenderer {

  /** Get the color of the name (and possibly other things) */
  ChatColor getColor(Identity identity, NameType type);

  /** Get a legacy display name */
  String getLegacyName(Identity identity, NameType type);

  /**
   * Get a component display name
   *
   * @return
   */
  Component getComponentName(Identity identity, NameType type);

  /**
   * Called when the appearance of name(s) has changed for some combination of identities and
   * styles. Any renderer with a cache should invalidate the given identity rendered in the given
   * style. If either parameter is null, all values of that type should be invalidated.
   */
  void invalidateCache(@Nullable Identity identity);
}
