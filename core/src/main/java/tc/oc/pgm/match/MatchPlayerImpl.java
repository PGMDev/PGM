package tc.oc.pgm.match;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.api.setting.Settings;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.events.PlayerResetEvent;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.WalkSpeedKit;
import tc.oc.util.ClassLogger;
import tc.oc.util.bukkit.ViaUtils;
import tc.oc.util.bukkit.chat.PlayerAudience;
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.component.types.PersonalizedPlayer;
import tc.oc.util.bukkit.named.NameStyle;
import tc.oc.util.bukkit.nms.DeathOverride;
import tc.oc.util.bukkit.nms.NMSHacks;

public class MatchPlayerImpl implements MatchPlayer, PlayerAudience, Comparable<MatchPlayer> {

  // TODO: Probably should be moved to a better location
  private static final int FROZEN_VEHICLE_ENTITY_ID = NMSHacks.allocateEntityId();
  private final Logger logger;
  private final Match match;
  private final UUID id;
  private final WeakReference<Player> bukkit;
  private final AtomicReference<Party> party;
  private final AtomicReference<PlayerQuery> query;
  private final AtomicBoolean frozen;
  private final AtomicBoolean dead;
  private final AtomicBoolean visible;
  private final AtomicInteger protocolVersion;

  public MatchPlayerImpl(Match match, Player player) {
    this.logger =
        ClassLogger.get(
            checkNotNull(match).getLogger(), getClass(), checkNotNull(player).getName());
    this.match = match;
    this.id = player.getUniqueId();
    this.bukkit = new WeakReference<>(player);
    this.party = new AtomicReference<>(null);
    this.query = new AtomicReference<>(null);
    this.frozen = new AtomicBoolean(false);
    this.dead = new AtomicBoolean(false);
    this.visible = new AtomicBoolean(false);
    this.protocolVersion = new AtomicInteger(ViaUtils.getProtocolVersion(player));
  }

  @Override
  public Match getMatch() {
    return match;
  }

  @Override
  public Party getParty() {
    return party.get();
  }

  @Nullable
  @Override
  public Competitor getCompetitor() {
    final Party party = getParty();
    if (party instanceof Competitor) return (Competitor) party;
    return null;
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public MatchPlayerState getState() {
    final Party party = getParty();
    if (party == null) {
      return null;
    } else if (party instanceof Competitor) {
      return getParticipantState();
    } else {
      return new MatchPlayerStateImpl(this);
    }
  }

  @Nullable
  @Override
  public ParticipantState getParticipantState() {
    final Competitor competitor = getCompetitor();
    if (competitor == null) {
      return null;
    } else {
      return new ParticipantStateImpl(this);
    }
  }

  @Override
  public PlayerQuery getQuery() {
    return query.get();
  }

  @Nullable
  @Override
  public Player getBukkit() {
    return bukkit.get();
  }

  @Override
  public boolean isParticipating() {
    final Party party = getParty();
    return party != null && party.isParticipating();
  }

  @Override
  public boolean isObserving() {
    final Party party = getParty();
    return party != null && party.isObserving();
  }

  @Override
  public boolean isDead() {
    return dead.get();
  }

  @Override
  public boolean isAlive() {
    return !isDead() && isParticipating();
  }

  @Override
  public boolean isVisible() {
    return visible.get();
  }

  @Override
  public boolean isFrozen() {
    return frozen.get();
  }

  @Override
  public boolean canInteract() {
    return isAlive() && !isFrozen();
  }

  @Override
  public boolean canSee(MatchPlayer other) {
    if (!other.isVisible()) return false;
    if (other.isParticipating()) return true;
    return isObserving()
        && getSettings().getValue(SettingKey.OBSERVERS) == SettingValue.OBSERVERS_ON;
  }

  @Override
  public void resetGamemode() {
    boolean participating = canInteract();
    logger.fine("Refreshing gamemode as " + (participating ? "participant" : "observer"));

    if (!participating) getBukkit().leaveVehicle();
    setGameMode(participating ? GameMode.SURVIVAL : GameMode.CREATIVE);
    this.getBukkit().setAllowFlight(!participating);
    this.getBukkit().spigot().setAffectsSpawning(participating);
    this.getBukkit().spigot().setCollidesWithEntities(participating);
    this.getBukkit().setDisplayName(getBukkit().getDisplayName());
    this.resetVisibility();
  }

  @Override
  public void resetInventory() {
    getInventory().clear();
    getInventory().setArmorContents(null);
  }

  @Override
  public void resetVisibility() {
    final Player bukkit = getBukkit();

    bukkit.showInvisibles(isObserving());

    for (MatchPlayer other : getMatch().getPlayers()) {
      if (canSee(other)) {
        bukkit.showPlayer(other.getBukkit());
      } else {
        bukkit.hidePlayer(other.getBukkit());
      }

      if (other.canSee(this)) {
        other.getBukkit().showPlayer(getBukkit());
      } else {
        other.getBukkit().hidePlayer(getBukkit());
      }
    }
  }

  @Override
  public void reset() {
    getMatch().callEvent(new PlayerResetEvent(this));

    setFrozen(false);
    Player bukkit = getBukkit();
    bukkit.closeInventory();
    resetInventory();
    bukkit.setArrowsStuck(0);
    bukkit.setExhaustion(0);
    bukkit.setFallDistance(0);
    bukkit.setFireTicks(0);
    bukkit.setFoodLevel(20); // full
    bukkit.setHealth(bukkit.getMaxHealth());
    bukkit.setLevel(0);
    bukkit.setExp(0); // clear xp
    bukkit.setSaturation(5); // default
    bukkit.setAllowFlight(false);
    bukkit.setFlying(false);
    bukkit.setSneaking(false);
    bukkit.setSprinting(false);
    bukkit.setFlySpeed(0.1f);
    bukkit.setKnockbackReduction(0);
    bukkit.setWalkSpeed(WalkSpeedKit.BUKKIT_DEFAULT);

    for (PotionEffect effect : bukkit.getActivePotionEffects()) {
      if (effect.getType() != null) {
        bukkit.removePotionEffect(effect.getType());
      }
    }

    NMSHacks.resetAttributes(bukkit);
    NMSHacks.setAbsorption(bukkit, 0);

    // we only reset bed spawn here so people don't have to see annoying messages when they respawn
    bukkit.setBedSpawnLocation(null);
  }

  @Override
  public void setDead(boolean yes) {
    dead.set(yes);
    DeathOverride.setDead(getBukkit(), yes);
  }

  @Override
  public void setVisible(boolean yes) {
    visible.set(yes);
  }

  @Override
  public void setFrozen(boolean yes) {
    if (frozen.compareAndSet(!yes, yes)) {
      Player bukkit = getBukkit();
      if (yes) {
        resetGamemode();

        NMSHacks.EntityMetadata metadata = NMSHacks.createEntityMetadata();
        NMSHacks.setEntityMetadata(metadata, false, false, false, false, true, (short) 0);
        NMSHacks.setArmorStandFlags(metadata, false, false, false, false);
        NMSHacks.spawnLivingEntity(
            bukkit,
            EntityType.ARMOR_STAND,
            FROZEN_VEHICLE_ENTITY_ID,
            bukkit.getLocation().subtract(0, 1.1, 0),
            metadata);
        NMSHacks.entityAttach(bukkit, bukkit.getEntityId(), FROZEN_VEHICLE_ENTITY_ID, false);
      } else {
        NMSHacks.destroyEntities(bukkit, FROZEN_VEHICLE_ENTITY_ID);

        resetGamemode();
      }
    }
  }

  @Override
  public void setGameMode(GameMode gameMode) {
    getBukkit().setGameMode(gameMode);
  }

  /**
   * When max health is lowered by an item attribute or potion effect, the client can go into an
   * inconsistent state that has strange effects, like the death animation playing when the player
   * isn't dead. It is probably related to this bug:
   *
   * <p>https://bugs.mojang.com/browse/MC-19690
   *
   * <p>This appears to fix the client state, for reasons that are unclear. The one tick delay is
   * necessary. Any less and getMaxHealth will not reflect whatever was applied in the kit to modify
   * it.
   */
  @Override
  public void applyKit(Kit kit, boolean force) {
    List<ItemStack> displacedItems = new ArrayList<>();
    kit.apply(this, force, displacedItems);
    for (ItemStack stack : displacedItems) {
      getInventory().addItem(stack);
    }

    getMatch()
        .getScheduler(MatchScope.LOADED)
        .runTaskLater(
            1,
            new Runnable() {
              @Override
              public void run() {
                final Player bukkit = getBukkit();
                if (bukkit.isOnline() && !isDead() && bukkit.getMaxHealth() < 20) {
                  bukkit.setHealth(Math.min(bukkit.getHealth(), bukkit.getMaxHealth()));
                }
              }
            });
  }

  @Override
  public int getProtocolVersion() {
    return protocolVersion.get();
  }

  @Override
  public Settings getSettings() {
    return PGM.get().getDatastore().getSettings(id);
  }

  @Override
  public void internalSetParty(Party newParty) {
    if (party.compareAndSet(getParty(), newParty)) {
      query.set(new tc.oc.pgm.filters.query.PlayerQuery(null, this));
    }
  }

  @Override
  public PlayerInventory getInventory() {
    return getBukkit().getInventory();
  }

  @Override
  public World getWorld() {
    return getMatch().getWorld();
  }

  @Override
  public Component getStyledName(NameStyle style) {
    return new PersonalizedPlayer(getBukkit(), style);
  }

  @Override
  public String getPrefixedName() {
    return PGM.get().getPrefixRegistry().getPrefixedName(getBukkit(), getParty());
  }

  @Override
  public GameMode getGameMode() {
    return getBukkit().getGameMode();
  }

  @Override
  public void tick(Match match, Tick tick) {
    final Player bukkit = getBukkit();
    if (isFrozen()) {
      // If the player right-clicks on another vehicle while frozen, the client will
      // eject them from the freeze entity unconditionally, so we have to spam them
      // with these packets to keep them on it.
      NMSHacks.entityAttach(bukkit, bukkit.getEntityId(), FROZEN_VEHICLE_ENTITY_ID, false);
    }
  }

  @Override
  public Player getAudience() {
    return getBukkit();
  }

  @Override
  public int compareTo(MatchPlayer o) {
    return new CompareToBuilder()
        .append(getMatch(), o.getMatch())
        .append(getId(), o.getId())
        .build();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(getMatch()).append(getId()).build();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof MatchPlayer)) return false;
    final MatchPlayer o = (MatchPlayer) obj;
    return new EqualsBuilder()
        .append(getMatch(), o.getMatch())
        .append(getId(), o.getId())
        .isEquals();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("id", getId())
        .append("bukkit", getBukkit())
        .append("match", getMatch().getId())
        .build();
  }
}
