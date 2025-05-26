package tc.oc.pgm.platform.modern.dfu;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.schemas.Schema;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import net.minecraft.SharedConstants;
import net.minecraft.util.datafix.DataFixers;
import tc.oc.pgm.util.reflect.ReflectionUtils;

public class PGMDataFixer {
  private static Method ADD_FIXERS_METHOD =
      ReflectionUtils.getMethod(DataFixers.class, "addFixers", DataFixerBuilder.class);
  private static Field DFU_BUILDER_SCHEMAS =
      ReflectionUtils.getField(DataFixerBuilder.class, "schemas");

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

  private static DataFixerBuilder.Result createFixer() {
    var builder = new DataFixerBuilder(
        SharedConstants.getCurrentVersion().getDataVersion().getVersion());

    // Add Mojang's data fixers
    ReflectionUtils.callMethod(ADD_FIXERS_METHOD, null, builder);

    // Give worlds made before 1.18 experimental snapshot 1 a custom dimension limiting height
    var schema1_18_exp1 = getSchemaForDataVersion(builder, 2825);
    builder.addFixer(new LegacyOverworldFix(schema1_18_exp1));

    return builder.build();
  }
}
