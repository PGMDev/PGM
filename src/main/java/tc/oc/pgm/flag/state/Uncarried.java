package tc.oc.pgm.flag.state;

import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import tc.oc.block.BlockStates;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.material.Materials;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.FlagMatchModule;
import tc.oc.pgm.flag.Post;
import tc.oc.pgm.flag.event.FlagPickupEvent;
import tc.oc.world.NMSHacks;

/** Base class for flag states in which the banner is placed on the ground somewhere as a block */
public abstract class Uncarried extends Spawned {

  protected final Location location;
  protected final BlockState oldBlock;
  protected final BlockState oldBase;
  protected ArmorStand labelEntity;
  private @Nullable MatchPlayer pickingUp;

  public Uncarried(Flag flag, Post post, @Nullable Location location) {
    super(flag, post);
    if (location == null) location = flag.getReturnPoint(post);
    this.location =
        new Location(
            location.getWorld(),
            location.getBlockX() + 0.5,
            location.getBlockY(),
            location.getBlockZ() + 0.5,
            location.getYaw(),
            location.getPitch());

    Block block = this.location.getBlock();
    if (block.getType() == Material.STANDING_BANNER) {
      // Banner may already be here at match start
      this.oldBlock = BlockStates.cloneWithMaterial(block, Material.AIR);
    } else {
      this.oldBlock = block.getState();
    }
    this.oldBase = block.getRelative(BlockFace.DOWN).getState();
  }

  @Override
  public Location getLocation() {
    return this.location;
  }

  protected void placeBanner() {
    if (!this.flag.canDropOn(oldBase)) {
      oldBase.getBlock().setType(Material.SEA_LANTERN, false);
    } else if (Materials.isWater(oldBase.getType())) {
      oldBase.getBlock().setType(Material.ICE, false);
    }

    Materials.placeStanding(this.location, this.flag.getBannerMeta());

    this.labelEntity =
        this.location.getWorld().spawn(this.location.clone().add(0, 0.7, 0), ArmorStand.class);
    this.labelEntity.setVisible(false);
    this.labelEntity.setGravity(false);
    this.labelEntity.setRemoveWhenFarAway(false);
    this.labelEntity.setSmall(true);
    this.labelEntity.setArms(false);
    this.labelEntity.setBasePlate(false);
    this.labelEntity.setCustomName(this.flag.getColoredName());
    this.labelEntity.setCustomNameVisible(true);
    NMSHacks.enableArmorSlots(this.labelEntity, false);
  }

  protected void breakBanner() {
    this.labelEntity.remove();
    oldBase.update(true, false);
    oldBlock.update(true, false);
  }

  @Override
  public void enterState() {
    super.enterState();
    this.placeBanner();
  }

  @Override
  public void leaveState() {
    this.breakBanner();
    super.leaveState();
  }

  @Override
  public boolean isCarrying(MatchPlayer player) {
    // This allows CarryingFlagFilter to match and cancel the pickup before it actually happens
    return player == this.pickingUp || super.isCarrying(player);
  }

  @Override
  public boolean isCarrying(Party party) {
    return (this.pickingUp != null && party == this.pickingUp.getParty())
        || super.isCarrying(party);
  }

  protected boolean pickupFlag(MatchPlayer carrier) {
    try {
      this.pickingUp = carrier;
      FlagPickupEvent event = new FlagPickupEvent(this.flag, carrier, this.location);
      this.flag.getMatch().callEvent(event);
      if (event.isCancelled()) return false;
    } finally {
      this.pickingUp = null;
    }

    this.flag.playStatusSound(Flag.PICKUP_SOUND_OWN, Flag.PICKUP_SOUND);
    this.flag.touch(carrier.getParticipantState());

    this.flag.transition(new Carried(this.flag, this.post, carrier, this.location));

    return true;
  }

  protected boolean inPickupRange(Location playerLoc) {
    Location flagLoc = this.getLocation();
    if (playerLoc.getY() < flagLoc.getY() + 2 && playerLoc.getY() >= flagLoc.getY() - 2) {
      double dx = playerLoc.getX() - flagLoc.getX();
      double dz = playerLoc.getZ() - flagLoc.getZ();

      if (dx * dx + dz * dz <= 1) {
        return true;
      }
    }

    return false;
  }

  protected boolean canPickup(MatchPlayer player) {
    if (this.pickingUp != null) return false; // Prevent infinite recursion

    for (Flag flag : this.flag.getMatch().getModule(FlagMatchModule.class).getFlags()) {
      if (flag.isCarrying(player)) return false;
    }

    return this.flag.canPickup(player, this.post);
  }

  @Override
  public void onEvent(PlayerMoveEvent event) {
    super.onEvent(event);
    MatchPlayer player = this.flag.getMatch().getPlayer(event.getPlayer());
    if (player == null || !player.canInteract() || player.getBukkit().isDead()) return;

    if (this.inPickupRange(player.getBukkit().getLocation()) && this.canPickup(player)) {
      this.pickupFlag(player);
    }
  }

  @Override
  public void onEvent(BlockTransformEvent event) {
    super.onEvent(event);

    Block block = event.getOldState().getBlock();
    Block flagBlock = this.location.getBlock();

    if (block.equals(flagBlock) || block.equals(flagBlock.getRelative(BlockFace.UP))) {
      event.setCancelled(true, new PersonalizedTranslatable("match.flag.cannotBreak"));
    } else if (block.equals(flagBlock.getRelative(BlockFace.DOWN))) {
      event.setCancelled(true, new PersonalizedTranslatable("match.flag.cannotBreakBlockUnder"));
    }
  }

  @Override
  public void onEvent(EntityDamageEvent event) {
    super.onEvent(event);

    if (event.getEntity() == this.labelEntity) {
      event.setCancelled(true);

      if (event instanceof EntityDamageByEntityEvent
          && ((EntityDamageByEntityEvent) event).getDamager() instanceof Projectile) {
        ((EntityDamageByEntityEvent) event).getDamager().remove();
      }
    }
  }

  @Override
  public void tickLoaded() {
    super.tickLoaded();

    if (this.particleClock % 10 == 0) {
      // ASHCON
      /*this.flag.getMatch().getWorld().playEffect(this.getLocation().clone().add(0, 1, 0),
      Effect.PORTAL,
      0, 0,
      0, 0, 0,
      1, 8, 64);*/
    }
  }
}
