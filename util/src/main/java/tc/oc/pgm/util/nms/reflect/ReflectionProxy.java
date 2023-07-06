package tc.oc.pgm.util.nms.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.reflect.MinecraftReflectionUtils;
import tc.oc.pgm.util.reflect.ReflectionUtils;

public class ReflectionProxy implements InvocationHandler {

  static Map<Method, Function<Object[], Object>> functionMap = new ConcurrentHashMap<>();

  static ReflectionProxy INSTANCE = new ReflectionProxy();

  public static <T> T getProxy(Class<T> classType) {
    return (T)
        Proxy.newProxyInstance(
            ReflectionProxy.class.getClassLoader(), new Class[] {classType}, INSTANCE);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    return functionMap
        .computeIfAbsent(
            method,
            (input) -> {
              Class<?> declaringClass = method.getDeclaringClass();
              Class<?> parentClass = getAnnotatedClass(declaringClass);
              if (parentClass == null) {
                return processComponent(method, args);
              } else {
                return processComponent(method, args, parentClass);
              }
            })
        .apply(args);
  }

  @Nullable
  private static Function<Object[], Object> processComponent(Method method, Object[] args) {
    if (method.getDeclaredAnnotationsByType(Reflect.NMS.class).length > 0) {
      return processNMSComponent(method, args);
    } else if (method.getDeclaredAnnotationsByType(Reflect.CB.class).length > 0) {
      return processCBComponent(method, args);
    } else if (method.getDeclaredAnnotationsByType(Reflect.B.class).length > 0) {
      return processBukkitComponent(method, args);
    }
    throw new RuntimeException("Error processing Method: " + method);
  }

  @Nullable
  private static Function<Object[], Object> processComponent(
      Method method, Object[] args, Class<?> parentClass) {
    if (method.getDeclaredAnnotationsByType(Reflect.StaticMethod.class).length > 0) {
      return processStaticMethod(method, parentClass);
    } else if (method.getDeclaredAnnotationsByType(Reflect.Method.class).length > 0) {
      return processMethod(method, args, parentClass);
    } else if (method.getDeclaredAnnotationsByType(Reflect.Field.class).length > 0) {
      return processField(method, args, parentClass);
    } else if (method.getDeclaredAnnotationsByType(Reflect.Constructor.class).length > 0) {
      return processConstructor(method, parentClass);
    }
    throw new RuntimeException("Error processing method: " + method);
  }

  private static Function<Object[], Object> processStaticMethod(
      Method method, Class<?> parentClass) {
    for (Reflect.StaticMethod staticMethod :
        method.getDeclaredAnnotationsByType(Reflect.StaticMethod.class)) {
      try {
        Method reflectedMethod =
            parentClass.getMethod(
                staticMethod.value(), processParameters(staticMethod.parameters()));
        return (input) -> ReflectionUtils.callMethod(reflectedMethod, null, input);
      } catch (NoSuchMethodException e) {
      }
    }
    throw new RuntimeException("Method not found for " + method);
  }

  private static Function<Object[], Object> processMethod(
      Method method, Object[] args, Class<?> parentClass) {
    for (Reflect.Method reflectMethod : method.getDeclaredAnnotationsByType(Reflect.Method.class)) {
      try {
        Method reflectedMethod =
            parentClass.getMethod(
                reflectMethod.value(), processParameters(reflectMethod.parameters()));
        return callMethod(args, reflectedMethod);
      } catch (NoSuchMethodException ignored) {
      }
    }
    throw new RuntimeException("Method not found for " + method);
  }

  @Nullable
  private static Function<Object[], Object> processField(
      Method method, Object[] args, Class<?> parentClass) {
    for (Reflect.Field field : method.getDeclaredAnnotationsByType(Reflect.Field.class)) {
      try {
        Field reflectedField = parentClass.getDeclaredField(field.value());
        reflectedField.setAccessible(true);
        return performActionOnField(args, reflectedField);
      } catch (NoSuchFieldException ignored) {
      }
    }
    throw new RuntimeException("Field not found for " + method);
  }

  @NotNull
  private static Function<Object[], Object> processConstructor(
      Method method, Class<?> parentClass) {
    for (Reflect.Constructor constructor :
        method.getDeclaredAnnotationsByType(Reflect.Constructor.class)) {
      try {
        Constructor<?> reflectedConstructor =
            parentClass.getConstructor(processParameters(constructor.value()));
        return (input) -> ReflectionUtils.callConstructor(reflectedConstructor, input);
      } catch (NoSuchMethodException ignored) {
      }
    }
    throw new RuntimeException("Constructor not found for " + method);
  }

  @Nullable
  private static Function<Object[], Object> processBukkitComponent(Method method, Object[] args) {
    for (Reflect.B bukkit : method.getDeclaredAnnotationsByType(Reflect.B.class)) {
      String methodPath = bukkit.value();
      Class<?>[] parameters = bukkit.parameters();

      String[] split = splitClassAndMethod(methodPath);

      String className = split[0];
      String methodName = split[1];

      Class<?> parentClass;
      try {
        parentClass = MinecraftReflectionUtils.getBukkitClass(className);
      } catch (RuntimeException ignored) {
        continue;
      }

      if (methodName.endsWith("()")) {
        Method reflectedMethod = parseStandaloneMethod(parentClass, methodName, parameters);
        if (reflectedMethod != null) {
          return callMethod(args, reflectedMethod);
        }
      } else {
        Field reflectedField = parseStandaloneField(parentClass, methodName);
        if (reflectedField != null) {
          return performActionOnField(args, reflectedField);
        }
      }
    }
    throw new RuntimeException("Error processing Method: " + method);
  }

  @Nullable
  private static Function<Object[], Object> processCBComponent(Method method, Object[] args) {
    for (Reflect.CB cb : method.getDeclaredAnnotationsByType(Reflect.CB.class)) {
      String methodPath = cb.value();
      Class<?>[] parameters = cb.parameters();

      String[] split = splitClassAndMethod(methodPath);

      String className = split[0];
      String methodName = split[1];

      Class<?> parentClass;
      try {
        parentClass = MinecraftReflectionUtils.getCraftBukkitClass(className);
      } catch (RuntimeException ignored) {
        continue;
      }

      if (methodName.endsWith("()")) {
        Method reflectedMethod = parseStandaloneMethod(parentClass, methodName, parameters);
        if (reflectedMethod != null) {
          return callMethod(args, reflectedMethod);
        }
      } else {
        Field reflectedField = parseStandaloneField(parentClass, methodName);
        if (reflectedField != null) {
          return performActionOnField(args, reflectedField);
        }
      }
    }
    throw new RuntimeException("Error processing Method: " + method);
  }

  @Nullable
  private static Function<Object[], Object> processNMSComponent(Method method, Object[] args) {
    for (Reflect.NMS nms : method.getDeclaredAnnotationsByType(Reflect.NMS.class)) {
      String methodPath = nms.value();
      Class<?>[] parameters = nms.parameters();

      String[] split = splitClassAndMethod(methodPath);

      String className = split[0];
      String methodName = split[1];

      Class<?> parentClass;
      parentClass = findNMSClass(className);
      if (parentClass == null) continue;

      if (methodName.endsWith("()")) {
        Method reflectedMethod = parseStandaloneMethod(parentClass, methodName, parameters);
        if (reflectedMethod != null) {
          return callMethod(args, reflectedMethod);
        }
      } else {
        Field reflectedField = parseStandaloneField(parentClass, methodName);
        if (reflectedField != null) {
          return performActionOnField(args, reflectedField);
        }
      }
    }
    throw new RuntimeException("Error processing Method: " + method);
  }

  @Nullable
  private static Function<Object[], Object> performActionOnField(
      Object[] args, Field reflectedField) {
    if (args.length == 0) {
      throw new UnsupportedOperationException("Annotated fields must have more than 0 parameters!");
    } else if (args.length == 1) {
      return (input) -> ReflectionUtils.readField(input[0], reflectedField);
    } else if (args.length == 2) {
      return (input) -> {
        ReflectionUtils.setField(input[0], input[1], reflectedField);
        return null;
      };
    } else {
      throw new UnsupportedOperationException("Annotated fields must have less than 3 parameters!");
    }
  }

  private static Function<Object[], Object> callMethod(Object[] args, Method reflectedMethod) {
    if (args.length > 1) {
      return (input) ->
          ReflectionUtils.callMethod(
              reflectedMethod, input[0], Arrays.copyOfRange(input, 1, input.length));
    } else {
      return (input) -> ReflectionUtils.callMethod(reflectedMethod, input[0]);
    }
  }

  @Nullable
  private static Field parseStandaloneField(Class<?> parentClass, String methodName) {
    Field reflectedField;
    try {
      reflectedField = parentClass.getDeclaredField(methodName);
      reflectedField.setAccessible(true);
    } catch (NoSuchFieldException e) {
      return null;
    }
    return reflectedField;
  }

  @Nullable
  private static Method parseStandaloneMethod(
      Class<?> parentClass, String methodName, Class<?>[] parameters) {
    Method reflectedMethod;
    methodName = methodName.substring(0, methodName.length() - 2);
    try {
      if (parameters.length > 0) {
        reflectedMethod = parentClass.getMethod(methodName, processParameters(parameters));
      } else {
        reflectedMethod = parentClass.getMethod(methodName);
      }
      reflectedMethod.setAccessible(true);
    } catch (NoSuchMethodException e) {
      return null;
    }
    return reflectedMethod;
  }

  private static Class<?>[] processParameters(Class<?>[] givenParameters) {
    Class<?>[] parameters = new Class<?>[givenParameters.length];

    for (int i = 0; i < givenParameters.length; i++) {
      try {
        Class<?> annotatedClass = getAnnotatedClass(givenParameters[i]);
        if (annotatedClass != null) {
          parameters[i] = annotatedClass;
          break;
        }
      } catch (RuntimeException ignored) {
      }
      parameters[i] = givenParameters[i];
    }

    return parameters;
  }

  @NotNull
  private static String[] splitClassAndMethod(String fieldMethodPath) {
    String[] split = new String[2];
    int index = fieldMethodPath.lastIndexOf(".");
    split[0] = fieldMethodPath.substring(0, index);
    split[1] = fieldMethodPath.substring(index + 1);
    return split;
  }

  private static Class<?> getAnnotatedClass(Class<?> declaringClass) {
    if (declaringClass.isAnnotationPresent(Reflect.NMS.class)) {
      for (Reflect.NMS nms : declaringClass.getDeclaredAnnotationsByType(Reflect.NMS.class)) {
        Class<?> parentClass = findNMSClass(nms.value());
        if (parentClass != null) {
          return parentClass;
        }
      }
      throw new RuntimeException("Class not found for " + declaringClass);
    } else if (declaringClass.isAnnotationPresent(Reflect.CB.class)) {
      for (Reflect.CB cb : declaringClass.getDeclaredAnnotationsByType(Reflect.CB.class)) {
        try {
          return MinecraftReflectionUtils.getCraftBukkitClass(cb.value());
        } catch (RuntimeException ignored) {
        }
      }
      throw new RuntimeException("Class not found for " + declaringClass);
    } else if (declaringClass.isAnnotationPresent(Reflect.B.class)) {
      for (Reflect.B bukkit : declaringClass.getDeclaredAnnotationsByType(Reflect.B.class)) {
        try {
          return MinecraftReflectionUtils.getBukkitClass(bukkit.value());
        } catch (RuntimeException ignored) {
        }
      }
      throw new RuntimeException("Class not found for " + declaringClass);
    }
    return null;
  }

  private static Class<?> findNMSClass(String className) {
    Class<?> parentClass = null;
    try {
      parentClass = MinecraftReflectionUtils.getNMSClassLegacy(className);
    } catch (RuntimeException ignored) {
    }
    try {
      parentClass = MinecraftReflectionUtils.getNMSClassNew(className);
    } catch (RuntimeException ignored) {
    }
    return parentClass;
  }
}
