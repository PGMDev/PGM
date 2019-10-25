package tc.oc.pgm.match;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.joda.time.Duration;
import org.joda.time.Instant;
import tc.oc.chat.Audience;
import tc.oc.chat.Sound;
import tc.oc.component.Component;
import tc.oc.component.render.ComponentRenderers;
import tc.oc.component.types.PersonalizedPlayer;
import tc.oc.component.types.PersonalizedText;
import tc.oc.identity.Identities;
import tc.oc.named.NameStyle;
import tc.oc.named.Named;
import tc.oc.pgm.events.PlayerResetEvent;
import tc.oc.pgm.filters.query.IPlayerQuery;
import tc.oc.pgm.filters.query.PlayerQuery;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.WalkSpeedKit;
import tc.oc.util.logging.ClassLogger;
import tc.oc.world.DeathOverride;
import tc.oc.world.NMSHacks;

/**
 * MatchPlayer represents a player who is part of a match. Note that the MatchPlayer object should
 * only exist as long as the corresponding Match instance exists.
 *
 * <p>MatchPlayer stores all information that is necessary for the core plugin.
 */
public class MatchPlayer implements InventoryHolder, Tickable, Audience, Named {

  private static final Sound ERROR_SOUND = new Sound("note.bass", 1f, 0.75f);

  private static final Duration ERROR_SOUND_COOLDOWN = Duration.standardSeconds(10);
  private static final Duration SPARK_SOUND_COOLDOWN = Duration.standardSeconds(1);

  protected final Logger logger;
  protected final Player bukkit;
  protected final Match match;
  protected Party party;

  protected Instant lastErrorSoundTime;
  protected Instant lastSparkSoundTime;
  protected int killStreak;
  protected boolean frozen;
  protected boolean dead;
  protected boolean visible;

  protected @Nullable IPlayerQuery query;

  // WARNING: team is not set here and must be set IMMEDIATELY after construction
  public MatchPlayer(Player bukkit, Match match) {
    this.logger = ClassLogger.get(match.getLogger(), getClass(), bukkit.getName());
    this.bukkit = bukkit;
    this.match = match;
  }

  @Override
  public String toString() {
    return this.getName();
  }

  @Override
  public PlayerInventory getInventory() {
    return this.bukkit.getInventory();
  }

  public Match getMatch() {
    return this.match;
  }

  @Override
  public World getWorld() {
    return getMatch().getWorld();
  }

  public Player getBukkit() {
    return this.bukkit;
  }

  public UUID getPlayerId() {
    return getBukkit().getUniqueId();
  }

  /** Reverse-chronological list of match commitments within the last 24 hours */
  public List<Instant> recentMatchCommitments() {
    return new ArrayList<>();
  }

  public Party getParty() {
    return this.party;
  }

  public @Nullable Competitor getCompetitor() {
    return party instanceof Competitor ? (Competitor) party : null;
  }

  public MatchPlayerState getState() {
    if (match == null || party == null) {
      return null;
    } else if (party instanceof Competitor) {
      return new ParticipantState(
          match,
          Identities.current(bukkit),
          bukkit.getUniqueId(),
          (Competitor) party,
          bukkit.getLocation());
    } else {
      return new MatchPlayerState(
          match, Identities.current(bukkit), bukkit.getUniqueId(), party, bukkit.getLocation());
    }
  }

  public @Nullable ParticipantState getParticipantState() {
    if (match != null && party instanceof Competitor) {
      return new ParticipantState(
          match,
          Identities.current(bukkit),
          bukkit.getUniqueId(),
          (Competitor) party,
          bukkit.getLocation());
    } else {
      return null;
    }
  }

  /**
   * Return a query for this player, with a null event field. Returned query is invalidated when the
   * player changes parties.
   */
  public IPlayerQuery getQuery() {
    if (query == null) query = new PlayerQuery(null, this);
    return query;
  }

  /**
   * Called ONLY by {@link Match#setPlayerParty}. The match is not necessarily in a consistent state
   * when this method is called, so it should do nothing except update internal data structures. Any
   * other reactions to changing parties should be implemented with an event handler.
   */
  protected void setParty(@Nullable Party newParty) {
    if (party != newParty) {
      this.query = null;
      this.party = newParty;
    }
  }

  /** Called when the player is committed to the match. See {@link Match#commit()} for details. */
  protected void commit() {}

  /** Get the cumulative time the player has participated in this match */
  public Duration getCumulativeParticipationTime() {
    return getMatch().getParticipationClock().getCumulativePresence(getPlayerId());
  }

  /**
   * Get the cumulative percentage of the match running time in which the player has participated
   */
  public double getCumulativeParticipationPercent() {
    return getMatch().getParticipationClock().getCumulativePresencePercent(getPlayerId());
  }

  public boolean isParticipatingType() {
    return party != null && party.isParticipatingType();
  }

  public boolean isParticipating() {
    return party != null && party.isParticipating();
  }

  public boolean isObservingType() {
    return party != null && party.isObservingType();
  }

  public boolean isObserving() {
    return party != null && party.isObserving();
  }

  public boolean isCommitted() {
    return isParticipatingType() && getMatch().isCommitted();
  }

  public String getDisplayName() {
    return this.getBukkit().getDisplayName();
  }

  public String getDisplayName(Player viewer) {
    return this.getBukkit().getDisplayName(viewer);
  }

  public String getDisplayName(MatchPlayer viewer) {
    return this.getDisplayName(viewer.getBukkit());
  }

  public boolean canInteract() {
    return this.isParticipating() && !this.isDead() && !this.isFrozen();
  }

  public int getKillStreak() {
    return killStreak;
  }

  public void setKillStreak(int killStreak) {
    this.killStreak = killStreak;
  }

  public boolean isDead() {
    return dead;
  }

  public void setDead(boolean dead) {
    this.dead = dead;
    DeathOverride.setDead(bukkit, dead);
  }

  public boolean isAlive() {
    return isParticipating() && !isDead();
  }

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  public boolean isFrozen() {
    return frozen;
  }

  public void setFrozen(boolean yes) {
    if (frozen != yes) {
      if (frozen = yes) {
        refreshGameMode();

        NMSHacks.EntityMetadata metadata = NMSHacks.createEntityMetadata();
        NMSHacks.setEntityMetadata(metadata, false, false, false, false, true, (short) 0);
        NMSHacks.setArmorStandFlags(metadata, false, false, false, false);
        NMSHacks.spawnLivingEntity(
            bukkit,
            EntityType.ARMOR_STAND,
            match.freezeEntityId,
            bukkit.getLocation().subtract(0, 1.1, 0),
            metadata);
        NMSHacks.entityAttach(bukkit, bukkit.getEntityId(), match.freezeEntityId, false);
      } else {
        NMSHacks.destroyEntities(bukkit, match.freezeEntityId);

        refreshGameMode();
      }
    }
  }

  public void refreshGameMode() {
    boolean participating = this.canInteract();
    logger.fine("Refreshing gamemode as " + (participating ? "participant" : "observer"));

    if (!participating) this.getBukkit().leaveVehicle();
    this.getBukkit().setGameMode(participating ? GameMode.SURVIVAL : GameMode.CREATIVE);
    this.getBukkit().setAllowFlight(!participating);
    this.getBukkit().spigot().setAffectsSpawning(participating);
    this.getBukkit().spigot().setCollidesWithEntities(participating);
    this.getBukkit().setDisplayName(this.getDisplayName());
    this.updateVisibility();
  }

  public void clearInventory() {
    bukkit.getInventory().clear();
    bukkit.getInventory().setArmorContents(null);
  }

  public void reset() {
    this.getMatch().getPluginManager().callEvent(new PlayerResetEvent(this));
    setFrozen(false);
    Player bukkit = this.getBukkit();
    bukkit.closeInventory();
    clearInventory();
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
    this.resetPotions();
    NMSHacks.resetAttributes(bukkit);
    NMSHacks.setAbsorption(bukkit, 0);

    // we only reset bed spawn here so people don't have to see annoying messages when they respawn
    bukkit.setBedSpawnLocation(null);
  }

  public void resetPotions() {
    for (PotionEffect effect : bukkit.getActivePotionEffects()) {
      if (effect.getType() != null) {
        bukkit.removePotionEffect(effect.getType());
      }
    }
  }

  public static boolean canSee(MatchPlayer source, MatchPlayer target) {
    return (source.isObserving() || target.isParticipating()) && target.isVisible();
  }

  public void updateVisibility() {
    this.getBukkit().showInvisibles(this.isObserving());

    for (MatchPlayer other : this.match.getPlayers()) {
      if (canSee(this, other)) {
        this.showPlayer(other);
      } else {
        this.hidePlayer(other);
      }

      if (canSee(other, this)) {
        other.showPlayer(this);
      } else {
        other.hidePlayer(this);
      }
    }
  }

  @Override
  public void tick(Match match) {
    if (isFrozen()) {
      // If the player right-clicks on another vehicle while frozen, the client will
      // eject them from the freeze entity unconditionally, so we have to spam them
      // with these packets to keep them on it.
      NMSHacks.entityAttach(bukkit, bukkit.getEntityId(), match.freezeEntityId, false);
    }
  }

  public boolean canJoinFull() {
    return getMatch().needMatchModule(JoinMatchModule.class).canJoinFull(this);
  }

  public boolean canPriorityKick() {
    return getMatch().needMatchModule(JoinMatchModule.class).canPriorityKick(this);
  }

  public String getName() {
    return this.getBukkit().getName();
  }

  public String getName(CommandSender viewer) {
    return this.getBukkit().getName(viewer);
  }

  public String getName(MatchPlayer viewer) {
    return this.getName(viewer.getBukkit());
  }

  public ChatColor getColor() {
    return party == null ? ChatColor.AQUA : party.getColor();
  }

  public String getColoredName() {
    return getColor() + getName();
  }

  public String getColoredName(CommandSender viewer) {
    return getColor() + getName(viewer);
  }

  public String getColoredName(MatchPlayer viewer) {
    return getColor() + getName(viewer);
  }

  public Component getComponentName() {
    return getStyledName(NameStyle.COLOR);
  }

  @Override
  public Component getStyledName(NameStyle style) {
    return new PersonalizedPlayer(getBukkit(), style);
  }

  @Override
  public void sendMessage(String message) {
    this.getBukkit().sendMessage(message);
  }

  @Override
  public void sendMessage(Component message) {
    ComponentRenderers.send(this.getBukkit(), checkNotNull(message));
  }

  @Override
  public void sendHotbarMessage(Component message) {
    NMSHacks.sendHotbarMessage(this.getBukkit(), message);
  }

  @Override
  public void showTitle(
      Component title, Component subtitle, int inTicks, int stayTicks, int outTicks) {
    title = title == null ? new PersonalizedText("") : title;
    subtitle = subtitle == null ? new PersonalizedText("") : subtitle;
    this.getBukkit()
        .showTitle(
            title.render(getBukkit()), subtitle.render(getBukkit()), inTicks, stayTicks, outTicks);
  }

  @Override
  public void sendWarning(String message, boolean audible) {
    this.bukkit.sendMessage(
        ChatColor.YELLOW + " \u26a0 " + ChatColor.RED + message); // The character is '⚠'
    if (audible) playWarningSound();
  }

  @Override
  public void sendWarning(Component message, boolean audible) {
    sendMessage(
        new PersonalizedText(net.md_5.bungee.api.ChatColor.RED)
            .extra(
                new PersonalizedText(" \u26a0 ", net.md_5.bungee.api.ChatColor.YELLOW), message));
    if (audible) playWarningSound();
  }

  @Override
  public void playSound(Sound sound) {
    this.playSound(sound, this.getBukkit().getLocation());
  }

  public void sendMessage(List<String> lines) {
    for (String line : lines) {
      this.sendMessage(line);
    }
  }

  public void sendWarning(String message) {
    this.sendWarning(message, false);
  }

  public void sendWarning(Component message) {
    this.sendWarning(message, false);
  }

  public boolean playWarningSound() {
    Instant now = Instant.now();
    if (this.lastErrorSoundTime == null
        || this.lastErrorSoundTime.isBefore(now.minus(ERROR_SOUND_COOLDOWN))) {
      this.lastErrorSoundTime = now;
      this.playSound(ERROR_SOUND);
      return true;
    }
    return false;
  }

  public void playSound(String sound, Location location, float volume, float pitch) {
    this.getBukkit().playSound(location, sound, volume, pitch);
  }

  public void playSound(Sound sound, Location location) {
    this.playSound(sound.name, location, sound.volume, sound.pitch);
  }

  public void playSound(org.bukkit.Sound sound, Location location, float volume, float pitch) {
    this.getBukkit().playSound(location, sound, volume, pitch);
  }

  public void playSound(org.bukkit.Sound sound, float volume, float pitch) {
    this.playSound(sound, this.getBukkit().getLocation(), volume, pitch);
  }

  public void playSound(org.bukkit.Sound sound, Location location) {
    this.playSound(sound, location, 1, 1);
  }

  public void playSound(org.bukkit.Sound sound) {
    this.playSound(sound, this.getBukkit().getLocation());
  }

  public void playSparks() {
    if (this.lastSparkSoundTime == null
        || this.lastSparkSoundTime.isBefore(Instant.now().minus(SPARK_SOUND_COOLDOWN))) {
      this.playSound(org.bukkit.Sound.FIREWORK_BLAST2, 1, 1);
      this.playSound(org.bukkit.Sound.FIREWORK_TWINKLE2, 1, 1);
    }
  }

  public void showPlayer(MatchPlayer other) {
    this.getBukkit().showPlayer(other.getBukkit());
  }

  public void hidePlayer(MatchPlayer other) {
    this.getBukkit().hidePlayer(other.getBukkit());
  }

  public void applyKit(Kit kit, boolean force) {
    List<ItemStack> displacedItems = new ArrayList<>();
    kit.apply(this, force, displacedItems);
    for (ItemStack stack : displacedItems) {
      this.getBukkit().getInventory().addItem(stack);
    }

    /**
     * When max health is lowered by an item attribute or potion effect, the client can go into an
     * inconsistent state that has strange effects, like the death animation playing when the player
     * isn't dead. It is probably related to this bug:
     *
     * <p>https://bugs.mojang.com/browse/MC-19690
     *
     * <p>This appears to fix the client state, for reasons that are unclear. The one tick delay is
     * necessary. Any less and getMaxHealth will not reflect whatever was applied in the kit to
     * modify it.
     */
    getMatch()
        .getScheduler(MatchScope.LOADED)
        .runTaskLater(
            1,
            new Runnable() {
              @Override
              public void run() {
                if (getBukkit().isOnline() && !isDead() && getBukkit().getMaxHealth() < 20) {
                  getBukkit()
                      .setHealth(Math.min(getBukkit().getHealth(), getBukkit().getMaxHealth()));
                }
              }
            });
  }
}
