package tc.oc.pgm.platform.modern.dfu;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.util.datafix.fixes.References;

public class LegacyOverworldFix extends DataFix {
  public LegacyOverworldFix(Schema outputSchema) {
    super(outputSchema, false);
  }

  @Override
  protected TypeRewriteRule makeRule() {
    var chunkType = getInputSchema().getType(References.CHUNK);

    var outputChunkType = getOutputSchema().getType(References.CHUNK);

    return this.fixTypeEverywhereTyped(
        "[PGM] Use legacy overworld dimension on old worlds",
        chunkType,
        outputChunkType,
        c -> c.update(DSL.remainderFinder(), chunkData -> {
          var chunkCtxOpt = chunkData.get("__context").result();

          if (chunkCtxOpt.isPresent()) {
            var chunkCtx = chunkCtxOpt.get();
            var dimension = chunkCtx.get("dimension").asString().result().orElse("");

            if ("minecraft:overworld".equals(dimension)) {
              chunkCtx.set("dimension", chunkCtx.createString("pgm:legacy_overworld"));
            }
          }

          return chunkData;
        }));
  }
}
