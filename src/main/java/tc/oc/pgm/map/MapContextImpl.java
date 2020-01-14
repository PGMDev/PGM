package tc.oc.pgm.map;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.module.ModuleContext;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class MapContextImpl extends MapInfoExtraImpl implements MapContext {

  private final MapSource source;
  private final Map<Class<? extends MapModule>, MapModule> modules;

  public MapContextImpl(MapInfo info, MapSource source, ModuleContext<MapModule> context) {
    super(info, context);
    this.source = checkNotNull(source);
    this.modules = ImmutableMap.copyOf(context.getModules());
  }

  @Override
  public MapSource getSource() {
    return source;
  }

  @Override
  public Map<Class<? extends MapModule>, MapModule> getModules() {
    return modules;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(getSource()).append(getModules()).build();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof MapContext)) return false;
    final MapContext o = (MapContext) obj;
    return new EqualsBuilder().append(getSource(), o.getSource()).append(getModules(), o.getModules()).build();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("id", getId())
        .append("source", getSource().getId())
        .append("modules", getModules().size())
        .build();
  }
}
