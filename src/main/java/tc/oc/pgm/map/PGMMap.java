package tc.oc.pgm.map;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.Files;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.JDOMParseException;
import org.jdom2.input.SAXBuilder;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.module.ModuleLoadException;
import tc.oc.pgm.module.ModuleRegistry;
import tc.oc.pgm.modules.InfoModule;
import tc.oc.pgm.spawns.SpawnModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.util.SemanticVersion;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.InvalidXMLVersionException;
import tc.oc.xml.Node;

/**
 * PGMMap is persistant through matches and represents an "anchor" that so that map information can
 * be reloaded easily.
 */
public final class PGMMap implements Comparable<PGMMap> {

  protected final PGM pgm;
  protected final ModuleRegistry factory;
  protected final SAXBuilder builder;
  protected final MapFolder folder;
  protected final MapLogger logger;

  protected @Nullable MapModuleContext context;
  private @Nullable MapPersistentContext persistentContext;
  protected File xmlFile;
  protected HashCode xmlFileHash;

  protected boolean pushed;

  public PGMMap(PGM pgm, ModuleRegistry factory, SAXBuilder builder, MapFolder folder) {
    this.pgm = checkNotNull(pgm);
    this.factory = checkNotNull(factory);
    this.builder = checkNotNull(builder);

    this.folder = checkNotNull(folder);

    this.logger = new MapLogger();
  }

  private MapModuleContext load() throws InvalidXMLException, MapNotFoundException {
    final Path descriptionFile = getFolder().getAbsoluteDescriptionFilePath();
    if (!java.nio.file.Files.isRegularFile(descriptionFile)) {
      throw new MapNotFoundException();
    }

    xmlFile = descriptionFile.toFile();
    String path = xmlFile.getPath();
    String sourcePath = getFolder().getSource().getPath().toString();
    if (path.startsWith(sourcePath)) {
      path = path.substring(sourcePath.length());
    }
    Document doc;

    try (HashingInputStream istream =
        new HashingInputStream(Hashing.sha256(), new FileInputStream(xmlFile))) {
      doc = builder.build(istream);
      doc.setBaseURI(path);
      xmlFileHash = istream.hash();

    } catch (FileNotFoundException e1) {
      throw new InvalidXMLException("File not found", path);
    } catch (IOException e1) {
      throw new InvalidXMLException("Error reading file: " + e1.getMessage(), path);
    } catch (JDOMParseException e1) {
      throw InvalidXMLException.fromJDOM(e1, path);
    } catch (JDOMException e1) {
      throw new InvalidXMLException("Unhandled " + e1.getClass().getSimpleName(), path, e1);
    }

    // verify proto
    Node protoNode = Node.fromRequiredAttr(doc.getRootElement(), "proto");
    SemanticVersion protoVersion = XMLUtils.parseSemanticVersion(protoNode);
    if (protoVersion.isNewerThan(PGM.get().getMapProtoSupported())) {
      throw new InvalidXMLVersionException(protoNode, protoVersion);
    }

    File basePath;
    try {
      basePath = getFolder().getAbsolutePath().toFile().getCanonicalFile();
    } catch (IOException e) {
      throw new InvalidXMLException("File system error while resolving map folder", doc);
    }

    MapModuleContext context =
        new MapModuleContext(this.pgm, this.factory, doc, protoVersion, basePath);
    context.load();

    if (!context.hasErrors()) {
      // Don't bother checking these if there are other errors, since those
      // errors will very often prevent these modules from loading.
      if (!context.hasModule(InfoModule.class)) {
        context.addError(
            new ModuleLoadException(new InvalidXMLException("must have info module", doc)));
      }
      if (!context.hasModule(SpawnModule.class)) {
        context.addError(
            new ModuleLoadException(new InvalidXMLException("must have spawn module", doc)));
      }
    }

    return context;
  }

  public boolean shouldReload() {
    if (!Config.AutoReload.enabled()) return false;

    if (xmlFile == null) {
      return Config.AutoReload.reloadWhenError();
    } else {
      try {
        return !Files.hash(xmlFile, Hashing.sha256()).equals(xmlFileHash);
      } catch (IOException e) {
        return true;
      }
    }
  }

  public boolean reload(boolean saveContext) throws MapNotFoundException {
    this.pgm.getMapErrorTracker().clearErrors(this); // TODO: decouple this orthogonal concern

    List<? extends ModuleLoadException> errors;

    try {
      MapModuleContext newContext = this.load();

      if (!newContext.hasErrors()) {
        if (saveContext) {
          this.context = newContext;
        }
        this.persistentContext = newContext.generatePersistentContext();
        this.pushed = false;
        return true;
      }
      errors = newContext.getErrors();
    } catch (InvalidXMLVersionException e) {
      logger.warning("Skipping map with unsupported proto " + e.getVersion());
      errors = ImmutableList.of();
    } catch (InvalidXMLException e) {
      errors = ImmutableList.of(new ModuleLoadException(e));
    }

    for (ModuleLoadException error : errors) {
      logger.log(new MapLogRecord(error));
    }

    return false;
  }

  public void deleteContext() {
    this.context = null;
  }

  public boolean isPushed() {
    return pushed;
  }

  public void setPushed(boolean pushed) {
    this.pushed = pushed;
  }

  public boolean push() {
    if (pushed) {
      return false;
    } else {
      forcePush();
      return true;
    }
  }

  public void forcePush() {
    pushed = true;
  }

  public ModuleRegistry getFactoryContext() {
    return factory;
  }

  public Optional<MapModuleContext> getContext() {
    return Optional.ofNullable(context);
  }

  public MapPersistentContext getPersistentContext() {
    if (persistentContext == null) {
      throw new IllegalStateException("Map is not loaded: " + this);
    }
    return persistentContext;
  }

  public MapInfo getInfo() {
    return getPersistentContext().getInfo();
  }

  public MapFolder getFolder() {
    return folder;
  }

  public MapSource getSource() {
    return getFolder().getSource();
  }

  public String getDottedPath() {
    return Joiner.on(".").join(getFolder().getRelativePath());
  }

  public String getName() {
    MapInfo info = this.getInfo();
    if (info != null) {
      return info.name;
    } else {
      return getFolder().getRelativePath().toString();
    }
  }

  /**
   * NOTE: not consistent with equals(), which is allowed but discouraged. This should be in a
   * seperate comparator.
   */
  @Override
  public int compareTo(PGMMap other) {
    return this.getInfo().name.compareTo(other.getInfo().name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PGMMap)) return false;
    PGMMap map = (PGMMap) o;
    if (!folder.equals(map.folder)) return false;
    return true;
  }

  @Override
  public String toString() {
    return getName();
  }

  @Override
  public int hashCode() {
    return folder.hashCode();
  }

  public MapLogger getLogger() {
    return logger;
  }

  public class MapLogger extends Logger {
    protected MapLogger() {
      super(pgm.getMapLogger().getName() + "." + getDottedPath(), null);
      setParent(pgm.getMapLogger());
      setUseParentHandlers(true);
    }

    @Override
    public void log(LogRecord record) {
      if (record instanceof MapLogRecord) {
        super.log(record);
      } else {
        super.log(new MapLogRecord(record));
      }
    }
  }

  public class MapLogRecord extends LogRecord {

    protected final String location;

    protected MapLogRecord(
        Level level,
        @Nullable String location,
        @Nullable String message,
        @Nullable Throwable thrown) {
      super(level, message != null ? message : thrown != null ? thrown.getMessage() : null);

      if (thrown != null && thrown.getCause() instanceof InvalidXMLException) {
        thrown = thrown.getCause();
        message = thrown.getMessage();
      }

      if (location == null) {
        if (thrown instanceof InvalidXMLException) {
          location = ((InvalidXMLException) thrown).getFullLocation();
        }

        if (location == null) {
          location = PGMMap.this.getFolder().getRelativeDescriptionFilePath().toString();
        }
      }

      this.location = location;

      if (thrown != null) setThrown(thrown);
      if (message != null) setMessage(message);
      setLoggerName(PGMMap.this.getLogger().getName());
    }

    protected MapLogRecord(ModuleLoadException thrown) {
      this(Level.SEVERE, null, null, thrown);
    }

    protected MapLogRecord(LogRecord record) {
      this(record.getLevel(), null, record.getMessage(), record.getThrown());
    }

    public String getFullMessage() {
      return getLocation() + ": " + getMessage();
    }

    public String getLegacyFormattedMessage() {
      String message = ChatColor.AQUA + getLocation() + ": " + ChatColor.RED + getMessage();

      Throwable thrown = getThrown();
      if (thrown != null && thrown.getCause() != null) {
        message += ", caused by: " + thrown.getCause().getMessage();
      }

      return message;
    }

    public PGMMap getMap() {
      return PGMMap.this;
    }

    public String getLocation() {
      return location;
    }
  }
}
