package tc.oc.pgm.platform.modern.particle;

import static tc.oc.pgm.platform.modern.particle.shapes.ParticleShapeType.TEXT;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.action.replacements.Replacement;
import tc.oc.pgm.action.replacements.ReplacementParser;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.platform.modern.material.ModernBlockData;
import tc.oc.pgm.platform.modern.particle.shapes.CircleShape;
import tc.oc.pgm.platform.modern.particle.shapes.CompositeShape;
import tc.oc.pgm.platform.modern.particle.shapes.CubeShape;
import tc.oc.pgm.platform.modern.particle.shapes.CurveShape;
import tc.oc.pgm.platform.modern.particle.shapes.LineShape;
import tc.oc.pgm.platform.modern.particle.shapes.ParticleShape;
import tc.oc.pgm.platform.modern.particle.shapes.ParticleShapeType;
import tc.oc.pgm.platform.modern.particle.shapes.PlaneShape;
import tc.oc.pgm.platform.modern.particle.shapes.SquareShape;
import tc.oc.pgm.platform.modern.particle.shapes.TextShape;
import tc.oc.pgm.platform.modern.particle.shapes.TriangleShape;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class ParticleModule implements MapModule<ParticleMatchModule> {
  private final ImmutableSet<ParticleDefinition> particleDefinitions;

  public ParticleModule(ImmutableSet<ParticleDefinition> particleDefinitions) {
    this.particleDefinitions = particleDefinitions;
  }

  @Override
  public ParticleMatchModule createMatchModule(Match match) {
    return new ParticleMatchModule(match, particleDefinitions);
  }

  public static class Factory implements MapModuleFactory<ParticleModule> {
    @Override
    public Collection<Class<? extends MapModule<?>>> getSoftDependencies() {
      return ImmutableList.of(FilterModule.class);
    }

    @Override
    public @Nullable ParticleModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Set<ParticleDefinition> particles = new HashSet<>();

      for (Element particleElement :
          XMLUtils.flattenElements(doc.getRootElement(), "particles", "particle")) {
        String id = XMLUtils.getRequiredAttribute(particleElement, "id").getValue();
        Particle type =
            XMLUtils.parseEnum(Node.fromRequiredAttr(particleElement, "type"), Particle.class);
        int amount =
            XMLUtils.parseNumber(Node.fromAttr(particleElement, "amount"), Integer.class, true, 1);
        if (amount == Integer.MAX_VALUE) amount = ParticleDefinition.INFINITE_PARTICLE_AMOUNT;

        boolean needsDust = type.getDataType() == Particle.DustOptions.class
            || type.getDataType() == Particle.DustTransition.class;
        boolean needsEndColor = type.getDataType() == Particle.DustTransition.class;
        Particle.DustOptions dustOptions = null;
        boolean teamColor =
            XMLUtils.parseBoolean(Node.fromAttr(particleElement, "team-color"), false);
        boolean isSpell = type.getDataType() == Particle.Spell.class;
        Particle.Spell power = null;

        if (teamColor && Node.fromAttr(particleElement, "color") != null)
          throw new InvalidXMLException(
              "team-color and color cannot both be defined", particleElement);

        if (needsDust) {
          if (teamColor) {
            float dustSize =
                XMLUtils.parseNumber(Node.fromAttr(particleElement, "dust-size"), Float.class, 1f);
            if (needsEndColor) {
              Color endColor =
                  XMLUtils.parseHexColor(Node.fromRequiredAttr(particleElement, "end-color"));
              dustOptions = new Particle.DustTransition(Color.WHITE, endColor, dustSize);
            } else {
              dustOptions = new Particle.DustOptions(Color.WHITE, dustSize);
            }
          } else {
            Color dustColor =
                XMLUtils.parseHexColor(Node.fromRequiredAttr(particleElement, "color"));
            float dustSize =
                XMLUtils.parseNumber(Node.fromAttr(particleElement, "dust-size"), Float.class, 1f);
            if (needsEndColor) {
              Color endColor =
                  XMLUtils.parseHexColor(Node.fromRequiredAttr(particleElement, "end-color"));
              dustOptions = new Particle.DustTransition(dustColor, endColor, dustSize);
            } else {
              dustOptions = new Particle.DustOptions(dustColor, dustSize);
            }
          }
        }

        boolean needsColor = type.getDataType() == Color.class;
        Color color = null;
        if (teamColor && !needsDust && !needsColor && !isSpell)
          throw new InvalidXMLException(
              "Team-color is not supported for this particle type", particleElement);

        if (needsColor) {
          if (!teamColor) {
            color = XMLUtils.parseHexColor(Node.fromRequiredAttr(particleElement, "color"));
          }
        } else if (!needsDust && !needsColor && !isSpell) {
          if (Node.fromAttr(particleElement, "color") != null) {
            throw new InvalidXMLException(
                "Color is not supported for this particle type", particleElement);
          }
        }

        if (isSpell) {
          float amplifier =
              XMLUtils.parseNumber(Node.fromAttr(particleElement, "amplifier"), Float.class, 1F);

          if (!teamColor && Node.fromAttr(particleElement, "color") != null) {
            Color spellColor = XMLUtils.parseHexColor(Node.fromAttr(particleElement, "color"));
            power = new Particle.Spell(spellColor, amplifier);
          } else if (teamColor && Node.fromAttr(particleElement, "color") == null) {
            power = new Particle.Spell(Color.WHITE, amplifier);
          }
          if (!teamColor && Node.fromAttr(particleElement, "color") == null) {
            throw new InvalidXMLException("Missing color attribute", particleElement);
          }
        }

        boolean isSculkCharge = type == Particle.SCULK_CHARGE;
        Float angle = null;

        if (isSculkCharge) {
          angle = XMLUtils.parseNumber(Node.fromAttr(particleElement, "angle"), Float.class, 0F);
        }
        if (!isSculkCharge && Node.fromAttr(particleElement, "angle") != null) {
          throw new InvalidXMLException(
              "Angle is only supported for particle type SCULK_CHARGE", particleElement);
        }

        boolean isShriek = type == Particle.SHRIEK;
        Integer delay = null;
        if (isShriek) {
          delay = XMLUtils.parseNumber(Node.fromAttr(particleElement, "delay"), Integer.class, 0);
        }
        if (!isShriek && Node.fromAttr(particleElement, "delay") != null) {
          throw new InvalidXMLException(
              "Delay can only be used for particle type SHRIEK", particleElement);
        }

        boolean isDragonBreath = type == Particle.DRAGON_BREATH;
        Float breathPower = null;
        if (isDragonBreath) {
          breathPower =
              XMLUtils.parseNumber(Node.fromAttr(particleElement, "power"), Float.class, 1F);
        }
        if (!isDragonBreath && Node.fromAttr(particleElement, "power") != null) {
          throw new InvalidXMLException(
              "Power can only be used for particle type DRAGON_BREATH", particleElement);
        }

        org.bukkit.block.data.BlockData blockData = null;
        ItemStack itemStack = null;
        boolean needsBlockData = type.getDataType() == BlockData.class;
        boolean needsItemStack = type.getDataType() == ItemStack.class;
        boolean needsMaterial = needsBlockData || needsItemStack;
        if (needsMaterial) {
          Material mat = XMLUtils.parseMaterial(Node.fromAttr(particleElement, "material"));
          if (needsBlockData) {
            BlockMaterialData blockMaterialData = MaterialData.block(mat);
            blockData = ((ModernBlockData) blockMaterialData).getBlock();
          } else {
            itemStack = new ItemStack(mat);
          }
        } else if (Node.fromAttr(particleElement, "material") != null) {
          throw new InvalidXMLException(
              "Material is only supported for block/item particles", particleElement);
        }

        boolean force = XMLUtils.parseBoolean(Node.fromAttr(particleElement, "force"), false);
        Filter viewerFilter =
            factory.getParser().filter(particleElement, "viewer-filter").orNull();

        ParticleShape shape = null;
        Element shapeElement = particleElement.getChild("shape");
        if (shapeElement != null) {
          ParticleShapeType preset = XMLUtils.parseEnum(
              Node.fromAttr(shapeElement, "preset"), ParticleShapeType.class, null);

          if (preset == null) {
            if (Node.fromAttr(shapeElement, "scale") != null)
              throw new InvalidXMLException(
                  "Scale is only supported when using a shape preset", shapeElement);
            if (Node.fromAttr(shapeElement, "yaw") != null)
              throw new InvalidXMLException(
                  "Yaw is only supported when using a shape preset", shapeElement);
            if (Node.fromAttr(shapeElement, "pitch") != null)
              throw new InvalidXMLException(
                  "Pitch is only supported when using a shape preset", shapeElement);

            List<Element> lineElements = shapeElement.getChildren("line");
            List<Element> curveElements = shapeElement.getChildren("curve");
            if (lineElements.isEmpty() && curveElements.isEmpty())
              throw new InvalidXMLException(
                  "Shape must have either a preset or at least one <line> or <curve> defined",
                  shapeElement);

            List<LineShape.Line> lines = new ArrayList<>();
            for (Element lineElement : lineElements) {
              Vector origin = XMLUtils.parseVector(Node.fromRequiredAttr(lineElement, "origin"));
              Vector destination =
                  XMLUtils.parseVector(Node.fromRequiredAttr(lineElement, "destination"));

              lines.add(new LineShape.Line(origin, destination));
            }

            List<CurveShape.Curve> curves = new ArrayList<>();
            for (Element curveElement : curveElements) {
              Vector origin = XMLUtils.parseVector(Node.fromRequiredAttr(curveElement, "origin"));
              Vector controlA =
                  XMLUtils.parseVector(Node.fromRequiredAttr(curveElement, "control-a"));
              Vector destination =
                  XMLUtils.parseVector(Node.fromRequiredAttr(curveElement, "destination"));
              Vector controlB = XMLUtils.parseVector(Node.fromAttr(curveElement, "control-b"));

              curves.add(new CurveShape.Curve(origin, controlA, destination, controlB));
            }

            if (!lines.isEmpty() && !curves.isEmpty()) {
              shape = new CompositeShape(lines, curves);
            } else if (!lines.isEmpty()) {
              shape = new LineShape(lines);
            } else {
              shape = new CurveShape(curves);
            }

          } else {
            float scale =
                XMLUtils.parseNumber(Node.fromAttr(shapeElement, "scale"), Float.class, 1f);
            float yaw = XMLUtils.parseNumber(Node.fromAttr(shapeElement, "yaw"), Float.class, 0f);
            float pitch =
                XMLUtils.parseNumber(Node.fromAttr(shapeElement, "pitch"), Float.class, 0f);

            Element messageElement = shapeElement.getChild("message");
            String messageText = null;
            Map<String, Replacement> replacementMap = null;

            if (messageElement != null) {
              messageText = Node.fromRequiredAttr(messageElement, "text").getValue();
              List<Element> replacementElements =
                  XMLUtils.flattenElements(messageElement, "replacements");
              if (!replacementElements.isEmpty()) {
                ReplacementParser replacementParser = new ReplacementParser(factory);
                ImmutableMap.Builder<String, Replacement> builder = ImmutableMap.builder();
                for (Element replacement : replacementElements) {
                  builder.put(
                      XMLUtils.parseRequiredId(replacement),
                      replacementParser.parse(replacement, null));
                }
                replacementMap = builder.build();
              }
            }

            if (preset == TEXT) {
              if (messageElement == null)
                throw new InvalidXMLException("missing message child element", shapeElement);
            } else if (messageElement != null) {
              throw new InvalidXMLException(
                  "Message can only be used with the TEXT preset", shapeElement);
            }

            shape = switch (preset) {
              case CIRCLE -> new CircleShape(scale, yaw, pitch);
              case SQUARE -> new SquareShape(scale, yaw, pitch);
              case CUBE -> new CubeShape(scale, yaw, pitch);
              case PLANE -> new PlaneShape(scale, yaw, pitch);
              case TRIANGLE -> new TriangleShape(scale, yaw, pitch);
              case TEXT -> new TextShape(messageText, replacementMap);
              default ->
                throw new InvalidXMLException("Unknown shape preset: " + preset, shapeElement);
            };
          }
        }
        ParticleDefinition particleDefinition = new ParticleDefinition(
            id,
            type,
            amount,
            dustOptions,
            color,
            teamColor,
            power,
            angle,
            delay,
            breathPower,
            blockData,
            itemStack,
            viewerFilter,
            force,
            shape);

        factory.getFeatures().addFeature(particleElement, particleDefinition);
        particles.add(particleDefinition);
      }

      return particles.isEmpty() ? null : new ParticleModule(ImmutableSet.copyOf(particles));
    }
  }
}
