package tc.oc.pgm.match;

import static com.google.common.base.Preconditions.checkNotNull;
import static tc.oc.pgm.util.text.PlayerComponent.player;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
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
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.ClassLogger;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.attribute.Attribute;
import tc.oc.pgm.util.attribute.AttributeInstance;
import tc.oc.pgm.util.attribute.AttributeMap;
import tc.oc.pgm.util.attribute.AttributeMapImpl;
import tc.oc.pgm.util.attribute.AttributeModifier;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.bukkit.ViaUtils;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.nms.NMSHacks;

public class MatchPlayerImpl implements MatchPlayer, Comparable<MatchPlayer> {

  // TODO: Probably should be moved to a better location
  private static final int FROZEN_VEHICLE_ENTITY_ID = NMSHacks.allocateEntityId();

  private static final String DEATH_KEY = "isDead";
  private static final MetadataValue DEATH_VALUE = new FixedMetadataValue(PGM.get(), true);

  private final Logger logger;
  private final Match match;
  private final UUID id;
  private final WeakReference<Player> bukkit;
  private final Audience audience;
  private final AtomicReference<Party> party;
  private final AtomicReference<PlayerQuery> query;
  private final AtomicBoolean frozen;
  private final AtomicBoolean dead;
  private final AtomicBoolean visible;
  private final AtomicBoolean protocolReady;
  private final AtomicInteger protocolVersion;
  private final AtomicBoolean vanished;
  private final AttributeMap attributeMap;

  public MatchPlayerImpl(Match match, Player player) {
    this.logger =
        ClassLogger.get(
            checkNotNull(match).getLogger(), getClass(), checkNotNull(player).getName());
    this.match = match;
    this.id = player.getUniqueId();
    this.bukkit = new WeakReference<>(player);
    this.audience = Audience.get(player);
    this.party = new AtomicReference<>(null);
    this.query = new AtomicReference<>(null);
    this.frozen = new AtomicBoolean(false);
    this.dead = new AtomicBoolean(false);
    this.visible = new AtomicBoolean(false);
    this.vanished = new AtomicBoolean(false);
    this.protocolReady = new AtomicBoolean(ViaUtils.isReady(player));
    this.protocolVersion = new AtomicInteger(ViaUtils.getProtocolVersion(player));
    this.attributeMap = new AttributeMapImpl(player);
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
  public boolean isVanished() {
    return vanished.get();
  }

  @Override
  public boolean canInteract() {
    return isAlive() && !isFrozen();
  }

  @Override
  public boolean canSee(MatchPlayer other) {
    if (!other.isVisible()) return false;
    if (other.isParticipating()) return true;
    if (other.isVanished() && !getBukkit().hasPermission(Permissions.VANISH)) return false;
    return isObserving()
        && getSettings().getValue(SettingKey.OBSERVERS) == SettingValue.OBSERVERS_ON;
  }

  @Override
  public void resetInteraction() {
    Player player = getBukkit();
    if (player == null) return;

    boolean interact = canInteract();

    if (!interact) player.leaveVehicle();

    // This is only possible in sportpaper
    if (BukkitUtils.isSportPaper()) {
      player.spigot().setAffectsSpawning(interact);
    }
    player.spigot().setCollidesWithEntities(interact);
  }

  @Override
  public void resetInventory() {
    getInventory().clear();
    getInventory().setArmorContents(null);
  }

  @Override
  public void resetVisibility() {
    final Player bukkit = getBukkit();
    if (bukkit == null) return;

    // Not relevant and not a method outside of SportPaper
    if (BukkitUtils.isSportPaper()) {
      bukkit.showInvisibles(isObserving());
    }

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
    Player bukkit = getBukkit();
    if (bukkit == null) return;

    bukkit.closeInventory();
    resetInventory();
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
    bukkit.setWalkSpeed(WalkSpeedKit.BUKKIT_DEFAULT);
    NMSHacks.clearArrowsInPlayer(bukkit);

    // Not possible outside of SportPaper
    if (BukkitUtils.isSportPaper()) {
      bukkit.setKnockbackReduction(0);
    }

    for (PotionEffect effect : bukkit.getActivePotionEffects()) {
      if (effect.getType() != null) {
        bukkit.removePotionEffect(effect.getType());
      }
    }

    for (Attribute attribute : Attribute.values()) {
      AttributeInstance attributes = getAttribute(attribute);
      if (attributes == null) continue;

      for (AttributeModifier modifier : attributes.getModifiers()) {
        attributes.removeModifier(modifier);
      }
    }

    NMSHacks.setAbsorption(bukkit, 0);

    // we only reset bed spawn here so people don't have to see annoying messages when they respawn
    bukkit.setBedSpawnLocation(null);

    getMatch().callEvent(new PlayerResetEvent(this));
  }

  @Override
  public void setDead(boolean yes) {
    if (dead.compareAndSet(!yes, yes)) {
      if (yes) {
        getBukkit().setMetadata(DEATH_KEY, DEATH_VALUE);
      } else {
        getBukkit().removeMetadata(DEATH_KEY, DEATH_VALUE.getOwningPlugin());
      }
    }
  }

  @Override
  public void setVisible(boolean yes) {
    visible.set(yes);
  }

  @Override
  public void setFrozen(boolean yes) {
    if (frozen.compareAndSet(!yes, yes)) {
      Player bukkit = getBukkit();
      if (bukkit == null) return;

      if (yes) {
        NMSHacks.spawnFreezeEntity(bukkit, FROZEN_VEHICLE_ENTITY_ID, isLegacy());
        NMSHacks.entityAttach(bukkit, bukkit.getEntityId(), FROZEN_VEHICLE_ENTITY_ID, false);
      } else {
        NMSHacks.destroyEntities(bukkit, FROZEN_VEHICLE_ENTITY_ID);
      }
      resetInteraction();
    }
  }

  @Override
  public void setGameMode(GameMode gameMode) {
    getBukkit().setGameMode(gameMode);
  }

  @Override
  public void setVanished(boolean yes) {
    vanished.set(yes);
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

    match
        .getExecutor(MatchScope.LOADED)
        .schedule(
            () -> {
              final Player bukkit = getBukkit();
              if (bukkit.isOnline() && !isDead() && bukkit.getMaxHealth() < 20) {
                bukkit.setHealth(Math.min(bukkit.getHealth(), bukkit.getMaxHealth()));
              }
            },
            TimeUtils.TICK,
            TimeUnit.MILLISECONDS);
  }

  @Override
  public int getProtocolVersion() {
    if (!protocolReady.get()) {
      protocolReady.set(ViaUtils.isReady(getBukkit()));
      protocolVersion.set(ViaUtils.getProtocolVersion(getBukkit()));
    }
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
  public Component getName(NameStyle style) {
    return player(getBukkit(), style);
  }

  @Override
  public String getNameLegacy() {
    return getBukkit().getName();
  }

  @Override
  public String getPrefixedName() {
    return PGM.get()
        .getNameDecorationRegistry()
        .getDecoratedName(getBukkit(), getParty().getColor());
  }

  @Override
  public AttributeInstance getAttribute(Attribute attribute) {
    return attributeMap.getAttribute(attribute);
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
  public @Nonnull Audience audience() {
    return audience;
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
