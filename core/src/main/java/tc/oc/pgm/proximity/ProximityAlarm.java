package tc.oc.pgm.proximity;

import static net.kyori.adventure.key.Key.key;
import static net.kyori.adventure.sound.Sound.sound;
import static net.kyori.adventure.text.Component.text;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.controlpoint.RegionPlayerTracker;
import tc.oc.pgm.fireworks.FireworkMatchModule;
import tc.oc.pgm.util.nms.NMSHacks;

public class ProximityAlarm {
  private static final long MESSAGE_INTERVAL = 5000;
  private static final float FLARE_CHANCE = 0.25f;

  private static final Sound SOUND =
      sound(key("fireworks.blast_far"), Sound.Source.MASTER, 1f, 0.7f);

  protected final Random random;
  protected final Match match;
  protected final ProximityAlarmDefinition definition;
  protected long lastMessageTime = 0;

  protected final RegionPlayerTracker playerTracker;

  public ProximityAlarm(Match match, ProximityAlarmDefinition definition, Random random) {
    this.random = random;
    this.match = match;
    this.definition = definition;
    this.playerTracker =
        new RegionPlayerTracker(match, definition.detectRegion, definition.detectFilter);
  }

  public void showAlarm() {
    if (this.random.nextFloat() < FLARE_CHANCE && !this.playerTracker.getPlayers().isEmpty()) {
      this.showFlare();
      this.showMessage();
    }
  }

  private void showFlare() {
    if (!definition.flares) return;

    float angle = (float) (this.random.nextFloat() * Math.PI * 2);
    Location location =
        this.definition
            .detectRegion
            .getBounds()
            .getCenterPoint()
            .toLocation(match.getWorld())
            .add(
                Math.sin(angle) * this.definition.flareRadius,
                0,
                Math.cos(angle) * this.definition.flareRadius);

    Set<Color> colors = new HashSet<>();

    for (MatchPlayer player : this.playerTracker.getPlayers()) {
      colors.add(player.getParty().getFullColor());
    }

    Firework firework =
        FireworkMatchModule.spawnFirework(
            location,
            FireworkEffect.builder().with(FireworkEffect.Type.BALL).withColor(colors).build(),
            0);
    NMSHacks.skipFireworksLaunch(firework);
  }

  private void showMessage() {
    if (this.definition.alertMessage == null) {
      return;
    }

    long now = System.currentTimeMillis();
    if (this.lastMessageTime + MESSAGE_INTERVAL < now) {
      this.lastMessageTime = now;

      for (MatchPlayer player : this.match.getPlayers()) {
        if (this.definition.alertFilter.query(player).isAllowed()) {
          player.sendMessage(text(this.definition.alertMessage, NamedTextColor.RED));
          player.playSound(SOUND);
        }
      }
    }
  }
}
