package tc.oc.pgm.match;

import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.integration.Integration;
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
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.MaxHealthKit;
import tc.oc.pgm.kits.WalkSpeedKit;
import tc.oc.pgm.modules.SpectateMatchModule;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.ClassLogger;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.attribute.Attribute;
import tc.oc.pgm.util.attribute.AttributeInstance;
import tc.oc.pgm.util.attribute.AttributeMap;
import tc.oc.pgm.util.attribute.AttributeMapImpl;
import tc.oc.pgm.util.attribute.AttributeModifier;
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
  private final AtomicBoolean frozen;
  private final AtomicBoolean dead;
  private final AtomicBoolean visible;
  private final AtomicBoolean protocolReady;
  private final AtomicInteger protocolVersion;
  private final AttributeMap attributeMap;

  public MatchPlayerImpl(Match match, Player player) {
    this.logger =
        ClassLogger.get(
            assertNotNull(match).getLogger(), getClass(), assertNotNull(player).getName());
    this.match = match;
    this.id = player.getUniqueId();
    this.bukkit = new WeakReference<>(player);
    this.audience = Audience.get(player);
    this.party = new AtomicReference<>(null);
    this.frozen = new AtomicBoolean(false);
    this.dead = new AtomicBoolean(false);
    this.visible = new AtomicBoolean(false);
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

  @Nullable
  @Override
  public Location getLocation() { // TODO: move over usages of #getBukkit#getLocation to this
    final Player bukkit = this.getBukkit();
    if (bukkit == null) return null;

    return bukkit.getLocation();
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
    @Nullable MatchPlayer spectatorTarget = this.getSpectatorTarget();
    boolean isSpectatorTarget =
        spectatorTarget != null && spectatorTarget.getId().equals(other.getId());
    if (!other.isVisible() && !isSpectatorTarget) return false;
    if (other.isParticipating()) return true;
    if (Integration.isVanished(other.getBukkit()) && !getBukkit().hasPermission(Permissions.VANISH))
      return false;
    SettingValue setting = getSettings().getValue(SettingKey.OBSERVERS);
    boolean friendsOnly =
        Integration.isFriend(getBukkit(), other.getBukkit())
            && setting == SettingValue.OBSERVERS_FRIEND;
    return isObserving() && (setting == SettingValue.OBSERVERS_ON || friendsOnly);
  }

  @Override
  public void resetInteraction() {
    Player player = getBukkit();
    if (player == null) return;

    boolean interact = canInteract();

    if (!interact) player.leaveVehicle();

    // This is only possible in sportpaper
    NMSHacks.setAffectsSpawning(player, interact);
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

    NMSHacks.showInvisibles(bukkit, isObserving());

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
    bukkit.setMaxHealth(MaxHealthKit.BUKKIT_DEFAULT);
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
    NMSHacks.setKnockbackReduction(bukkit, 0);
    bukkit.setVelocity(new Vector());

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

    if (displacedItems.size() > 0) {
      Collection<ItemStack> leftover =
          getInventory().addItem(displacedItems.toArray(new ItemStack[0])).values();
      if (leftover.size() > 0) {
        kit.applyLeftover(this, new ArrayList<>(leftover));
      }
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
    party.set(newParty);
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
    return player(this, style);
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
  public @NotNull Audience audience() {
    return audience;
  }

  @Nullable
  @Override
  public MatchPlayer getSpectatorTarget() {
    Player bukkit = getBukkit();
    return bukkit == null ? null : match.getPlayer(bukkit.getSpectatorTarget());
  }

  @Override
  public List<MatchPlayer> getSpectators() {
    return match.needModule(SpectateMatchModule.class).getSpectating(this);
  }

  @Override
  @Nullable
  public Party getFilterableParent() {
    return this.getParty();
  }

  @Override
  public Collection<? extends Filterable<? extends PlayerQuery>> getFilterableChildren() {
    return Collections.emptyList();
  }

  @Override
  public int compareTo(MatchPlayer o) {
    final int diff = this.id.compareTo(o.getId());
    if (diff == 0) {
      return this.match.getId().compareTo(o.getMatch().getId());
    }
    return diff;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 31 * hash + this.id.hashCode();
    hash = 31 * hash + this.match.hashCode();
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof MatchPlayer)) return false;
    final MatchPlayer o = (MatchPlayer) obj;
    return this.id.equals(o.getId()) && this.match.equals(o.getMatch());
  }

  @Override
  public String toString() {
    final Player player = this.getBukkit();
    return "MatchPlayer{id="
        + this.id
        + ", player="
        + (player == null ? "<null>" : player.getName())
        + ", match="
        + this.match.getId()
        + "}";
  }
}
