//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.apache.dubbo.common.utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.model.FrameworkModel;

public class SerializeSecurityManager {
    private final Set<String> allowedPrefix = new LinkedHashSet();
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(SerializeSecurityManager.class);
    private final SerializeClassChecker checker = SerializeClassChecker.getInstance();
    private final Set<AllowClassNotifyListener> listeners = new ConcurrentHashSet();
    private volatile SerializeCheckStatus checkStatus;

    public SerializeSecurityManager(FrameworkModel frameworkModel) {
        this.checkStatus = AllowClassNotifyListener.DEFAULT_STATUS;

        try {
            Set<ClassLoader> classLoaders = frameworkModel.getClassLoaders();
            List<URL> urls = (List)ClassLoaderResourceLoader.loadResources("security/serialize.allowlist", classLoaders).values().stream().flatMap(Collection::stream).collect(Collectors.toList());
            Iterator var4 = urls.iterator();

            while(var4.hasNext()) {
                URL u = (URL)var4.next();

                try {
                    logger.info("Read serialize allow list from " + u);
                    String[] lines = IOUtils.readLines(u.openStream());
                    String[] var7 = lines;
                    int var8 = lines.length;

                    for(int var9 = 0; var9 < var8; ++var9) {
                        String line = var7[var9];
                        line = line.trim();
                        if (!StringUtils.isEmpty(line) && !line.startsWith("#")) {
                            this.allowedPrefix.add(line);
                        }
                    }
                } catch (IOException var11) {
                    logger.error("0-22", "", "", "Failed to load allow class list! Will ignore allow lis from " + u, var11);
                }
            }

            this.checkStatus = SerializeCheckStatus.valueOf(System.getProperty("dubbo.application.serialize-check-status", AllowClassNotifyListener.DEFAULT_STATUS.name()));
            logger.info("Serialize check level: " + this.checkStatus.name());
        } catch (InterruptedException var12) {
            logger.error("99-1", "", "", "Failed to load allow class list! Will ignore allow list from configuration.", var12);
            Thread.currentThread().interrupt();
        }

    }

    public void registerInterface(Class<?> clazz) {
        Set<Class<?>> markedClass = new HashSet();
        markedClass.add(clazz);
        this.addToAllow(clazz.getName());
        Method[] methodsToExport = clazz.getMethods();
        Method[] var4 = methodsToExport;
        int var5 = methodsToExport.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            Method method = var4[var6];
            Class<?>[] parameterTypes = method.getParameterTypes();
            Class[] var9 = parameterTypes;
            int var10 = parameterTypes.length;

            int var11;
            for(var11 = 0; var11 < var10; ++var11) {
                Class<?> parameterType = var9[var11];
            }

            Type[] genericParameterTypes = method.getGenericParameterTypes();
            Type[] var19 = genericParameterTypes;
            var11 = genericParameterTypes.length;

            for(int var22 = 0; var22 < var11; ++var22) {
                Type genericParameterType = var19[var22];
            }

            Class<?> returnType = method.getReturnType();
            Type genericReturnType = method.getGenericReturnType();
            Class<?>[] exceptionTypes = method.getExceptionTypes();
            Class[] var24 = exceptionTypes;
            int var14 = exceptionTypes.length;

            int var15;
            for(var15 = 0; var15 < var14; ++var15) {
                Class<?> exceptionType = var24[var15];
            }

            Type[] genericExceptionTypes = method.getGenericExceptionTypes();
            Type[] var26 = genericExceptionTypes;
            var15 = genericExceptionTypes.length;

            for(int var27 = 0; var27 < var15; ++var27) {
                Type genericExceptionType = var26[var27];
            }
        }

    }

    private void checkType(Set<Class<?>> markedClass, Type type) {
        if (type instanceof Class) {
            this.checkClass(markedClass, (Class)type);
        } else {
            Type[] var4;
            int var5;
            int var6;
            Type bound;
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType)type;
                this.checkClass(markedClass, (Class)parameterizedType.getRawType());
                var4 = parameterizedType.getActualTypeArguments();
                var5 = var4.length;

                for(var6 = 0; var6 < var5; ++var6) {
                    bound = var4[var6];
                    this.checkType(markedClass, bound);
                }
            } else if (type instanceof GenericArrayType) {
                GenericArrayType genericArrayType = (GenericArrayType)type;
                this.checkType(markedClass, genericArrayType.getGenericComponentType());
            } else if (type instanceof TypeVariable) {
                TypeVariable typeVariable = (TypeVariable)type;
                var4 = typeVariable.getBounds();
                var5 = var4.length;

                for(var6 = 0; var6 < var5; ++var6) {
                    bound = var4[var6];
                    this.checkType(markedClass, bound);
                }
            } else if (type instanceof WildcardType) {
                WildcardType wildcardType = (WildcardType)type;
                var4 = wildcardType.getUpperBounds();
                var5 = var4.length;

                for(var6 = 0; var6 < var5; ++var6) {
                    bound = var4[var6];
                    this.checkType(markedClass, bound);
                }

                var4 = wildcardType.getLowerBounds();
                var5 = var4.length;

                for(var6 = 0; var6 < var5; ++var6) {
                    bound = var4[var6];
                    this.checkType(markedClass, bound);
                }
            }
        }

    }

    private void checkClass(Set<Class<?>> markedClass, Class<?> clazz) {
        if (!markedClass.contains(clazz)) {
            markedClass.add(clazz);
            this.addToAllow(clazz.getName());
            Class<?>[] interfaces = clazz.getInterfaces();
            Class[] var4 = interfaces;
            int var5 = interfaces.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                Class<?> interfaceClass = var4[var6];
                this.checkClass(markedClass, interfaceClass);
            }

            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null) {
                this.checkClass(markedClass, superclass);
            }

            Field[] fields = clazz.getDeclaredFields();
            Field[] var13 = fields;
            int var14 = fields.length;

            for(int var8 = 0; var8 < var14; ++var8) {
                Field field = var13[var8];
                if (!Modifier.isTransient(field.getModifiers())) {
                    Class<?> fieldClass = field.getType();
                    this.checkClass(markedClass, fieldClass);
                    this.checkType(markedClass, field.getGenericType());
                }
            }

        }
    }

    protected void addToAllow(String className) {
        if (this.checker.validateClass(className, false)) {
            boolean modified;
            if (!className.startsWith("java.") && !className.startsWith("javax.") && !className.startsWith("com.sun.") && !className.startsWith("sun.") && !className.startsWith("jdk.")) {
                String[] subs = className.split("\\.");
                if (subs.length > 3) {
                    modified = this.allowedPrefix.add(subs[0] + "." + subs[1] + "." + subs[2]);
                } else {
                    modified = this.allowedPrefix.add(className);
                }

                if (modified) {
                    this.notifyListeners();
                }

            } else {
                modified = this.allowedPrefix.add(className);
                if (modified) {
                    this.notifyListeners();
                }

            }
        }
    }

    public void registerListener(AllowClassNotifyListener listener) {
        this.listeners.add(listener);
        listener.notify(this.checkStatus, this.allowedPrefix);
    }

    private void notifyListeners() {
        Iterator var1 = this.listeners.iterator();

        while(var1.hasNext()) {
            AllowClassNotifyListener listener = (AllowClassNotifyListener)var1.next();
            listener.notify(this.checkStatus, this.allowedPrefix);
        }

    }

    protected Set<String> getAllowedPrefix() {
        return this.allowedPrefix;
    }
}
