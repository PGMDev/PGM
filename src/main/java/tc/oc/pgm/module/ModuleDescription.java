package tc.oc.pgm.module;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import tc.oc.pgm.map.MapModule;

/**
 * ModuleDescription is an annotation that must be present on all modules to provide declarative
 * information about the module that is not bound to an instance.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleDescription {
  /** Name of this module. */
  String name();

  /**
   * Try to load all of these modules before this module and throw an exception if any of them fail
   * to load
   */
  Class<? extends MapModule>[] requires() default {};

  /** Silently skip loading this module if any of these modules fail to load */
  Class<? extends MapModule>[] depends() default {};

  /** Try to load all of these modules before this module but ignore if any of them fail to load */
  Class<? extends MapModule>[] follows() default {};
}
