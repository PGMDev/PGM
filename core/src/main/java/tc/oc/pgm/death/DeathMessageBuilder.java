package tc.oc.pgm.death;

import java.util.NavigableSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.*;
import tc.oc.pgm.api.tracker.info.PotionInfo;
import tc.oc.pgm.tracker.Trackers;
import tc.oc.pgm.tracker.info.BlockInfo;
import tc.oc.pgm.tracker.info.EntityInfo;
import tc.oc.pgm.tracker.info.ExplosionInfo;
import tc.oc.pgm.tracker.info.FallingBlockInfo;
import tc.oc.pgm.tracker.info.FireInfo;
import tc.oc.pgm.tracker.info.GenericDamageInfo;
import tc.oc.pgm.tracker.info.ItemInfo;
import tc.oc.pgm.tracker.info.MobInfo;
import tc.oc.pgm.tracker.info.ProjectileInfo;
import tc.oc.pgm.tracker.info.SpleefInfo;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.Components;
import tc.oc.pgm.util.component.types.PersonalizedText;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.translations.TranslationUtils;

public class DeathMessageBuilder {

  static class NoMessage extends Exception {}

  static NavigableSet<String> allKeys;

  static NavigableSet<String> getAllKeys() {
    if (allKeys == null) {
      allKeys = TranslationUtils.getKeys("death.");
    }
    return allKeys;
  }

  private static final long SNIPE_DISTANCE = 60;
  private static final int TRIPPED_HEIGHT = 5;
  private static final int NOTABLE_HEIGHT = 12;
  private static final int ORBIT_HEIGHT = 60;

  private final Logger logger;
  private final MatchPlayer victim;
  private final @Nullable ParticipantState killer;

  private String key;
  private Component weapon = Components.blank();
  private Component mob = Components.blank();
  private Long distance;

  public DeathMessageBuilder(MatchPlayer victim, DamageInfo damageInfo, Logger logger) {
    this.victim = victim;
    this.killer = damageInfo.getAttacker();
    this.logger = logger;

    build(damageInfo);
  }

  public Component getMessage() {
    return new PersonalizedTranslatable(key, getArgs());
  }

  Component[] getArgs() {
    Component[] args = new Component[5];
    args[0] = victim.getStyledName(NameStyle.COLOR);
    args[1] = killer == null ? Components.blank() : killer.getStyledName(NameStyle.COLOR);
    args[2] = weapon;
    args[3] = mob;
    args[4] =
        distance == null ? Components.blank() : new PersonalizedText(String.valueOf(distance));
    return args;
  }

  void setDistance(double n) {
    if (!Double.isNaN(n)) {
      distance = Math.round(Math.max(0, n));
      if (distance == 1l) distance = 2l; // Cleverly ensure the text is always plural
    }
  }

  /*
   * Primitive methods for manipulating the key
   */

  /** Test if the given string is a prefix of any existing key */
  boolean exists(String prefix) {
    String key = getAllKeys().ceiling(prefix);
    return key != null && key.startsWith(prefix);
  }

  /** Return a new key built from the current key with the given tokens appended */
  String append(String... tokens) {
    String newKey = key;
    for (String token : tokens) {
      newKey += '.' + token;
    }
    return newKey;
  }

  /**
   * Try to append an optional sequence of tokens to the current key. If the new key is invalid, the
   * current key is not changed.
   */
  boolean option(String... tokens) {
    String newKey = append(tokens);
    if (exists(newKey)) {
      key = newKey;
      return true;
    }
    return false;
  }

  /**
   * Append a sequence of tokens to the current key.
   *
   * @throws NoMessage if the new key is not valid
   */
  void require(String... tokens) throws NoMessage {
    String newKey = append(tokens);
    if (!exists(newKey)) {
      logger.warning("Generated invalid death message key: " + newKey);
      throw new NoMessage();
    }
    key = newKey;
  }

  /**
   * Assert that the current key is complete and valid.
   *
   * @throws NoMessage if it's not
   */
  void finish() throws NoMessage {
    if (!getAllKeys().contains(key)) {
      throw new NoMessage();
    }
  }

  /*
   * Optional components
   *
   * These methods all try
   * to append something to the key, and return true if successful.
   * If they fail, they leave the key unchanged.
   */

  boolean variant() {
    int count = 0;
    for (; getAllKeys().contains(key + "." + count); count++) ;

    if (count == 0) return false;

    int variant = victim.getMatch().getRandom().nextInt(count);
    key += "." + variant;
    return true;
  }

  boolean ranged(RangedInfo rangedInfo, @Nullable Location distanceReference) {
    double distance = Trackers.distanceFromRanged(rangedInfo, distanceReference);
    if (!Double.isNaN(distance) && option("distance")) {
      setDistance(distance);
      if (distance >= SNIPE_DISTANCE) {
        option("snipe");
      }
      return true;
    }
    return false;
  }

  boolean potion(PotionInfo potionInfo) {
    if (option("potion")) {
      weapon = potionInfo.getLocalizedName();
      return true;
    }
    return false;
  }

  boolean item(ItemInfo itemInfo) {
    if (itemInfo.getItem().getType() != Material.AIR && option("item")) {
      weapon = itemInfo.getLocalizedName();
      return true;
    }
    return false;
  }

  boolean block(BlockInfo blockInfo) {
    if (option("block")) {
      weapon = blockInfo.getLocalizedName();
      return true;
    }
    return false;
  }

  boolean entity(EntityInfo entityInfo) {
    if (option("entity")) {
      weapon = entityInfo.getLocalizedName();
      option(entityInfo.getIdentifier());
      return true;
    }
    return false;
  }

  boolean insentient(@Nullable PhysicalInfo info) {
    if (info instanceof PotionInfo) {
      if (potion((PotionInfo) info)) {
        return true;
      } else if (option("entity")) {
        // PotionInfo.getLocalizedName returns a potion name,
        // which doesn't work outside a potion death message.
        weapon = new PersonalizedTranslatable("item.potion.name");
        return true;
      }
    } else if (info instanceof EntityInfo) {
      return !(info instanceof MobInfo) && entity((EntityInfo) info);
    } else if (info instanceof BlockInfo) {
      return block((BlockInfo) info);
    } else if (info instanceof ItemInfo) {
      return item((ItemInfo) info);
    }

    return false;
  }

  boolean mob(MobInfo mobInfo) {
    if (option("mob")) {
      mob = mobInfo.getLocalizedName();
      option(mobInfo.getIdentifier());
      return true;
    }
    return false;
  }

  boolean physical(@Nullable PhysicalInfo info) {
    if (info instanceof MobInfo) {
      return mob((MobInfo) info);
    } else {
      return insentient(info);
    }
  }

  /*
   * Required components
   *
   * Each of these methods appends several keys to the death message,
   * and generally expects to complete successfully. If they fail, they
   * throw a {@link NoMessage} exception and leave the key in an
   * unknown state.
   */

  void player() throws NoMessage {
    if (killer != null) {
      require("player");
    }
  }

  void attack(@Nullable PhysicalInfo attacker, @Nullable PhysicalInfo weapon) throws NoMessage {
    player();
    if (attacker instanceof MobInfo && !mob((MobInfo) attacker)) {
      return;
    }
    insentient(weapon);
  }

  void generic(GenericDamageInfo info) throws NoMessage {
    switch (info.getDamageType()) {
      case CONTACT:
        require("cactus");
        break;
      case DROWNING:
        require("drown");
        break;
      case LIGHTNING:
        require("lightning");
        break;
      case STARVATION:
        require("starve");
        break;
      case SUFFOCATION:
        require("suffocate");
        break;
      case CUSTOM:
        require("generic");
        break;
      default:
        require("unknown");
        break;
    }
  }

  void melee(MeleeInfo melee) throws NoMessage {
    require("melee");
    attack(melee, melee.getWeapon());
  }

  void magic(PotionInfo potion, @Nullable PhysicalInfo attacker) throws NoMessage {
    require("magic");
    attack(attacker, potion);
  }

  void projectile(ProjectileInfo projectile, Location distanceReference) throws NoMessage {
    if (projectile.getProjectile() instanceof PotionInfo) {
      try {
        magic((PotionInfo) projectile.getProjectile(), projectile.getShooter());
        return;
      } catch (NoMessage ignored) {
        // If we can't generate a magic message (probably because it's part
        // of a fall message), fall back to a projectile message.
      }
    }

    require("projectile");

    if (projectile.getProjectile() instanceof EntityInfo
        && ((EntityInfo) projectile.getProjectile()).getEntityType() == EntityType.ARROW) {
      // "shot by arrow" is redundant
      attack(projectile.getShooter(), null);
    } else {
      attack(projectile.getShooter(), projectile.getProjectile());
      weapon =
          projectile
              .getLocalizedName(); // Projectile name may be different than entity name e.g. custom
      // projectile
    }

    ranged(projectile, distanceReference);
  }

  void squash(FallingBlockInfo fallingBlock) throws NoMessage {
    require("squash");
    attack(null, fallingBlock);
  }

  void explosion(ExplosionInfo explosion, Location distanceReference) throws NoMessage {
    require("explosive");
    player();
    physical(explosion.getExplosive());
    ranged(explosion, distanceReference);
  }

  void fire(FireInfo fire) throws NoMessage {
    require("fire");
    player();
    if (!(fire.getIgniter() instanceof BlockInfo
        && ((BlockInfo) fire.getIgniter()).getMaterial().getItemType() == Material.FIRE)) {
      // "burned by fire" is redundant
      physical(fire.getIgniter());
    }
  }

  void fall(FallInfo fall) throws NoMessage {
    require("fall");
    require(fall.getTo().name().toLowerCase());

    TrackerInfo cause = fall.getCause();
    if (cause instanceof SpleefInfo) {
      require("spleef");
      DamageInfo breaker = ((SpleefInfo) cause).getBreaker();
      if (breaker instanceof ExplosionInfo) {
        explosion((ExplosionInfo) breaker, fall.getOrigin());
      } else {
        player();
      }
    } else if (cause instanceof DamageInfo) {
      damage((DamageInfo) cause);
    } else if (fall.getTo() == FallInfo.To.GROUND) {
      setDistance(Trackers.distanceFromRanged(fall, victim.getBukkit().getLocation()));

      if (distance != null) {
        if (distance <= TRIPPED_HEIGHT) {
          // Very short falls get a "tripped" message
          option("tripped");
        } else if (distance >= ORBIT_HEIGHT) {
          // Very long falls get an "orbit" message
          option("orbit");
        } else if (victim.getMatch().getRandom().nextFloat() < 0.01f) {
          // Occasionally they get a rare message
          option("rare");
        }

        // Show distance if it's high enough and the message supports it
        if (distance >= NOTABLE_HEIGHT) option("distance");
      }
    }
  }

  void damage(DamageInfo info) throws NoMessage {
    if (info instanceof MeleeInfo) {
      melee((MeleeInfo) info);
    } else if (info instanceof ProjectileInfo) {
      projectile((ProjectileInfo) info, victim.getBukkit().getLocation());
    } else if (info instanceof ExplosionInfo) {
      explosion((ExplosionInfo) info, victim.getBukkit().getLocation());
    } else if (info instanceof FireInfo) {
      fire((FireInfo) info);
    } else if (info instanceof PotionInfo) {
      magic((PotionInfo) info, null);
    } else if (info instanceof FallingBlockInfo) {
      squash((FallingBlockInfo) info);
    } else if (info instanceof FallInfo) {
      fall((FallInfo) info);
    } else if (info instanceof GenericDamageInfo) {
      generic((GenericDamageInfo) info);
    } else {
      throw new NoMessage();
    }
  }

  void build(DamageInfo damageInfo) {
    logger.fine("Generating death message for " + damageInfo);

    try {
      key = "death";
      damage(damageInfo);
      variant();
      finish();
    } catch (NoMessage ex) {
      logger.log(
          Level.SEVERE,
          "Generated invalid death message '"
              + key
              + "' for victim="
              + victim
              + " info="
              + damageInfo
              + " killer="
              + killer
              + " weapon="
              + weapon
              + " mob="
              + mob
              + " distance="
              + distance,
          ex);
      key = "death.generic";
    }
  }
}
