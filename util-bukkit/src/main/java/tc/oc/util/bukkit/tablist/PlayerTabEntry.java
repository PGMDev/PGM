package tc.oc.util.bukkit.tablist;

import java.util.UUID;
import javax.annotation.Nullable;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Skin;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSkinPartsChangeEvent;
import tc.oc.util.bukkit.component.types.PersonalizedPlayer;
import tc.oc.util.bukkit.named.NameStyle;
import tc.oc.util.bukkit.nms.NMSHacks;
import tc.oc.util.collection.DefaultProvider;

/**
 * {@link TabEntry} showing a {@link Player}'s name and skin.
 *
 * <p>Note that this is NOT the player's real entry. It has a random UUID and name, like any other
 * {@link SimpleTabEntry}. While this entry is visible in a {@link TabView}, a fake player entity
 * will be spawned with a copy of the real player's metadata.
 */
public class PlayerTabEntry extends DynamicTabEntry {
  public static class Factory implements DefaultProvider<Player, PlayerTabEntry> {
    @Override
    public PlayerTabEntry get(Player key) {
      return new PlayerTabEntry(key);
    }
  }

  private static UUID randomUUIDVersion2SameDefaultSkin(UUID original) {
    // Parity of UUID.hashCode determines if the player's default skin is Steve/Alex
    // To make the player list match, we generate a random UUID with the same hashCode parity.
    // UUID.hashCode returns the XOR of its four 32-bit segments, so set bit 0 to the desired
    // parity, and clear bits 32, 64, and 96

    long parity = original.hashCode() & 1L;
    long mask = ~((1L << 32) | 1L);
    UUID uuid = randomUUIDVersion2();
    uuid =
        new UUID(
            uuid.getMostSignificantBits() & mask, (uuid.getLeastSignificantBits() & mask) | parity);
    return uuid;
  }

  protected final Player player;
  protected @Nullable PersonalizedPlayer content;
  private final int spareEntityId;

  public PlayerTabEntry(Player player) {
    super(randomUUIDVersion2SameDefaultSkin(player.getUniqueId()));
    this.player = player;
    this.spareEntityId = NMSHacks.allocateEntityId();
  }

  @Override
  public BaseComponent getContent(TabView view) {
    if (content == null) {
      this.content = new PersonalizedPlayer(player, NameStyle.TAB);
    }
    return content.render(view.getViewer());
  }

  @Override
  public int getFakeEntityId(TabView view) {
    return this.spareEntityId;
  }

  @Override
  public Player getFakePlayer(TabView view) {
    return this.player;
  }

  @Override
  public @Nullable Skin getSkin(TabView view) {
    return this.player.getSkin();
  }

  // Dispatched by TabManager
  protected void onSkinPartsChange(PlayerSkinPartsChangeEvent event) {
    if (this.player == event.getPlayer()) {
      this.updateFakeEntity();
    }
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "{" + this.player.getName() + "}";
  }
}
