package tc.oc.pgm.api.module.exception;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.module.Module;

/** When a {@link Module} or its factory is unable to load. */
public class ModuleLoadException extends RuntimeException {

  private final @Nullable Class<? extends Module> key;

  public ModuleLoadException(Class<? extends Module> key, String message, Throwable cause) {
    super(getFullMessage(message, key), cause);
    this.key = key;
  }

  public @Nullable Class<? extends Module> getModule() {
    return key;
  }

  public static String getFullMessage(String message, Class<? extends Module> key) {
    return message + (key != null ? " @ " + key.getSimpleName() : "");
  }

  public ModuleLoadException(Class<? extends Module> key, String message) {
    this(key, message, null);
  }

  public ModuleLoadException(Class<? extends Module> key, Throwable cause) {
    this(key, cause.getMessage(), cause);
  }

  public ModuleLoadException(String message, Throwable cause) {
    this(null, message, cause);
  }

  public ModuleLoadException(Throwable cause) {
    this(cause.getMessage(), cause);
  }

  public ModuleLoadException(String message) {
    this(message, null);
  }
}
