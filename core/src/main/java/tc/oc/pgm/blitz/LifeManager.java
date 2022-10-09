package tc.oc.pgm.blitz;

import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.Assert.assertTrue;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.UUID;

public class LifeManager {

  final int lives;
  final Map<UUID, Integer> livesLeft = Maps.newHashMap();

  public LifeManager(int lives) {
    assertTrue(lives > 0, "lives must be greater than zero");

    this.lives = lives;
  }

  public int getLives() {
    return this.lives;
  }

  public int getLives(UUID player) {
    assertNotNull(player, "player id");

    Integer livesLeft = this.livesLeft.get(player);
    if (livesLeft != null) {
      return livesLeft;
    } else {
      return this.lives;
    }
  }

  public int addLives(UUID player, int dlives) {
    assertNotNull(player, "player id");

    int lives = Math.max(0, this.getLives(player) + dlives);
    this.livesLeft.put(player, lives);

    return lives;
  }
}
