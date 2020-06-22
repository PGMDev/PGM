package tc.oc.pgm.rage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mysql.jdbc.StringUtils;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.entity.EntityType;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class RageModule implements MapModule {
  private static final Collection<MapTag> TAGS =
      ImmutableList.of(MapTag.create("rage", "Rage", true, true));

  private final boolean allProjectiles;
  private final List<EntityType> entities;

  public RageModule(boolean allProjectiles, List<EntityType> entities) {
    this.allProjectiles = allProjectiles;
    this.entities = entities;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new RageMatchModule(match, this.allProjectiles, this.entities);
  }

  @Override
  public Collection<MapTag> getTags() {
    return TAGS;
  }

  public static class Factory implements MapModuleFactory<RageModule> {
    @Override
    public RageModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Element rageEle = doc.getRootElement().getChild("rage");
      if (rageEle != null) {
        List<String> projectileTypes =
            StringUtils.split(
                rageEle.getAttributeValue("projectile", "arrow").toLowerCase(), ",", true);
        boolean all = projectileTypes.contains("all");

        if (projectileTypes.size() > 1 && all)
          throw new InvalidXMLException(
              "The \"all\" projectile type must be used exclusively", rageEle);

        List<EntityType> entities = Lists.newArrayList();
        Node rageNode = new Node(rageEle);
        for (String type : projectileTypes) {
          entities.add(XMLUtils.parseEnum(rageNode, type, EntityType.class, "entity type"));
        }
        return new RageModule(all, entities);
      } else {
        return null;
      }
    }
  }
}
