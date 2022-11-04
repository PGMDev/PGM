package tc.oc.pgm.structure;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.snapshot.SnapshotMatchModule;

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
    for (StructureDefinition structure : structures.values()) {
      smm.saveRegion(structure.getRegion());

      if (structure.clearSource())
        smm.removeBlocks(structure.getRegion(), new BlockVector(), structure.includeAir());
    }

    FilterMatchModule fmm = match.needModule(FilterMatchModule.class);
    for (DynamicStructureDefinition dynamicDefinition : dynamics) {
      DynamicStructure dynamicStructure = new DynamicStructure(dynamicDefinition, match);

      match.getFeatureContext().add(dynamicStructure);
      final DynamicStructureDefinition dynamicDef = dynamicStructure.getDefinition();
      fmm.onChange(
          Match.class,
          dynamicDef.getTrigger(),
          (m, response) -> {
            if (response) {
              // This is the passive filter, not the dynamic one
              if (dynamicDef.getPassive().query(m).isAllowed()) {
                this.queuePlace(dynamicStructure);
              }
            } else {
              this.queueClear(dynamicStructure);
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
