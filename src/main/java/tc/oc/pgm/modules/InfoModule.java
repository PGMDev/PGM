package tc.oc.pgm.modules;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Difficulty;
import org.bukkit.World.Environment;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.pgm.map.Contributor;
import tc.oc.pgm.map.MapInfo;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.map.ProtoVersions;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.util.SemanticVersion;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

@ModuleDescription(name = "Info")
public class InfoModule extends MapModule {

  private final MapInfo info;

  public InfoModule(MapInfo info) {
    this.info = info;
  }

  public MapInfo getMapInfo() {
    return this.info;
  }

  public static InfoModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    Element root = doc.getRootElement();

    String name = Node.fromRequiredChildOrAttr(root, "name").getValueNormalize();
    SemanticVersion version =
        XMLUtils.parseSemanticVersion(Node.fromRequiredChildOrAttr(root, "version"));

    // Allow multiple <objective> elements, so include files can provide defaults
    String objective = null;
    for (Element elObjective : root.getChildren("objective")) {
      objective = elObjective.getTextNormalize();
    }
    if (objective == null) {
      throw new InvalidXMLException("'objective' element is required", root);
    }

    String slug = root.getChildTextNormalize("slug");
    Component game = XMLUtils.parseFormattedText(root, "game");

    List<Contributor> authors = readContributorList(root, "authors", "author");
    if (authors.isEmpty()) {
      throw new InvalidXMLException("map must have at least one author", root);
    }

    if (game != null) {
      Element blitz = root.getChild("blitz");
      if (blitz != null) {
        Element title = blitz.getChild("title");
        if (title != null) {
          if (context.getProto().isNoOlderThan(ProtoVersions.REMOVE_BLITZ_TITLE)) {
            throw new InvalidXMLException(
                "<title> inside <blitz> is no longer supported, use <map game=\"...\">", title);
          }
          game = new PersonalizedText(title.getTextNormalize());
        }
      }
    }

    List<Contributor> contributors = readContributorList(root, "contributors", "contributor");

    List<String> rules = new ArrayList<String>();
    for (Element parent : root.getChildren("rules")) {
      for (Element rule : parent.getChildren("rule")) {
        rules.add(rule.getTextNormalize());
      }
    }

    Difficulty difficulty =
        XMLUtils.parseEnum(
            Node.fromLastChildOrAttr(root, "difficulty"), Difficulty.class, "difficulty");

    Environment dimension =
        XMLUtils.parseEnum(
            Node.fromLastChildOrAttr(root, "dimension"),
            Environment.class,
            "dimension",
            Environment.NORMAL);

    boolean friendlyFire =
        XMLUtils.parseBoolean(
            Node.fromLastChildOrAttr(root, "friendly-fire", "friendlyfire"), false);

    return new InfoModule(
        new MapInfo(
            context.getProto(),
            slug,
            name,
            version,
            game,
            objective,
            authors,
            contributors,
            rules,
            difficulty,
            dimension,
            friendlyFire));
  }

  private static List<Contributor> readContributorList(Element root, String topLevelTag, String tag)
      throws InvalidXMLException {
    List<Contributor> contribs = new ArrayList<Contributor>();
    for (Element parent : root.getChildren(topLevelTag)) {
      for (Element child : parent.getChildren(tag)) {
        String name = XMLUtils.getNormalizedNullableText(child);
        UUID uuid = XMLUtils.parseUuid(Node.fromAttr(child, "uuid"));
        String contribution = XMLUtils.getNullableAttribute(child, "contribution", "contrib");

        if (name == null && uuid == null) {
          throw new InvalidXMLException("Contributor must have either a name or UUID", child);
        }

        contribs.add(new Contributor(uuid, name, contribution));
      }
    }
    return contribs;
  }
}
