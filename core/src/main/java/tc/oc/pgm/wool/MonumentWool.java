package tc.oc.pgm.wool;

import java.util.Collections;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.event.PlayerItemTransferEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.ProximityMetric;
import tc.oc.pgm.goals.TouchableGoal;
import tc.oc.pgm.kits.ApplyItemKitEvent;
import tc.oc.pgm.kits.ApplyKitEvent;
import tc.oc.pgm.kits.ArmorKit;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;
import tc.oc.pgm.util.named.NameStyle;

public class MonumentWool extends TouchableGoal<MonumentWoolFactory>
    implements Goal<MonumentWoolFactory> {

  public static final String SYMBOL_WOOL_INCOMPLETE = "\u2b1c"; // ⬜
  public static final String SYMBOL_WOOL_TOUCHED = "\u2592"; // ▒
  public static final String SYMBOL_WOOL_COMPLETE = "\u2b1b"; // ⬛

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
    StringBuilder sb = new StringBuilder("MonumentWool{");
    sb.append("id=").append(this.getId());
    sb.append(",owner=").append(this.owner.getName());
    sb.append(",definition=").append(this.definition);
    sb.append('}');
    return sb.toString();
  }

  // Remove @Nullable
  @Override
  public Team getOwner() {
    return super.getOwner();
  }

  @Override
  public Component getTouchMessage(ParticipantState toucher, boolean self) {
    return new PersonalizedTranslatable(
        self ? "wool.touch.owned.you" : "wool.touch.owned.player",
        toucher.getStyledName(NameStyle.COLOR),
        getComponentName(),
        toucher.getParty().getComponentName());
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
    return !hasTouched(getOwner())
        || (oldState.getType() == Material.AIR
            && this.getDefinition().isObjectiveWool(newState.getData()));
  }

  public void handleWoolAcquisition(Player player, ItemStack item) {
    if (!this.isPlaced() && this.getDefinition().isObjectiveWool(item)) {
      ParticipantState participant = this.getMatch().getParticipantState(player);
      if (participant != null && this.canComplete(participant.getParty())) {
        touch(participant);

        // Initialize monument proximity
        ProximityMetric metric = getProximityMetric(participant.getParty());
        if (metric != null) {
          switch (metric.type) {
            case CLOSEST_BLOCK:
              updateProximity(participant, this.woolLocation);
              break;
            case CLOSEST_PLAYER:
              updateProximity(participant, player.getLocation());
              break;
          }
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onItemTransfer(PlayerItemTransferEvent event) {
    if (event.isAcquiring()) handleWoolAcquisition(event.getPlayer(), event.getItemStack());
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
      for (ArmorKit.ArmorItem armorPiece : ((ArmorKit) event.getKit()).getArmor().values()) {
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
  public ChatColor renderSidebarStatusColor(@Nullable Competitor competitor, Party viewer) {
    if (getDyeColor() == DyeColor.BLUE) {
      return ChatColor.DARK_BLUE; // DARK_BLUE looks ok on sidebar, but not in chat
    } else {
      return BukkitUtils.dyeColorToChatColor(this.getDyeColor());
    }
  }

  @Override
  public String renderSidebarStatusText(@Nullable Competitor competitor, Party viewer) {
    if (this.isCompleted()) {
      return SYMBOL_WOOL_COMPLETE;
    } else if (shouldShowTouched(competitor, viewer)) {
      return SYMBOL_WOOL_TOUCHED;
    } else {
      return SYMBOL_WOOL_INCOMPLETE;
    }
  }
}
