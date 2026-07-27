package tc.oc.pgm.structure;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.regions.CuboidRegion;
import tc.oc.pgm.snapshot.WorldSnapshot;

public final class StructureTest {

  @Test
  public void placeAbsoluteAlignsDefaultOriginWithTarget() {
    Region region = new CuboidRegion(new Vector(5, 0, 5), new Vector(6, 1, 6));
    StructureDefinition definition =
        new StructureDefinition("test-structure", null, region, true, false);
    RecordingWorldSnapshot snapshot = new RecordingWorldSnapshot();
    Structure structure = new Structure(definition, null, snapshot);

    structure.placeAbsolute(new BlockVector(5, 0, 5), false);

    assertBlockCoordinates(0, 0, 0, snapshot.offset);
  }

  @Test
  public void placeAbsoluteAlignsConfiguredOriginWithTarget() {
    Region region = new CuboidRegion(new Vector(5, 0, 5), new Vector(6, 1, 6));
    StructureDefinition definition =
        new StructureDefinition("test-structure", new Vector(4, -1, 4), region, true, false);
    RecordingWorldSnapshot snapshot = new RecordingWorldSnapshot();
    Structure structure = new Structure(definition, null, snapshot);

    structure.placeAbsolute(new BlockVector(5, 0, 5), false);

    assertBlockCoordinates(1, 1, 1, snapshot.offset);
  }

  private static void assertBlockCoordinates(
      int expectedX, int expectedY, int expectedZ, BlockVector actual) {
    assertAll(
        () -> assertEquals(expectedX, actual.getBlockX()),
        () -> assertEquals(expectedY, actual.getBlockY()),
        () -> assertEquals(expectedZ, actual.getBlockZ()));
  }

  private static final class RecordingWorldSnapshot extends WorldSnapshot {

    private BlockVector offset;

    private RecordingWorldSnapshot() {
      super(null);
    }

    @Override
    public void saveRegion(Region region) {}

    @Override
    public void placeBlocks(Region region, BlockVector offset, boolean update) {
      this.offset = offset.clone();
    }
  }
}
