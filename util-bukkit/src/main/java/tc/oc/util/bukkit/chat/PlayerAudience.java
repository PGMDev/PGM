package tc.oc.util.bukkit.chat;

import static tc.oc.util.bukkit.nms.NMSHacks.BossBarState;

import javax.annotation.Nullable;
import net.kyori.text.adapter.bukkit.TextAdapter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import tc.oc.util.bukkit.BukkitUtils;
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.component.types.PersonalizedText;
import tc.oc.util.bukkit.nms.NMSHacks;

/** An {@link Audience} that represents an online {@link Player}. */
@FunctionalInterface
public interface PlayerAudience extends VirtualAudience {

  /**
   * Get the {@link Player} of this audience.
   *
   * @return A player.
   */
  @Override
  Player getAudience();

  @Override
  default void sendHotbarMessage(Component message) {
    NMSHacks.sendHotbarMessage(getAudience(), message);
  }

  @Override
  default void showHotbar(net.kyori.text.Component message) {
    TextAdapter.sendActionBar(getAudience(), renderMessage(message));
  }

  @Override
  default void showBossbar(@Nullable net.kyori.text.Component message, float progress) {
    final Player player = getAudience();
    final Plugin plugin = BukkitUtils.getPlugin();

    MetadataValue meta = player.getMetadata(BossBarState.KEY, plugin);
    if (meta == null || meta.value() == null) {
      meta = new FixedMetadataValue(plugin, new BossBarState());
      player.setMetadata(BossBarState.KEY, meta);
    }

    final BossBarState bar = (BossBarState) meta.value();
    bar.update(player, message == null ? null : renderMessageLegacy(message), progress);
  }

  @Override
  default void playSound(Sound sound) {
    final Player player = getAudience();
    final Location location =
        sound.location == null
            ? player.getLocation()
            : sound.location.toLocation(player.getWorld());
    player.playSound(location, sound.name, sound.volume, sound.pitch);
  }

  ///////////////////////////////////////////////////////////////
  // METHODS BELOW ARE ALL DEPRECATED AND WILL BE REMOVED SOON //
  ///////////////////////////////////////////////////////////////

  @Override
  default void showTitle(
      Component title, Component subtitle, int inTicks, int stayTicks, int outTicks) {
    title = title == null ? new PersonalizedText("") : title;
    subtitle = subtitle == null ? new PersonalizedText("") : subtitle;
    Player player = getAudience();
    player.showTitle(title.render(player), subtitle.render(player), inTicks, stayTicks, outTicks);
  }
}
