package tc.oc.pgm.util.event.player;

import org.bukkit.Skin;
import org.bukkit.entity.Player;

public class PlayerSkinChangeEvent extends PlayerSkinPartsChangeEvent {

  private final Skin skin;

  public PlayerSkinChangeEvent(Player who, Skin skin) {
    super(who);
    this.skin = skin;
  }

  public Skin getSkin() {
    return skin;
  }
}
