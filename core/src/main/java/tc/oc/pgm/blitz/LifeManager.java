package tc.oc.pgm.blitz;

import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.Assert.assertTrue;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

public class LifeManager {

  @Getter
  final int lives;

  final Map<UUID, Integer> livesLeft = Maps.newHashMap();

  public LifeManager(int lives) {
    assertTrue(lives > 0, "lives must be greater than zero");

    this.lives = lives;
  }

  public int getLives(UUID player) {
    assertNotNull(player, "player id");

    Integer livesLeft = this.livesLeft.get(player);
    return Objects.requireNonNullElse(livesLeft, this.lives);
  }

  public int addLives(UUID player, int dlives) {
    assertNotNull(player, "player id");

    int lives = Math.max(0, this.getLives(player) + dlives);
    this.livesLeft.put(player, lives);

    return lives;
  }

  public void setLives(UUID player, int lives) {
    assertNotNull(player, "player id");

    this.livesLeft.put(player, Math.max(0, lives));
  }
}
