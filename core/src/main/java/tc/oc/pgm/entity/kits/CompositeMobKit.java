package tc.oc.pgm.entity.kits;

import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.jdom2.Element;
import tc.oc.pgm.util.xml.InvalidXMLException;

public record CompositeMobKit(List<MobKit> kits) implements MobKit {

  @Override
  public void apply(LivingEntity entity) {
    for (MobKit kit : kits) {
      kit.apply(entity);
    }
  }

  @Override
  public void validate(Class<? extends LivingEntity> mobType, Element source)
      throws InvalidXMLException {
    for (MobKit kit : kits) {
      kit.validate(mobType, source);
    }
  }
}
