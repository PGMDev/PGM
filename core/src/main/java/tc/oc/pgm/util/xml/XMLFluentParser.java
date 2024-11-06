package tc.oc.pgm.util.xml;

import java.time.Duration;
import org.bukkit.Material;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jdom2.Element;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.action.ActionParser;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.KitParser;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.util.math.Formula;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextParser;
import tc.oc.pgm.util.xml.parsers.BoolBuilder;
import tc.oc.pgm.util.xml.parsers.Builder;
import tc.oc.pgm.util.xml.parsers.FilterBuilder;
import tc.oc.pgm.util.xml.parsers.ItemBuilder;
import tc.oc.pgm.util.xml.parsers.NumberBuilder;
import tc.oc.pgm.util.xml.parsers.PrimitiveBuilder;
import tc.oc.pgm.util.xml.parsers.ReferenceBuilder;
import tc.oc.pgm.util.xml.parsers.RegionBuilder;
import tc.oc.pgm.util.xml.parsers.StringBuilder;
import tc.oc.pgm.util.xml.parsers.VariableBuilder;
import tc.oc.pgm.variables.VariablesModule;

public class XMLFluentParser {

  private final MapFactory factory;
  private FeatureDefinitionContext features;
  private ActionParser actions;
  private FilterParser filters;
  private RegionParser regions;
  private KitParser kits;
  private VariablesModule variables;

  public XMLFluentParser(MapFactory factory) {
    this.factory = factory;
  }

  // This is required to avoid recursive initialization
  // ie: fluent parser -> action parser -> fluent parser
  public void init() {
    this.features = factory.getFeatures();
    this.actions = new ActionParser(factory);
    this.filters = factory.getFilters();
    this.regions = factory.getRegions();
    this.kits = factory.getKits();
    this.variables = factory.needModule(VariablesModule.class);
  }

  public BoolBuilder parseBool(Element el, String... prop) {
    return new BoolBuilder(el, prop);
  }

  public <T extends Enum<T>> PrimitiveBuilder.Generic<T> parseEnum(
      Class<T> type, Element el, String... prop) {
    return new PrimitiveBuilder.Generic<T>(el, prop) {
      @Override
      protected T parse(String text) throws TextException {
        return TextParser.parseEnum(text, type);
      }
    };
  }

  public StringBuilder string(Element el, String... prop) {
    return new StringBuilder(el, prop);
  }

  public PrimitiveBuilder.Generic<Duration> duration(Element el, String... prop) {
    return new PrimitiveBuilder.Generic<>(el, prop) {
      @Override
      protected Duration parse(String text) throws TextException {
        return TextParser.parseDuration(text);
      }
    };
  }

  public NumberBuilder<Integer> parseInt(Element el, String... prop) {
    return number(Integer.class, el, prop);
  }

  public NumberBuilder<Double> parseDouble(Element el, String... prop) {
    return number(Double.class, el, prop);
  }

  public <T extends Number> NumberBuilder<T> number(Class<T> cls, Element el, String... prop) {
    return new NumberBuilder<>(cls, el, prop);
  }

  public Builder.Generic<Material> material(Element el, String... prop) {
    return new Builder.Generic<>(el, prop) {
      @Override
      protected Material parse(Node node) throws InvalidXMLException {
        return XMLUtils.parseMaterial(node);
      }
    };
  }

  public Builder.Generic<Vector> vector(Element el, String... prop) {
    return new Builder.Generic<>(el, prop) {
      @Override
      protected Vector parse(Node node) throws InvalidXMLException {
        return XMLUtils.parseVector(node);
      }
    };
  }

  public Builder.Generic<BlockVector> blockVector(Element el, String... prop) {
    return new Builder.Generic<>(el, prop) {
      @Override
      protected BlockVector parse(Node node) throws InvalidXMLException {
        return XMLUtils.parseBlockVector(node);
      }
    };
  }

  public FilterBuilder filter(Element el, String... prop) {
    return new FilterBuilder(filters, el, prop);
  }

  public RegionBuilder region(Element el, String... prop) {
    return new RegionBuilder(regions, el, prop);
  }

  public ItemBuilder item(Element el, String... prop) {
    return new ItemBuilder(kits, el, prop);
  }

  public Builder.Generic<Kit> kit(Element el, String... prop) {
    return new Builder.Generic<>(el, prop) {
      @Override
      protected Kit parse(Node node) throws InvalidXMLException {
        return node.isAttribute()
            ? kits.parseReference(node, node.getValue())
            : kits.parse(node.getElement());
      }
    };
  }

  public <T extends FeatureDefinition> ReferenceBuilder<T> reference(
      Class<T> clazz, Element el, String... prop) {
    return new ReferenceBuilder<T>(features, clazz, el, prop);
  }

  public VariableBuilder<?> variable(Element el, String... prop) {
    return new VariableBuilder<>(features, el, prop);
  }

  public <T extends Filterable<?>> Builder.Generic<Action<? super T>> action(
      Class<T> clazz, Element el, String... prop) {
    return new Builder.Generic<>(el, prop) {
      @Override
      protected Action<? super T> parse(Node node) throws InvalidXMLException {
        return node.isAttribute()
            ? actions.parseReference(node, clazz)
            : actions.parseProperty(node.getElement(), clazz);
      }
    };
  }

  public ActionParser getActionParser() {
    return actions;
  }

  public <T extends Filterable<?>> Builder<Formula<T>, ?> formula(
      Class<T> clazz, Element el, String... prop) {
    return new Builder.Generic<>(el, prop) {
      @Override
      protected Formula<T> parse(Node node) {
        return Formula.of(node.getValue(), variables.getContext(clazz));
      }
    };
  }
}
