package tc.oc.pgm.platform.modern.particle;

import static tc.oc.pgm.platform.modern.particle.shapes.ParticleShapeType.TEXT;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import tc.oc.pgm.platform.modern.particle.shapes.SphereShape;
import tc.oc.pgm.platform.modern.particle.shapes.SpiralShape;
import tc.oc.pgm.platform.modern.particle.shapes.SquareShape;
import tc.oc.pgm.platform.modern.particle.shapes.StarShape;
import tc.oc.pgm.platform.modern.particle.shapes.TextShape;
import tc.oc.pgm.platform.modern.particle.shapes.TriangleShape;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class ParticleModule implements MapModule<ParticleMatchModule> {

  @Override
  public ParticleMatchModule createMatchModule(Match match) {
    return new ParticleMatchModule();
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
        else amount = Math.min(amount, ParticleDefinition.INFINITE_PARTICLE_AMOUNT);

        boolean needsDust = type.getDataType() == Particle.DustOptions.class
            || type.getDataType() == Particle.DustTransition.class;
        boolean needsEndColor = type.getDataType() == Particle.DustTransition.class;
        Particle.DustOptions dustOptions = null;
        boolean teamColor =
            XMLUtils.parseBoolean(Node.fromAttr(particleElement, "team-color"), false);
        boolean needsColor = type.getDataType() == Color.class;
        Color color = null;
        boolean isSpell = type.getDataType() == Particle.Spell.class;
        Particle.Spell power = null;

        if (teamColor) {
          if (Node.fromAttr(particleElement, "color") != null)
            throw new InvalidXMLException(
                "Cannot combine 'team-color' and 'color'", particleElement);
          if (!needsDust && !needsColor && !isSpell)
            throw new InvalidXMLException(
                "Attribute 'team-color' is not supported for particle type " + type.name(),
                particleElement);
        }

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
        } else if (Node.fromAttr(particleElement, "end-color") != null) {
          throw new InvalidXMLException(
              "Attribute 'end-color' is only supported for particle type DUST_COLOR_TRANSITION",
              particleElement);
        } else if (Node.fromAttr(particleElement, "dust-size") != null) {
          throw new InvalidXMLException(
              "Attribute 'dust-size' is only supported for dust particles", particleElement);
        }

        if (needsColor) {
          if (!teamColor) {
            color = XMLUtils.parseHexColor(Node.fromRequiredAttr(particleElement, "color"));
          }
        } else if (!needsDust && !isSpell) {
          if (Node.fromAttr(particleElement, "color") != null) {
            throw new InvalidXMLException(
                "Attribute 'color' is not supported for particle type " + type.name(),
                particleElement);
          }
        }

        if (isSpell) {
          float amplifier =
              XMLUtils.parseNumber(Node.fromAttr(particleElement, "amplifier"), Float.class, 1F);

          Node colorNode = Node.fromAttr(particleElement, "color");
          if (!teamColor && colorNode != null) {
            Color spellColor = null;
            try {
              ParticleEffectColor effectColor =
                  XMLUtils.parseEnum(colorNode, ParticleEffectColor.class, null);
              if (effectColor != null) spellColor = effectColor.toColor();
            } catch (InvalidXMLException ignored) {
            }
            if (spellColor == null) spellColor = XMLUtils.parseHexColor(colorNode);
            power = new Particle.Spell(spellColor, amplifier);
          } else if (teamColor && colorNode == null) {
            power = new Particle.Spell(Color.WHITE, amplifier);
          }
          if (!teamColor && colorNode == null) {
            throw new InvalidXMLException(
                "Particle type " + type.name() + " requires a 'color' attribute", particleElement);
          }
        } else if (Node.fromAttr(particleElement, "amplifier") != null) {
          throw new InvalidXMLException(
              "Attribute 'amplifier' is not supported for particle type " + type.name(),
              particleElement);
        }

        boolean isSculkCharge = type == Particle.SCULK_CHARGE;
        Float angle = null;
        if (isSculkCharge) {
          angle = XMLUtils.parseNumber(Node.fromAttr(particleElement, "angle"), Float.class, 0F);
        } else if (Node.fromAttr(particleElement, "angle") != null) {
          throw new InvalidXMLException(
              "Attribute 'angle' is only supported for SCULK_CHARGE", particleElement);
        }

        boolean isShriek = type == Particle.SHRIEK;
        Integer delay = null;
        if (isShriek) {
          delay = XMLUtils.parseNumber(Node.fromAttr(particleElement, "delay"), Integer.class, 0);
        } else if (Node.fromAttr(particleElement, "delay") != null) {
          throw new InvalidXMLException(
              "Attribute 'delay' is only supported for particle type SHRIEK", particleElement);
        }

        boolean isDragonBreath = type == Particle.DRAGON_BREATH;
        Float breathPower = null;
        if (isDragonBreath) {
          breathPower =
              XMLUtils.parseNumber(Node.fromAttr(particleElement, "power"), Float.class, 1F);
        } else if (Node.fromAttr(particleElement, "power") != null) {
          throw new InvalidXMLException(
              "Attribute 'power' is only supported for particle type DRAGON_BREATH",
              particleElement);
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
              "Attribute 'material' is only supported for block and item particle types",
              particleElement);
        }

        boolean force = XMLUtils.parseBoolean(Node.fromAttr(particleElement, "force"), false);
        Filter viewerFilter =
            factory.getParser().filter(particleElement, "viewer-filter").orNull();

        ParticleShape shape = null;
        Element shapeElement = particleElement.getChild("shape");
        if (shapeElement != null) {
          ParticleShapeType preset;
          try {
            preset = XMLUtils.parseEnum(
                Node.fromAttr(shapeElement, "preset"), ParticleShapeType.class, null);
          } catch (InvalidXMLException e) {
            Node presetNode = Node.fromAttr(shapeElement, "preset");
            throw new InvalidXMLException(
                "Invalid shape preset '" + (presetNode != null ? presetNode.getValue() : "") + "'",
                shapeElement);
          }

          if (preset == null) {
            if (Node.fromAttr(shapeElement, "scale") != null)
              throw new InvalidXMLException(
                  "Attribute 'scale' is only supported for shape presets", shapeElement);
            if (Node.fromAttr(shapeElement, "yaw") != null)
              throw new InvalidXMLException(
                  "Attribute 'yaw' is only supported for shape presets", shapeElement);
            if (Node.fromAttr(shapeElement, "pitch") != null)
              throw new InvalidXMLException(
                  "Attribute 'pitch' is only supported for shape presets", shapeElement);

            List<Element> lineElements = shapeElement.getChildren("line");
            List<Element> curveElements = shapeElement.getChildren("curve");
            List<Element> spiralElements = shapeElement.getChildren("spiral");
            if (lineElements.isEmpty() && curveElements.isEmpty() && spiralElements.isEmpty())
              throw new InvalidXMLException(
                  "<shape> must have either a preset or at least one <line>, <curve>, or <spiral> defined",
                  shapeElement);

            if (!spiralElements.isEmpty() && (!lineElements.isEmpty() || !curveElements.isEmpty()))
              throw new InvalidXMLException(
                  "<spiral> cannot be combined with <line> or <curve>", shapeElement);

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

            List<SpiralShape.Spiral> spirals = new ArrayList<>();
            for (Element spiralElement : spiralElements) {
              int turns = XMLUtils.parseNumber(
                  Node.fromRequiredAttr(spiralElement, "turns"), Integer.class);
              float radius =
                  XMLUtils.parseNumber(Node.fromRequiredAttr(spiralElement, "radius"), Float.class);
              spirals.add(new SpiralShape.Spiral(turns, radius));
            }

            if (!spiralElements.isEmpty()) {
              shape = new SpiralShape(spirals);
            } else if (!lines.isEmpty() && !curves.isEmpty()) {
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
                throw new InvalidXMLException(
                    "TEXT preset requires a <message> child element", shapeElement);
            } else if (messageElement != null) {
              throw new InvalidXMLException(
                  "<message> is only supported for the TEXT shape preset", shapeElement);
            }

            shape = switch (preset) {
              case CIRCLE -> new CircleShape(scale, yaw, pitch);
              case SPHERE -> new SphereShape(scale, yaw, pitch);
              case SQUARE -> new SquareShape(scale, yaw, pitch);
              case CUBE -> new CubeShape(scale, yaw, pitch);
              case PLANE -> new PlaneShape(scale, yaw, pitch);
              case TRIANGLE -> new TriangleShape(scale, yaw, pitch);
              case TEXT -> new TextShape(messageText, replacementMap);
              case STAR -> new StarShape(scale, yaw, pitch);
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

      return particles.isEmpty() ? null : new ParticleModule();
    }
  }
}
