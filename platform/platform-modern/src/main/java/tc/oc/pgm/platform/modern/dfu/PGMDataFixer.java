package tc.oc.pgm.platform.modern.dfu;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.schemas.Schema;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import net.minecraft.SharedConstants;
import net.minecraft.util.datafix.DataFixers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tc.oc.pgm.platform.modern.util.UnsafeReflection;
import tc.oc.pgm.util.reflect.ReflectionUtils;

public class PGMDataFixer {
  private static Field MOJANG_DATA_FIXER_FIELD =
      ReflectionUtils.getField(DataFixers.class, "DATA_FIXER");
  private static Method ADD_FIXERS_METHOD =
      ReflectionUtils.getMethod(DataFixers.class, "addFixers", DataFixerBuilder.class);
  private static Field DFU_BUILDER_SCHEMAS =
      ReflectionUtils.getField(DataFixerBuilder.class, "schemas");
  private static Field DFU_BUILDER_LIST =
      ReflectionUtils.getField(DataFixerBuilder.class, "globalList");

  private static final Logger LOGGER = LogManager.getLogger(PGMDataFixer.class);

  private static final DataFixerBuilder.Result DATA_FIXER = createFixer();

  public static DataFixer getFixer() {
    return DATA_FIXER.fixer();
  }

  public static CompletableFuture<?> optimize(Set<DSL.TypeReference> refs) {
    if (refs.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    } else {
      Executor singleThreadExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
          .setNameFormat("PGM Datafixer Bootstrap")
          .setDaemon(true)
          .setPriority(1)
          .build());
      return DATA_FIXER.optimize(refs, singleThreadExecutor);
    }
  }

  @SuppressWarnings("unchecked")
  private static Schema getSchemaForDataVersion(DataFixerBuilder builder, int dataVersion) {
    try {
      var schemas = (Int2ObjectSortedMap<Schema>) DFU_BUILDER_SCHEMAS.get(builder);
      return schemas.get(
          DataFixUtils.makeKey(dataVersion, 0)); // The data version is the versioning key
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static void sortBuilderFixes(DataFixerBuilder builder) {
    try {
      var fixes = (List<DataFix>) DFU_BUILDER_LIST.get(builder);
      fixes.sort(Comparator.comparingInt(DataFix::getVersionKey));
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private static DataFixerBuilder.Result createFixer() {
    var builder = new DataFixerBuilder(
        SharedConstants.getCurrentVersion().getDataVersion().getVersion());

    // Add Mojang's data fixers
    ReflectionUtils.callMethod(ADD_FIXERS_METHOD, null, builder);

    // Set a custom dimension on worlds made before 1.18 experimental snapshot 1 to avoid issues
    // with block connections
    var schema1_18_exp1 = getSchemaForDataVersion(builder, 2825);
    builder.addFixer(new LegacyOverworldFix(schema1_18_exp1));

    // Sorting the fixes is required, otherwise the fix won't be applied
    sortBuilderFixes(builder);

    return builder.build();
  }

  public static void hookMojangDFU() {
    try {
      UnsafeReflection.setStaticFieldUnsafe(MOJANG_DATA_FIXER_FIELD, DATA_FIXER);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }

    try {
      if (MOJANG_DATA_FIXER_FIELD.get(null) != DATA_FIXER
          || DataFixers.getDataFixer() != PGMDataFixer.getFixer()) {
        LOGGER.warn(
            "Hooking into Mojang's DFU failed. Unexpected things might happen to your cat.");
      } else {
        LOGGER.info("Successfully hooked into DFU.");
      }
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
