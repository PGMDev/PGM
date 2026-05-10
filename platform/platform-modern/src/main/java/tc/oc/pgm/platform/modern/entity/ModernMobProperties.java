package tc.oc.pgm.platform.modern.entity;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import org.bukkit.DyeColor;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Cat;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fox;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Wither;
import org.bukkit.entity.Zombie;
import tc.oc.pgm.entity.MobProperties;
import tc.oc.pgm.util.platform.Supports;

@Supports(PAPER)
public class ModernMobProperties extends MobProperties {

  public ModernMobProperties() {
    register(Entity.class, "silent", BOOL, Entity::setSilent);
    register(Entity.class, "glowing", BOOL, Entity::setGlowing);
    register(Entity.class, "invulnerable", BOOL, Entity::setInvulnerable);
    register(Entity.class, "invisible", BOOL, Entity::setInvisible);
    register(Entity.class, "persistent", BOOL, Entity::setPersistent);
    register(Entity.class, "gravity", BOOL, Entity::setGravity);
    register(Entity.class, "no-physics", BOOL, Entity::setNoPhysics);
    register(Entity.class, "freeze-ticks", INT, Entity::setFreezeTicks);
    register(Entity.class, "portal-cooldown", INT, Entity::setPortalCooldown);

    register(LivingEntity.class, "ai", BOOL, LivingEntity::setAI);
    register(LivingEntity.class, "collidable", BOOL, LivingEntity::setCollidable);
    register(LivingEntity.class, "gliding", BOOL, LivingEntity::setGliding);
    register(LivingEntity.class, "swimming", BOOL, LivingEntity::setSwimming);
    register(LivingEntity.class, "no-action-ticks", INT, LivingEntity::setNoActionTicks);

    register(Mob.class, "aware", BOOL, Mob::setAware);
    register(Mob.class, "aggressive", BOOL, Mob::setAggressive);
    register(Mob.class, "left-handed", BOOL, Mob::setLeftHanded);

    register(ChestedHorse.class, "carrying-chest", BOOL, ChestedHorse::setCarryingChest);

    register(Zombie.class, "can-break-doors", BOOL, Zombie::setCanBreakDoors);
    register(Zombie.class, "arms-raised", BOOL, Zombie::setArmsRaised);
    register(Zombie.class, "should-burn-in-day", BOOL, Zombie::setShouldBurnInDay);
    register(Zombie.class, "conversion-time", INT, Zombie::setConversionTime);

    register(Creeper.class, "max-fuse-ticks", INT, Creeper::setMaxFuseTicks);
    register(Creeper.class, "fuse-ticks", INT, Creeper::setFuseTicks);
    register(Creeper.class, "explosion-radius", INT, Creeper::setExplosionRadius);
    register(Creeper.class, "ignited", BOOL, Creeper::setIgnited);

    register(Phantom.class, "size", INT, Phantom::setSize);
    register(Phantom.class, "should-burn-in-day", BOOL, Phantom::setShouldBurnInDay);

    register(Ocelot.class, "trusting", BOOL, Ocelot::setTrusting);

    register(Cat.class, "collar-color", enumOf(DyeColor.class), Cat::setCollarColor);
    register(Cat.class, "lying-down", BOOL, Cat::setLyingDown);
    register(Cat.class, "head-up", BOOL, Cat::setHeadUp);

    register(Fox.class, "fox-type", enumOf(Fox.Type.class), Fox::setFoxType);
    register(Fox.class, "crouching", BOOL, Fox::setCrouching);
    register(Fox.class, "sleeping", BOOL, Fox::setSleeping);
    register(Fox.class, "interested", BOOL, Fox::setInterested);
    register(Fox.class, "leaping", BOOL, Fox::setLeaping);
    register(Fox.class, "defending", BOOL, Fox::setDefending);
    register(Fox.class, "faceplanted", BOOL, Fox::setFaceplanted);

    register(Panda.class, "main-gene", enumOf(Panda.Gene.class), Panda::setMainGene);
    register(Panda.class, "hidden-gene", enumOf(Panda.Gene.class), Panda::setHiddenGene);
    register(Panda.class, "rolling", BOOL, Panda::setRolling);
    register(Panda.class, "sneezing", BOOL, Panda::setSneezing);
    register(Panda.class, "eating", BOOL, Panda::setEating);

    register(Bee.class, "has-nectar", BOOL, Bee::setHasNectar);
    register(Bee.class, "has-stung", BOOL, Bee::setHasStung);
    register(Bee.class, "anger", INT, Bee::setAnger);
    register(Bee.class, "cannot-enter-hive-ticks", INT, Bee::setCannotEnterHiveTicks);
    register(Bee.class, "time-since-sting", INT, Bee::setTimeSinceSting);

    register(Snowman.class, "derp", BOOL, Snowman::setDerp);

    register(Endermite.class, "lifetime-ticks", INT, Endermite::setLifetimeTicks);

    register(Witch.class, "potion-use-time-left", INT, Witch::setPotionUseTimeLeft);

    register(Wither.class, "invulnerable-ticks", INT, Wither::setInvulnerableTicks);
    register(Wither.class, "can-travel-through-portals", BOOL, Wither::setCanTravelThroughPortals);
  }
}
