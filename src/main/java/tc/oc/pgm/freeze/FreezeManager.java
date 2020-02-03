package tc.oc.pgm.freeze;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.chat.Sound;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.match.ObservingParty;

/** Controls player freezing and those in freeze mode */
public class FreezeManager {
  private static final Sound FREEZE_SOUND = new Sound("mob.enderdragon.growl", 1f, 2f);

  private static FreezeManager freezeManager;

  public static FreezeManager get() {
    if (freezeManager == null) freezeManager = new FreezeManager();
    return freezeManager;
  }

  private Map<Player, Boolean> freezeModeMap = new HashMap<>();
  private Set<Player> frozenPlayers = new HashSet<>();

  public void toggleFreeze(Player freezer, Player freezee) {
    boolean isFrozen = isFrozen(freezee);
    if (isFrozen) unfreezePlayer(freezer, freezee);
    else freezePlayer(freezer, freezee);
  }

  public void freezePlayer(Player freezer, Player freezee) {
    if (freezee.hasPermission(Permissions.FREEZE)) {
      freezer.sendMessage(
          ChatColor.RED
              + AllTranslations.get()
                  .translate("command.freeze.exempt", freezer, freezee.getName()));
      return;
    }
    frozenPlayers.add(freezee);
    PGM.get().getMatchManager().getPlayer(freezee).playSound(FREEZE_SOUND);
    freezee.sendMessage(
        ChatColor.AQUA
            + AllTranslations.get().translate("freeze.frozen", freezee, freezer.getName()));
    PGM.get()
        .getChatDispatcher()
        .sendAdmin(
            PGM.get().getMatchManager().getMatch(freezee),
            PGM.get().getMatchManager().getPlayer(freezer),
            ChatColor.AQUA
                + AllTranslations.get()
                    .translate("adminchat.freezePlayer", freezer, freezee.getName()));
  }

  public void unfreezePlayer(Player freezer, Player freezee) {
    frozenPlayers.remove(freezee);
    freezee.sendMessage(
        ChatColor.AQUA
            + AllTranslations.get().translate("freeze.unfrozen", freezee, freezer.getName()));
    PGM.get()
        .getChatDispatcher()
        .sendAdmin(
            PGM.get().getMatchManager().getMatch(freezee),
            PGM.get().getMatchManager().getPlayer(freezer),
            ChatColor.AQUA
                + AllTranslations.get()
                    .translate("adminchat.unfreezePlayer", freezer, freezee.getName()));
  }

  public boolean isFrozen(Player player) {
    return frozenPlayers.contains(player);
  }

  public void setFreezeMode(Player player, boolean mode) {
    freezeModeMap.put(player, mode);
    if (mode) enableFreezeMode(player);
    else disableFreezeMode(player);
  }

  public boolean getFreezeMode(Player player) {
    return Config.Moderation.freezingEnabled() && freezeModeMap.getOrDefault(player, false);
  }

  private void enableFreezeMode(Player player) {
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    if (matchPlayer == null) return;
    if (matchPlayer.getParty() instanceof ObservingParty) {
      player.sendMessage(
          ChatColor.AQUA + AllTranslations.get().translate("freeze.freezeMode", player));
    } else {
      player.sendMessage(
          ChatColor.AQUA + AllTranslations.get().translate("freeze.freezeMode.nextMatch", player));
    }
  }

  private void disableFreezeMode(Player player) {
    player.getInventory().remove(Material.ICE);
  }
}
