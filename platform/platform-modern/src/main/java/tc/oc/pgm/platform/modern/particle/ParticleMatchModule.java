package tc.oc.pgm.platform.modern.particle;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.util.block.BlockData;

@ListenerScope(MatchScope.RUNNING)
public class ParticleMatchModule implements MatchModule, Listener {

  private final Match match;
  private final ImmutableSet<ParticleDefinition> particleDefinitions;

  public ParticleMatchModule(Match match, ImmutableSet<ParticleDefinition> particleDefinitions) {
    this.match = match;
    this.particleDefinitions = particleDefinitions;
  }

  public static void spawnParticle(
      ParticleDefinition def, Location origin, Color overrideColor, Match match) {
    List<Player> viewers = null;
    if (def.getViewerFilter() != null) {
      viewers = match.getPlayers().stream()
          .filter(player -> def.getViewerFilter().query(player).isAllowed())
          .map(MatchPlayer::getBukkit)
          .collect(Collectors.toList());
    }
    List<Player> receivers = null;

    Class<?> dataType = def.getType().getDataType();

    if (dataType == Particle.DustOptions.class) {
      Color color = overrideColor != null ? overrideColor : def.getDustOptions().getColor();
      origin
          .getWorld()
          .spawnParticle(
              def.getType(),
              viewers,
              null,
              origin.getX(),
              origin.getY(),
              origin.getZ(),
              def.getAmount(),
              0,
              0,
              0,
              0,
              new Particle.DustOptions(color, def.getDustOptions().getSize()),
              def.isForce());
    } else if (dataType == Particle.DustTransition.class) {
      org.bukkit.Color startColor =
          overrideColor != null ? overrideColor : def.getDustOptions().getColor();
      Particle.DustTransition transition = (Particle.DustTransition) def.getDustOptions();
      origin
          .getWorld()
          .spawnParticle(
              def.getType(),
              viewers,
              null,
              origin.getX(),
              origin.getY(),
              origin.getZ(),
              def.getAmount(),
              0,
              0,
              0,
              0,
              new Particle.DustTransition(
                  startColor, transition.getToColor(), def.getDustOptions().getSize()),
              def.isForce());
    } else if (dataType == org.bukkit.Color.class) {
      origin
          .getWorld()
          .spawnParticle(
              def.getType(),
              viewers,
              null,
              origin.getX(),
              origin.getY(),
              origin.getZ(),
              def.getAmount(),
              0,
              0,
              0,
              0,
              overrideColor != null ? overrideColor : def.getColor(),
              def.isForce());
    } else if (dataType == Particle.Spell.class) {
      org.bukkit.Color color =
          overrideColor != null ? overrideColor : def.getPower().getColor();
      origin
          .getWorld()
          .spawnParticle(
              def.getType(),
              viewers,
              null,
              origin.getX(),
              origin.getY(),
              origin.getZ(),
              def.getAmount(),
              0,
              0,
              0,
              0,
              new Particle.Spell(color, def.getPower().getPower()),
              def.isForce());
    } else if (dataType == Float.class) {
      float angleVal = def.getAngle() != null ? def.getAngle() : 0.0f;
      origin
          .getWorld()
          .spawnParticle(
              def.getType(),
              viewers,
              null,
              origin.getX(),
              origin.getY(),
              origin.getZ(),
              def.getAmount(),
              0,
              0,
              0,
              0,
              angleVal,
              def.isForce());
    } else if (dataType == Integer.class) {
      origin
          .getWorld()
          .spawnParticle(
              def.getType(),
              viewers,
              null,
              origin.getX(),
              origin.getY(),
              origin.getZ(),
              def.getAmount(),
              0,
              0,
              0,
              0,
              def.getDelay(),
              def.isForce());
    } else if (dataType == BlockData.class) {
      origin
          .getWorld()
          .spawnParticle(
              def.getType(),
              viewers,
              null,
              origin.getX(),
              origin.getY(),
              origin.getZ(),
              def.getAmount(),
              0,
              0,
              0,
              0,
              def.blockData(),
              def.isForce());
    } else if (dataType == ItemStack.class) {
      origin
          .getWorld()
          .spawnParticle(
              def.getType(),
              viewers,
              null,
              origin.getX(),
              origin.getY(),
              origin.getZ(),
              def.getAmount(),
              0,
              0,
              0,
              0,
              def.itemStack(),
              def.isForce());
    } else {
      origin
          .getWorld()
          .spawnParticle(
              def.getType(),
              viewers,
              null,
              origin.getX(),
              origin.getY(),
              origin.getZ(),
              def.getAmount(),
              0,
              0,
              0,
              0,
              null,
              def.isForce());
    }
  }
}
