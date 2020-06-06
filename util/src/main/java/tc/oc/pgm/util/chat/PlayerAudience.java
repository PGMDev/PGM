package tc.oc.pgm.util.chat;

import javax.annotation.Nullable;
import net.kyori.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.github.paperspigot.Title;
import tc.oc.pgm.util.text.TextTranslations;

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
  default void showHotbar(Component message) {
    getAudience().sendActionBar(TextTranslations.translateLegacy(message, getAudience()));
  }

  @Override
  default void showBossbar(@Nullable Component message, float progress) {
    // TODO
  }

  @Override
  default void showTitle(
      Component title, Component subTitle, int inTicks, int stayTicks, int outTicks) {
    Title bukkitTitle =
        Title.builder()
            .title(TextTranslations.toBaseComponentArray(title, getAudience()))
            .subtitle(TextTranslations.toBaseComponentArray(subTitle, getAudience()))
            .fadeIn(inTicks)
            .stay(stayTicks)
            .fadeOut(outTicks)
            .build();
    getAudience().sendTitle(bukkitTitle);
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
}
