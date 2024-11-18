package tc.oc.pgm.util.tablist;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.skin.Skin;

/** Content for a slot in a {@link TabView} */
public interface TabEntry {

  /** Called by {@link TabView}s when this entry is added to the view */
  void addToView(TabView view);

  /** Called by {@link TabView}s when this entry is removed from the view */
  void removeFromView(TabView view);

  /** Called by {@link TabView} during rendering to decide if this entry needs to be rendered */
  boolean isDirty(TabView view);

  /** Called by {@link TabView} after rendering this entry in the view */
  void markClean(TabView view);

  /**
   * UUID for the entry. The client's player list is keyed on this, so it must be unique. If it
   * matches a real player's UUID, this entry will affect the player in all sorts of ways.
   */
  UUID getId();

  /**
   * {@link Player} represented by this entry, or null if it does not represent a player. The
   * renderer will use this player's metadata to spawn a fake player so that the hat layer on the
   * entry's icon can be controlled.
   *
   * <p>See {@link PlayerTabEntry} for more info.
   */
  @Nullable
  Player getFakePlayer(TabView view);

  /**
   * Entity ID of the fake player mentioned above. If used, this must not collide with any real
   * entites.
   */
  int getFakeEntityId(TabView view);

  /** Name for the entry (not visible) */
  String getName(TabView view);

  /**
   * Content to show in the entry
   *
   * @return
   */
  Component getContent(TabView view);

  /**
   * Gamemode for the entry. If the entry is linked to a real player, this will change the client's
   * gamemode.
   */
  GameMode getGamemode();

  /** Ping value for the entry */
  int getPing();

  /**
   * Skin for the entry's icon
   *
   * @return
   */
  @Nullable
  Skin getSkin(TabView view);
}
