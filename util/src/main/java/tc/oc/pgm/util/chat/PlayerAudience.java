package tc.oc.pgm.util.chat;

import javax.annotation.Nullable;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.github.paperspigot.Title;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.types.PersonalizedText;
import tc.oc.pgm.util.nms.NMSHacks;
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
  default void sendHotbarMessage(Component message) {
    NMSHacks.sendHotbarMessage(getAudience(), message);
  }

  @Override
  default void showHotbar(net.kyori.text.Component message) {
    getAudience().sendActionBar(TextTranslations.translateLegacy(message, getAudience()));
  }

  @Override
  default void showBossbar(@Nullable net.kyori.text.Component message, float progress) {
    // TODO
  }

  @Override
  default void showTitle(
      @Nullable net.kyori.text.Component title,
      @Nullable net.kyori.text.Component subTitle,
      int inTicks,
      int stayTicks,
      int outTicks) {
    Title bukkitTitle =
        Title.builder()
            .title(
                TextComponent.fromLegacyText(
                    TextTranslations.translateLegacy(title, getAudience())))
            .subtitle(
                TextComponent.fromLegacyText(
                    TextTranslations.translateLegacy(subTitle, getAudience())))
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
