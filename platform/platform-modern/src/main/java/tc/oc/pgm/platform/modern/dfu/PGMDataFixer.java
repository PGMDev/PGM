package tc.oc.pgm.platform.modern.dfu;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.DataFixerUpper;
import com.mojang.datafixers.schemas.Schema;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import net.minecraft.util.datafix.DataFixers;
import tc.oc.pgm.util.DataVersions;
import tc.oc.pgm.util.reflect.ReflectionUtils;

public class PGMDataFixer {
  private static final DataFixerUpper DATA_FIXER = (DataFixerUpper) DataFixers.getDataFixer();
  private static final IntSortedSet DFU_FIXER_VERSIONS = ReflectionUtils.readField(
      DataFixerUpper.class, DATA_FIXER, IntSortedSet.class, "fixerVersions");

  @SuppressWarnings("unchecked")
  private static final Int2ObjectSortedMap<Schema> DFU_SCHEMAS = ReflectionUtils.readField(
      DataFixerUpper.class, DATA_FIXER, Int2ObjectSortedMap.class, "schemas");

  @SuppressWarnings("unchecked")
  private static final List<DataFix> DFU_FIXER_LIST =
      ReflectionUtils.readField(DataFixerUpper.class, DATA_FIXER, List.class, "globalList");

  private static Schema getSchemaForDataVersion(int dataVersion) {
    return DFU_SCHEMAS.get(
        DataFixUtils.makeKey(dataVersion, 0)); // The data version is the versioning key
  }

  private static void registerDataFix(DataFix dataFix) {
    var insertionIdx = Collections.binarySearch(
        DFU_FIXER_LIST, dataFix, Comparator.comparingInt(DataFix::getVersionKey));
    if (insertionIdx < 0) insertionIdx = -(insertionIdx + 1);
    DFU_FIXER_LIST.add(insertionIdx, dataFix);
    DFU_FIXER_VERSIONS.add(dataFix.getVersionKey());
  }

  private static void injectDataFix(int dataVersion, Function<Schema, DataFix> createDataFix) {
    var schema = getSchemaForDataVersion(dataVersion);
    var dataFix = createDataFix.apply(schema);
    registerDataFix(dataFix);
  }

  public static void hookMojangDFU() {
    injectDataFix(DataVersions.V1_18_EXP_1, LegacyOverworldFix::new);
  }
}
