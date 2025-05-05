package com.guicedee.guicedservlets.rest.pathing;

import com.guicedee.client.IGuiceContext;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Path;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Scans for classes with Jakarta WS annotations.
 */
public class JakartaWsScanner {

    /**
     * Scans for resource classes with Jakarta WS annotations.
     *
     * @param scanResult The scan result
     * @return A list of resource classes
     */
    public static List<Class<?>> scanForResourceClasses(ScanResult scanResult) {
        List<Class<?>> resourceClasses = new ArrayList<>();

        // Get classes with @ApplicationPath or @Path annotations
        ClassInfoList classInfoList = scanResult.getClassesWithAnnotation(ApplicationPath.class);
        classInfoList.addAll(scanResult.getClassesWithAnnotation(Path.class));

        // Filter out abstract classes, interfaces, inner classes, and static classes
        for (ClassInfo classInfo : classInfoList) {
            if (!classInfo.isAbstract() && !classInfo.isInterface() && !classInfo.isInnerClass() && !classInfo.isStatic()) {
                try {
                    Class<?> resourceClass = classInfo.loadClass(false);
                    resourceClasses.add(resourceClass);
                } catch (Exception e) {
                    // Ignore
                }
            }
        }

        return resourceClasses;
    }

    /**
     * Scans for resource methods in a resource class.
     *
     * @param resourceClass The resource class
     * @return A list of resource methods
     */
    public static List<Method> scanForResourceMethods(Class<?> resourceClass) {
        List<Method> resourceMethods = new ArrayList<>();

        // Get all public methods
        Method[] methods = resourceClass.getMethods();

        // Filter out methods without HTTP method annotations
        for (Method method : methods) {
            if (HttpMethodHandler.hasHttpMethodAnnotation(method)) {
                resourceMethods.add(method);
            }
        }

        return resourceMethods;
    }

    /**
     * Creates a resource instance for a resource class.
     *
     * @param resourceClass The resource class
     * @return The resource instance
     */
    public static Object createResourceInstance(Class<?> resourceClass) {
        // Use GuiceRestInjectionProvider to get an instance of the resource class
        return com.guicedee.guicedservlets.rest.services.GuiceRestInjectionProvider.getInstance(resourceClass);
    }

    /**
     * Gets information about a resource class.
     *
     * @param resourceClass The resource class
     * @return The resource info
     */
    public static ResourceInfo getResourceInfo(Class<?> resourceClass) {
        String basePath = PathHandler.getBasePath(resourceClass);
        List<Method> resourceMethods = scanForResourceMethods(resourceClass);
        Object resourceInstance = createResourceInstance(resourceClass);

        return new ResourceInfo(resourceClass, basePath, resourceMethods, resourceInstance);
    }

    /**
     * Class representing information about a resource.
     */
    public static class ResourceInfo {
        private final Class<?> resourceClass;
        private final String basePath;
        private final List<Method> resourceMethods;
        private final Object resourceInstance;

        public ResourceInfo(Class<?> resourceClass, String basePath, List<Method> resourceMethods, Object resourceInstance) {
            this.resourceClass = resourceClass;
            this.basePath = basePath;
            this.resourceMethods = resourceMethods;
            this.resourceInstance = resourceInstance;
        }

        public Class<?> getResourceClass() {
            return resourceClass;
        }

        public String getBasePath() {
            return basePath;
        }

        public List<Method> getResourceMethods() {
            return resourceMethods;
        }

        public Object getResourceInstance() {
            return resourceInstance;
        }
    }
}
