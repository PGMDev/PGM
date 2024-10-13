package tc.oc.pgm.platform.modern.registry;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEventType;
import io.papermc.paper.registry.PaperRegistryListenerManager;
import io.papermc.paper.registry.RegistryBuilder;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.event.RegistryEventProvider;
import io.papermc.paper.registry.event.RegistryFreezeEvent;
import io.papermc.paper.registry.event.type.RegistryEntryAddEventType;

/**
 * Analogous class to {@link io.papermc.paper.registry.event.RegistryEvents} but for registries that
 * don't have events
 */
@SuppressWarnings("UnstableApiUsage")
public class PgmRegistryEvents {

  public static final RegistryEventProvider<CraftDimensionType, CraftDimensionType.Builder>
      DIMENSION_TYPE = new RegistryEventProviderImpl<>(PgmRegistryKey.DIMENSION_TYPE);

  /** Copy of paper's (non-public) impl */
  @SuppressWarnings("NonExtendableApiUsage")
  record RegistryEventProviderImpl<T, B extends RegistryBuilder<T>>(RegistryKey<T> registryKey)
      implements RegistryEventProvider<T, B> {
    public RegistryEntryAddEventType<T, B> entryAdd() {
      return PaperRegistryListenerManager.INSTANCE.getRegistryValueAddEventType(this);
    }

    public LifecycleEventType.Prioritizable<BootstrapContext, RegistryFreezeEvent<T, B>> freeze() {
      return PaperRegistryListenerManager.INSTANCE.getRegistryFreezeEventType(this);
    }
  }
}
