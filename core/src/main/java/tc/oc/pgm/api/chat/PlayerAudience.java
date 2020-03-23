package tc.oc.pgm.api.chat;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.component.types.PersonalizedText;
import tc.oc.util.bukkit.nms.NMSHacks;

public class PlayerAudience extends CommandSenderAudience {

  public PlayerAudience(Player player) {
    super(player);
  }

  protected Player getPlayer() {
    return (Player) getCommandSender();
  }

  @Override
  public void sendHotbarMessage(Component message) {
    NMSHacks.sendHotbarMessage(getPlayer(), message);
  }

  @Override
  public void showTitle(
      Component title, Component subtitle, int inTicks, int stayTicks, int outTicks) {
    title = title == null ? new PersonalizedText("") : title;
    subtitle = subtitle == null ? new PersonalizedText("") : subtitle;

    Player player = getPlayer();
    player.showTitle(title.render(player), subtitle.render(player), inTicks, stayTicks, outTicks);
  }

  @Override
  public void playSound(Sound sound) {
    final Location location =
        sound.location == null
            ? getPlayer().getLocation()
            : sound.location.toLocation(getPlayer().getWorld());
    getPlayer().playSound(location, sound.name, sound.volume, sound.pitch);
  }
}
