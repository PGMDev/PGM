package tc.oc.pgm.util.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import tc.oc.pgm.util.LiquidMetal;
import tc.oc.pgm.util.StringUtils;

public class LiquidMetalTest {

  @Test
  public void testLiquidMetal() {
    List<String> maps =
        Arrays.asList(
            "Golden Drought",
            "Golden Drought II",
            "Golden Drought III",
            "Golden Drought III TE",
            "Race for Victory",
            "Race for Victory 2",
            "Race for Victory 2 TE");

    // All these should be found when searching for "gd"
    for (String map : maps.subList(0, 4)) {
      assertTrue(LiquidMetal.match(map, "gd"));
      assertTrue(LiquidMetal.match(map, "g d"));
    }

    for (String map : maps.subList(4, 7)) {
      assertTrue(LiquidMetal.match(map, "rv"));
      assertTrue(LiquidMetal.match(map, "rfv"));
    }

    // Ensurer the search returns the most-specific query possible
    assertEquals("Golden Drought", StringUtils.bestFuzzyMatch("gd", maps));
    assertEquals("Golden Drought II", StringUtils.bestFuzzyMatch("gdI", maps));
    assertEquals("Golden Drought III", StringUtils.bestFuzzyMatch("gdIII", maps));
    assertEquals("Golden Drought III TE", StringUtils.bestFuzzyMatch("gdIIIte", maps));

    assertEquals("Race for Victory", StringUtils.bestFuzzyMatch("rv", maps));
    assertEquals("Race for Victory", StringUtils.bestFuzzyMatch("rfv", maps));
    assertEquals("Race for Victory 2", StringUtils.bestFuzzyMatch("rfv2", maps));
    assertEquals("Race for Victory 2 TE", StringUtils.bestFuzzyMatch("rfv2te", maps));

    assertFalse(LiquidMetal.match("Golden Drought TE", "g i "));
    assertTrue(LiquidMetal.match("Golden Drought III TE", "g i "));
  }
}
