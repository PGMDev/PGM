package tc.oc.pgm.enderchest;

import java.util.logging.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class EnderChestModule implements MapModule {

  private final boolean enabled;
  private final int rows;

  public EnderChestModule(boolean enabled, int rows) {
    this.enabled = enabled;
    this.rows = rows;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new EnderChestMatchModule(match, enabled, rows);
  }

  public static class Factory implements MapModuleFactory<EnderChestModule> {
    @Override
    public EnderChestModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      boolean enabled = false;
      int rows = 3;

      for (Element enderRootEl : doc.getRootElement().getChildren("enderchest")) {
        Attribute rowAttr = XMLUtils.getAttribute(enderRootEl, "rows");
        if (rowAttr != null) rows = XMLUtils.parseNumber(rowAttr, Integer.class);

        if (rows < 1 || rows > 6)
          throw new InvalidXMLException("Row amount must be between 1 and 6", enderRootEl);

        enabled = true;
      }

      return new EnderChestModule(enabled, rows);
    }
  }
}
