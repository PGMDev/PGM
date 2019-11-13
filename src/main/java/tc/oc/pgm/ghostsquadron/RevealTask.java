package tc.oc.pgm.ghostsquadron;

import java.util.Iterator;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class RevealTask implements Runnable {
  public RevealTask(GhostSquadronMatchModule matchModule) {
    this.matchModule = matchModule;
  }

  @Override
  public void run() {
    for (Iterator<Map.Entry<Player, RevealEntry>> it =
            this.matchModule.revealMap.entrySet().iterator();
        it.hasNext(); ) {
      Map.Entry<Player, RevealEntry> mapEntry = it.next();

      Player player = mapEntry.getKey();
      RevealEntry entry = mapEntry.getValue();

      entry.potionTicks--;
      entry.revealTicks--;

      if (entry.potionTicks <= 0 || entry.revealTicks <= 0) {
        it.remove();
        if (entry.potionTicks > 0) {
          PotionEffectType.INVISIBILITY.createEffect(entry.potionTicks, 1).apply(player);
        }
      }
    }
  }

  final GhostSquadronMatchModule matchModule;

  public static class RevealEntry {
    public int revealTicks;
    public int potionTicks;

    public RevealEntry() {
      this(0, 0);
    }

    public RevealEntry(int revealTicks, int potionTicks) {
      this.revealTicks = revealTicks;
      this.potionTicks = potionTicks;
    }
  }
}
