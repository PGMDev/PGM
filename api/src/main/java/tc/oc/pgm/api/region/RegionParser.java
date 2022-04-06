package tc.oc.pgm.api.region;

import java.util.List;
import javax.annotation.Nullable;
import org.jdom2.Attribute;
import org.jdom2.Element;
import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.api.xml.InvalidXMLException;
import tc.oc.pgm.api.xml.Node;

public interface RegionParser {
  /**
   * Parse a single region element and return it. Also, store the region in whatever type of context
   * is in use.
   */
  Region parse(Element el) throws InvalidXMLException;

  Region parseReference(Attribute attr) throws InvalidXMLException;

  List<Element> getRegionChildren(Element parent);

  Region parseChild(Element parent) throws InvalidXMLException;

  Region parseChildren(Element parent) throws InvalidXMLException;

  Region[] parseSubRegionsArray(Element parent) throws InvalidXMLException;

  List<Region> parseSubRegions(Element parent) throws InvalidXMLException;

  @Nullable
  Region parseRegionProperty(Element rootElement, String... names) throws InvalidXMLException;

  @Nullable
  Region parseRegionProperty(Element rootElement, Region def, String... names)
      throws InvalidXMLException;

  @Nullable
  Region parseRegionProperty(
      Element rootElement,
      @Nullable FeatureValidation<RegionDefinition> validation,
      String... names)
      throws InvalidXMLException;

  @Nullable
  Region parseRegionProperty(
      Element rootElement,
      @Nullable FeatureValidation<RegionDefinition> validation,
      Region def,
      String... names)
      throws InvalidXMLException;

  Region parseRequiredRegionProperty(Element rootElement, String... names)
      throws InvalidXMLException;

  Region parseRequiredRegionProperty(
      Element rootElement,
      @Nullable FeatureValidation<RegionDefinition> validation,
      String... names)
      throws InvalidXMLException;

  void validate(Region region, FeatureValidation<RegionDefinition> validation, Node node)
      throws InvalidXMLException;

  boolean isRegion(Element el);

  public Region parsePoint(Element el) throws InvalidXMLException;
}
