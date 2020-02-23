/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tc.oc.util.reflect;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

/**
 * Borrowed from Apache Commons. Only change was making it public :( Don't add any code to this.
 *
 * <p>Contains common code for working with Methods/Constructors, extracted and refactored from
 * <code>MethodUtils</code> when it was imported from Commons BeanUtils.
 *
 * @author Apache Software Foundation
 * @author Steve Cohen
 * @author Matt Benson
 * @since 2.5
 * @version $Id: MemberUtils.java 1057013 2011-01-09 20:04:16Z niallp $
 */
public abstract class MemberUtils {
  // TODO extract an interface to implement compareParameterSets(...)?

  private static final int ACCESS_TEST = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE;

  /** Array of primitive number types ordered by "promotability" */
  private static final Class[] ORDERED_PRIMITIVE_TYPES = {
    Byte.TYPE, Short.TYPE, Character.TYPE, Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE
  };

  /**
   * XXX Default access superclass workaround
   *
   * <p>When a public class has a default access superclass with public members, these members are
   * accessible. Calling them from compiled code works fine. Unfortunately, on some JVMs, using
   * reflection to invoke these members seems to (wrongly) to prevent access even when the modifer
   * is public. Calling setAccessible(true) solves the problem but will only work from sufficiently
   * privileged code. Better workarounds would be gratefully accepted.
   *
   * @param o the AccessibleObject to set as accessible
   */
  static void setAccessibleWorkaround(AccessibleObject o) {
    if (o == null || o.isAccessible()) {
      return;
    }
    Member m = (Member) o;
    if (Modifier.isPublic(m.getModifiers())
        && isPackageAccess(m.getDeclaringClass().getModifiers())) {
      try {
        o.setAccessible(true);
      } catch (SecurityException e) {
        // ignore in favor of subsequent IllegalAccessException
      }
    }
  }

  /**
   * Learn whether a given set of modifiers implies package access.
   *
   * @param modifiers to test
   * @return true unless package/protected/private modifier detected
   */
  public static boolean isPackageAccess(int modifiers) {
    return (modifiers & ACCESS_TEST) == 0;
  }
}
