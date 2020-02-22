package tc.oc.pgm.kits;

import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;

public class GameModeKit extends AbstractKit {

  private final GameMode gameMode;

  public GameModeKit(GameMode mode) {
    gameMode = mode;
  }

  @Override
  protected void applyPostEvent(MatchPlayer player, boolean force, List<ItemStack> displacedItems) {
    player.setGameMode(gameMode);
  }
}
