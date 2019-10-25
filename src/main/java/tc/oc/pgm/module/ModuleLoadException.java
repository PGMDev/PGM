package tc.oc.pgm.module;

import javax.annotation.Nullable;

/** Thrown for map-related problems that cannot be detected until match load time */
public class ModuleLoadException extends Exception {

  private @Nullable ModuleInfo module;

  public ModuleLoadException(@Nullable ModuleInfo module, String message, Throwable cause) {
    super(message, cause);
    this.module = module;
  }

  public ModuleLoadException(@Nullable ModuleInfo module, String message) {
    this(module, message, null);
  }

  public ModuleLoadException(@Nullable ModuleInfo module) {
    this(module, null);
  }

  public ModuleLoadException(String message, Throwable cause) {
    this(null, message, cause);
  }

  public ModuleLoadException(Throwable cause) {
    this(null, cause);
  }

  public ModuleLoadException(String message) {
    this(message, null);
  }

  public ModuleLoadException() {
    this((ModuleInfo) null);
  }

  public void setModule(@Nullable ModuleInfo module) {
    this.module = module;
  }

  public void fillInModule(ModuleInfo module) {
    if (this.module == null) this.module = module;
  }

  public @Nullable ModuleInfo getModule() {
    return module;
  }
}
