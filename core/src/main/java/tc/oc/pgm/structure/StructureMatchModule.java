package tc.oc.pgm.structure;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.snapshot.SnapshotMatchModule;
import tc.oc.pgm.snapshot.WorldSnapshot;

public class StructureMatchModule implements MatchModule {

  private final Match match;
  private final Map<String, StructureDefinition> structures;
  private final List<DynamicStructureDefinition> dynamics;
  private final Queue<DynamicStructure> clearQueue;
  private final Queue<DynamicStructure> placeQueue;

  public StructureMatchModule(
      Match match,
      Map<String, StructureDefinition> structures,
      List<DynamicStructureDefinition> dynamics) {
    this.match = match;
    this.structures = structures;
    this.dynamics = dynamics;
    Comparator<DynamicStructure> order =
        Comparator.comparing(
            dynamicStructure -> this.dynamics.indexOf(dynamicStructure.getDefinition()));

    this.clearQueue = new PriorityQueue<>(order);
    this.placeQueue = new PriorityQueue<>(order);
  }

  @Override
  public void load() throws ModuleLoadException {
    SnapshotMatchModule smm = match.needModule(SnapshotMatchModule.class);
    WorldSnapshot originalWorld = smm.getOriginalSnapshot();

    boolean anyClears = false;

    for (StructureDefinition def : structures.values()) {
      Structure structure = new Structure(def, match, originalWorld);
      match.getFeatureContext().add(structure);

      anyClears |= def.clearSource();
    }

    // If no clears happened, then we're fine to use the same world snapshot.
    // Otherwise, use a different one for dynamic clear
    WorldSnapshot afterClear = anyClears ? new WorldSnapshot(match.getWorld()) : originalWorld;

    FilterMatchModule fmm = match.needModule(FilterMatchModule.class);
    for (DynamicStructureDefinition def : dynamics) {
      Structure struct =
          match.getFeatureContext().get(def.getStructureDefinition().getId(), Structure.class);

      DynamicStructure dynamic = new DynamicStructure(def, struct, afterClear);

      match.getFeatureContext().add(dynamic);

      fmm.onChange(
          Match.class,
          dynamic.getDefinition().getTrigger(),
          (m, response) -> {
            if (response) {
              // This is the passive filter, not the dynamic one
              if (dynamic.getDefinition().getPassive().query(m).isAllowed()) {
                this.queuePlace(dynamic);
              }
            } else {
              this.queueClear(dynamic);
            }
          });
    }
  }

  private void queuePlace(DynamicStructure dynamicStructure) {
    clearQueue.remove(dynamicStructure);
    placeQueue.add(dynamicStructure);
    schedule();
  }

  private void queueClear(DynamicStructure dynamicStructure) {
    placeQueue.remove(dynamicStructure);
    clearQueue.add(dynamicStructure);
    schedule();
  }

  private void schedule() {
    match.getExecutor(MatchScope.LOADED).submit(this::process);
  }

  /**
   * We need to make sure that dynamics are placed and cleared in definition order, and that clears
   * happen before placements.
   */
  private void process() {
    while (!clearQueue.isEmpty()) {
      clearQueue.poll().clear();
    }

    while (!placeQueue.isEmpty()) {
      placeQueue.poll().place();
    }
  }
}
