package tc.oc.named;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.component.Component;
import tc.oc.identity.Identities;
import tc.oc.identity.Identity;
import tc.oc.pgm.PGMUtil;

public class Names {
  private Names() {}

  /** Get the current (global) {@link NameRenderer} */
  public static NameRenderer renderer() {
    return PGMUtil.get().getNameRenderer();
  }

  /** Invalidate any cached names for the given {@link Identity} */
  public static void invalidate(Identity identity) {
    renderer().invalidateCache(identity);
  }

  /** Invalidate any cached names for the given {@link Player} */
  public static void invalidate(Player player) {
    renderer().invalidateCache(Identities.current(player));
  }

  /**
   * Get the plain text name of a player for a specific viewer
   *
   * @param identity identity of the named player
   * @param viewer viewer of the name
   */
  public static String text(Identity identity, CommandSender viewer) {
    return renderer().getLegacyName(identity, new NameType(NameStyle.PLAIN, identity, viewer));
  }

  /**
   * Get the name of a player for a specific viewer, colored with legacy formatting codes
   *
   * @param identity identity of the named player
   * @param viewer viewer of the name
   */
  public static String legacy(Identity identity, CommandSender viewer) {
    return renderer().getLegacyName(identity, new NameType(NameStyle.COLOR, identity, viewer));
  }

  /**
   * Get the colored name of a player for a specific viewer
   *
   * @param identity identity of the named player
   * @param viewer viewer of the name
   */
  public static Component plain(Identity identity, CommandSender viewer) {
    return renderer().getComponentName(identity, new NameType(NameStyle.COLOR, identity, viewer));
  }

  /**
   * Get the colored name of a player, with flair, for a specific viewer
   *
   * @param identity identity of the named player
   * @param viewer viewer of the name
   */
  public static Component fancy(Identity identity, CommandSender viewer) {
    return renderer().getComponentName(identity, new NameType(NameStyle.FANCY, identity, viewer));
  }

  /**
   * Get the colored name of a player, with flair and nickname, for a specific viewer
   *
   * @param identity identity of the named player
   * @param viewer viewer of the name
   */
  public static Component verbose(Identity identity, CommandSender viewer) {
    return renderer().getComponentName(identity, new NameType(NameStyle.VERBOSE, identity, viewer));
  }

  /** Calls {@link #text(Identity, CommandSender)} with the given player's current identity */
  public static String text(Player player, CommandSender viewer) {
    return text(Identities.current(player), viewer);
  }

  /** Calls {@link #legacy(Identity, CommandSender)} with the given player's current identity */
  public static String legacy(Player player, CommandSender viewer) {
    return legacy(Identities.current(player), viewer);
  }

  /** Calls {@link #plain(Identity, CommandSender)} with the given player's current identity */
  public static Component plain(Player player, CommandSender viewer) {
    return plain(Identities.current(player), viewer);
  }

  /** Calls {@link #fancy(Identity, CommandSender)} with the given player's current identity */
  public static Component fancy(Player player, CommandSender viewer) {
    return fancy(Identities.current(player), viewer);
  }

  /** Calls {@link #verbose(Identity, CommandSender)} with the given player's current identity */
  public static Component verbose(Player player, CommandSender viewer) {
    return verbose(Identities.current(player), viewer);
  }
}
