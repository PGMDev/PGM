package tc.oc.util.reflect;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nullable;

public class ReflectionUtils {
  private ReflectionUtils() {}

  public static void assertPublic(Method method) {
    if (!Modifier.isPublic(method.getModifiers())) {
      error(method, "is not public");
    }
  }

  public static void assertPublic(Constructor method) {
    if (!Modifier.isPublic(method.getModifiers())) {
      error(method, "is not public");
    }
  }

  public static void assertPublicThrows(Method method, Class<?>... exceptions) {
    assertPublic(method);

    for (Class<?> ex : method.getExceptionTypes()) {
      if (!RuntimeException.class.isAssignableFrom(ex)) {
        boolean found = false;
        for (Class<?> allowed : exceptions) {
          if (allowed.isAssignableFrom(ex)) {
            found = true;
            break;
          }
        }
        if (!found) {
          error(method, "throws unhandled exception " + ex.getName());
        }
      }
    }
  }

  public static void assertPublicThrows(Constructor method, Class<?>... exceptions) {
    assertPublic(method);

    for (Class<?> ex : method.getExceptionTypes()) {
      if (!RuntimeException.class.isAssignableFrom(ex)) {
        boolean found = false;
        for (Class<?> allowed : exceptions) {
          if (allowed.isAssignableFrom(ex)) {
            found = true;
            break;
          }
        }
        if (!found) {
          error(method, "throws unhandled exception " + ex.getName());
        }
      }
    }
  }

  private static void error(Method method, String description) {
    throw new IllegalArgumentException(
        method.getDeclaringClass().getName() + "#" + method.getName() + " " + description);
  }

  private static void error(Constructor method, String description) {
    throw new IllegalArgumentException(
        method.getDeclaringClass().getName() + "#" + method.getName() + " " + description);
  }

  public static <T> Class<T> boxType(Class<T> type) {
    if (type.isPrimitive()) {
      switch (type.getName()) {
        case "boolean":
          return (Class<T>) Boolean.class;
        case "char":
          return (Class<T>) Character.class;
        case "byte":
          return (Class<T>) Byte.class;
        case "short":
          return (Class<T>) Short.class;
        case "int":
          return (Class<T>) Integer.class;
        case "long":
          return (Class<T>) Long.class;
        case "float":
          return (Class<T>) Float.class;
        case "double":
          return (Class<T>) Double.class;
        default:
          return type;
      }
    } else {
      return type;
    }
  }

  public static <T> T boxCast(Class<T> type, T obj) {
    return boxType(type).cast(obj);
  }

  public static @Nullable Type superclass(Type type) {
    return type instanceof Class ? ((Class) type).getGenericSuperclass() : null;
  }

  private static final Type[] EMPTY_TYPE_ARRAY = new Type[] {};

  public static Type[] interfaces(Type type) {
    return type instanceof Class ? ((Class) type).getGenericInterfaces() : EMPTY_TYPE_ARRAY;
  }

  public static <T> Iterable<Class<? super T>> parents(Class<T> type) {
    Class<? super T> superclass = type.getSuperclass();
    Class<? super T>[] interfaces = (Class<? super T>[]) type.getInterfaces();
    if (superclass == null) {
      return Arrays.asList(interfaces);
    } else {
      return Iterables.concat(Arrays.asList(interfaces), Collections.singleton(superclass));
    }
  }

  public static <T> Iterable<Class<? super T>> ancestors(@Nullable Class<T> type) {
    if (type == null) return Collections.emptySet();
    return Iterables.concat(
        Collections.singleton(type),
        (Iterable<Class<? super T>>) (Object) Arrays.asList(type.getInterfaces()),
        ancestors(type.getSuperclass()));
  }

  public static String externalToInternal(String name) {
    return name.replace('.', '/');
  }

  public static String internalToExternal(String name) {
    return name.replace('/', '.');
  }

  public static String descriptor(Class<?> type) {
    if (type.isPrimitive()) {
      if (type == byte.class) return "B";
      if (type == char.class) return "C";
      if (type == double.class) return "D";
      if (type == float.class) return "F";
      if (type == int.class) return "I";
      if (type == long.class) return "J";
      if (type == short.class) return "S";
      if (type == boolean.class) return "Z";
      if (type == void.class) return "V";
      throw new RuntimeException("Unrecognized primitive " + type);
    }
    String desc =
        type.isArray()
            ? type.getName() // Array type names already have "L...;"
            : 'L' + type.getName() + ';';
    return externalToInternal(desc);
  }

  public static String descriptor(Field field) {
    return descriptor(field.getType());
  }

  public static String descriptor(Class<?>[] parameterTypes, Class<?> returnType) {
    String desc = "(";
    for (Class param : parameterTypes) {
      desc += descriptor(param);
    }
    return desc + ')' + descriptor(returnType);
  }

  public static String descriptor(Method method) {
    return descriptor(method.getParameterTypes(), method.getReturnType());
  }

  public static String descriptor(Constructor<?> ctor) {
    return descriptor(ctor.getParameterTypes(), void.class);
  }

  public static boolean isStaticInitializer(String methodName) {
    return "<clinit>".equals(methodName);
  }

  public static boolean isConstructor(String methodName) {
    return "<init>".equals(methodName);
  }

  public static @Nullable Constructor<?> getDeclaredConstructor(
      Class<?> parent, String descriptor) {
    for (Constructor<?> ctor : parent.getDeclaredConstructors()) {
      if (descriptor.equals(descriptor(ctor))) return ctor;
    }
    return null;
  }

  public static Constructor<?> needDeclaredConstructor(Class<?> parent, String descriptor)
      throws NoSuchMethodException {
    final Constructor<?> ctor = getDeclaredConstructor(parent, descriptor);
    if (ctor == null) {
      throw new NoSuchMethodException(
          "No constructor with descriptor '" + descriptor + "' in type '" + parent.getName() + "'");
    }
    return ctor;
  }

  public static @Nullable Method getDeclaredMethod(
      Class<?> parent, String name, String descriptor) {
    for (Method method : parent.getDeclaredMethods()) {
      if (name.equals(method.getName()) && descriptor.equals(descriptor(method))) return method;
    }
    return null;
  }

  public static Method needDeclaredMethod(Class<?> parent, String name, String descriptor)
      throws NoSuchMethodException {
    final Method method = getDeclaredMethod(parent, name, descriptor);
    if (method == null) {
      throw new NoSuchMethodException(
          "No method with name '"
              + name
              + "' and descriptor '"
              + descriptor
              + "' in type '"
              + parent.getName()
              + "'");
    }
    return method;
  }

  /**
   * Get the inner class of the given parent with the given name. The name can be fully qualified,
   * or "simple" i.e. just the member name. The numeric names of anonymous classes will also work.
   */
  public static @Nullable Class<?> getDeclaredClass(Class<?> parent, String name) {
    if (name.indexOf('$') == -1) {
      name = parent.getName() + '$' + name;
    }
    for (Class<?> type : parent.getDeclaredClasses()) {
      if (name.equals(type.getName())) return type;
    }
    return null;
  }

  public static Class<?> needDeclaredClass(Class<?> parent, String name)
      throws ClassNotFoundException {
    final Class<?> type = getDeclaredClass(parent, name);
    if (type == null) {
      throw new ClassNotFoundException(
          "No inner class with name '" + name + "' in type '" + parent.getName() + "'");
    }
    return type;
  }

  public static Field needAssignableField(
      boolean privateAccess, Class<?> parent, Class<?> type, String name)
      throws NoSuchFieldException {
    Field field = null;

    if (privateAccess) {
      try {
        field = parent.getDeclaredField(name);
      } catch (NoSuchFieldException ignored) {
      }
    }

    if (field == null) {
      try {
        field = parent.getField(name);
      } catch (NoSuchFieldException ignored) {
      }
    }

    if (field == null) {
      throw new NoSuchFieldException("No field named " + name);
    }

    if (!field.getType().isAssignableFrom(type)) {
      throw new NoSuchFieldException(
          "Field "
              + parent.getName()
              + "."
              + name
              + " with type "
              + field.getType().getName()
              + " is not assignable from "
              + type.getName());
    }

    return field;
  }

  public static <T> T readField(Class<?> parent, @Nullable Object obj, Class<T> type, String name) {
    try {
      return type.cast(readField(obj, parent.getDeclaredField(name)));
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  public static Object readField(@Nullable Object obj, Field field) {
    final boolean wasAccessible = field.isAccessible();
    try {
      field.setAccessible(true);
      return field.get(obj);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } finally {
      field.setAccessible(wasAccessible);
    }
  }

  /** Return a collection of all methods in the given class that have the given annotation */
  public static <T extends Annotation> Collection<Method> getAnnotatedMethods(
      Class<?> klass, final Class<T> annotation) {
    return Collections2.filter(
        Arrays.asList(klass.getMethods()),
        new Predicate<Method>() {
          @Override
          public boolean apply(Method method) {
            return method.getAnnotation(annotation) != null;
          }
        });
  }

  public static @Nullable <T extends Annotation> T getInheritableAnnotation(
      Class<?> cls, Class<T> annotation) {
    for (Class<?> anc : ancestors(cls)) {
      T annot = anc.getAnnotation(annotation);
      if (annot != null) return annot;
    }
    return null;
  }

  public static boolean instanceOfAny(Object o, Class... types) {
    for (Class type : types) {
      if (type.isInstance(o)) return true;
    }
    return false;
  }

  public static boolean instanceOfAll(Object o, Class... types) {
    for (Class type : types) {
      if (!type.isInstance(o)) return false;
    }
    return true;
  }

  public static Predicate<? super Type> assignableFrom(final Type from) {
    return new Predicate<Type>() {
      @Override
      public boolean apply(@Nullable Type to) {
        return to != null && TypeToken.of(to).isAssignableFrom(from);
      }
    };
  }

  public static Predicate<? super Type> assignableTo(final Type to) {
    final TypeToken<?> toToken = TypeToken.of(to);
    return new Predicate<Type>() {
      @Override
      public boolean apply(@Nullable Type from) {
        return from != null && toToken.isAssignableFrom(from);
      }
    };
  }

  public static Predicate<Field> fieldAssignableFrom(final Type from) {
    return new Predicate<Field>() {
      @Override
      public boolean apply(@Nullable Field field) {
        return TypeToken.of(field.getGenericType()).isAssignableFrom(from);
      }
    };
  }

  public static Predicate<Field> fieldAssignableTo(final Type from) {
    final TypeToken<?> fromToken = TypeToken.of(from);
    return new Predicate<Field>() {
      @Override
      public boolean apply(@Nullable Field field) {
        return fromToken.isAssignableFrom(field.getGenericType());
      }
    };
  }

  public static Predicate<? super Member> withModifiers(final int modifiers, final int mask) {
    return new Predicate<Member>() {
      @Override
      public boolean apply(@Nullable Member member) {
        return (member.getModifiers() & mask) == modifiers;
      }
    };
  }

  public static Predicate<? super Member> withAllModifiers(int modifiers) {
    return withModifiers(modifiers, modifiers);
  }

  public static Predicate<? super Member> withoutAnyModifiers(int modifiers) {
    return withModifiers(0, modifiers);
  }

  public static Predicate<? super Member> staticMembers() {
    return withAllModifiers(Modifier.STATIC);
  }

  public static Predicate<? super Member> instanceMembers() {
    return withoutAnyModifiers(Modifier.STATIC);
  }
}
