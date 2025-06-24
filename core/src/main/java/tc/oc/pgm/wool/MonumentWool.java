package tc.oc.pgm.wool;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import java.util.Collections;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.TouchableGoal;
import tc.oc.pgm.kits.ApplyItemKitEvent;
import tc.oc.pgm.kits.ApplyKitEvent;
import tc.oc.pgm.kits.ArmorKit;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.event.PlayerItemTransferEvent;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;

public class MonumentWool extends TouchableGoal<MonumentWoolFactory>
    implements Goal<MonumentWoolFactory> {

  public static final Component SYMBOL_WOOL_INCOMPLETE = text("\u2b1c"); // ⬜
  public static final Component SYMBOL_WOOL_TOUCHED = text("\u2592"); // ▒
  public static final Component SYMBOL_WOOL_COMPLETE = text("\u2b1b"); // ⬛

  protected boolean placed = false;
  private final Location woolLocation;
  private final Location monumentLocation;

  public MonumentWool(MonumentWoolFactory definition, Match match) {
    super(definition, match);
    this.woolLocation = definition.getLocation().toLocation(match.getWorld());
    this.monumentLocation =
        definition.getPlacementRegion().getBounds().getCenterPoint().toLocation(match.getWorld());
  }

  @Override
  public String toString() {
    return "MonumentWool{" + "id=" + this.getId() + ",owner="
        + this.owner.getNameLegacy() + ",definition="
        + this.definition + '}';
  }

  // Remove @Nullable
  @Override
  public Team getOwner() {
    Team owner = super.getOwner();
    if (owner == null) {
      throw new IllegalStateException("wool " + getId() + " has no owner");
    }
    return owner;
  }

  @Override
  public Component getTouchMessage(ParticipantState toucher, boolean self) {
    return translatable(
        self ? "wool.touch.owned.you" : "wool.touch.owned.player",
        toucher.getName(NameStyle.COLOR),
        getComponentName(),
        toucher.getParty().getName());
  }

  @Override
  public Iterable<Location> getProximityLocations(ParticipantState player) {
    if (hasTouched(player.getParty())) {
      // After the wool has been touched, the goal is located at the monument
      return Collections.singleton(monumentLocation);
    } else {
      // Before the wool has been touched, the goal is located at the wool
      return Collections.singleton(woolLocation);
    }
  }

  @Override
  protected boolean canPlayerUpdateProximity(ParticipantState player) {
    // Wool proximity is affected by all players, while monument proximity only counts for wool
    // runners
    if (!super.canPlayerUpdateProximity(player)) return false;
    if (!hasTouched(player.getParty())) return true;
    MatchPlayer onlinePlayer = player.getPlayer().orElse(null);
    return onlinePlayer != null && this.getDefinition().isHolding(onlinePlayer);
  }

  @Override
  protected boolean canBlockUpdateProximity(BlockState oldState, BlockState newState) {
    // If monument proximity metric is closest block, make it only the wool
    return !hasTouched(getOwner()) || this.getDefinition().isObjectiveWool(newState);
  }

  public void handleWoolAcquisition(Player player, ItemStack item) {
    if (!this.isPlaced() && this.getDefinition().isObjectiveWool(item)) {
      ParticipantState participant = this.getMatch().getParticipantState(player);
      if (participant != null && this.canComplete(participant.getParty())) {
        touch(participant);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onItemTransfer(PlayerItemTransferEvent event) {
    if (event.isAcquiring()) handleWoolAcquisition(event.getPlayer(), event.getItem());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onItemKitApplication(ApplyItemKitEvent event) {
    for (ItemStack item : event.getItems()) {
      handleWoolAcquisition(event.getPlayer().getBukkit(), item);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onArmorKitApplication(ApplyKitEvent event) {
    if (event.getKit() instanceof ArmorKit) {
      for (ArmorKit.ArmorItem armorPiece :
          ((ArmorKit) event.getKit()).getArmor().values()) {
        handleWoolAcquisition(event.getPlayer().getBukkit(), armorPiece.stack);
      }
    }
  }

  public DyeColor getDyeColor() {
    return this.definition.getColor();
  }

  public boolean isPlaced() {
    return this.placed;
  }

  public void markPlaced() {
    this.placed = true;
  }

  @Override
  public boolean isShared() {
    return false;
  }

  @Override
  public boolean canComplete(Competitor team) {
    return team == this.getOwner();
  }

  @Override
  public boolean isCompleted() {
    return this.placed;
  }

  @Override
  public boolean isCompleted(Competitor team) {
    return this.placed && this.canComplete(team);
  }

  @Override
  public TextColor renderSidebarStatusColor(@Nullable Competitor competitor, Party viewer) {
    if (getDyeColor() == DyeColor.BLUE) {
      return NamedTextColor.DARK_BLUE; // DARK_BLUE looks ok on sidebar, but not in chat
    } else {
      return TextFormatter.convert(BukkitUtils.dyeColorToChatColor(this.getDyeColor()));
    }
  }

  @Override
  public Component renderSidebarStatusText(@Nullable Competitor competitor, Party viewer) {
    if (this.isCompleted()) {
      return SYMBOL_WOOL_COMPLETE;
    } else if (shouldShowTouched(competitor, viewer)) {
      return SYMBOL_WOOL_TOUCHED;
    } else {
      return SYMBOL_WOOL_INCOMPLETE;
    }
  }
}
