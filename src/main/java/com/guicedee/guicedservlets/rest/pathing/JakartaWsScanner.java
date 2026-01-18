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
 * Scans the classpath for Jakarta REST resources.
 *
 * <p>This helper uses ClassGraph results produced by the Guice context to
 * locate resource classes and HTTP methods annotated with Jakarta REST
 * annotations.</p>
 */
public class JakartaWsScanner {

    /**
     * Finds resource classes annotated with {@link ApplicationPath} or {@link Path}.
     *
     * @param scanResult The ClassGraph scan result
     * @return A list of concrete, non-inner, non-static resource classes
     */
    public static List<Class<?>> scanForResourceClasses(ScanResult scanResult) {
        List<Class<?>> resourceClasses = new ArrayList<>();

        // Get classes with @ApplicationPath or @Path annotations
        // ClassInfoList is immutable, so we need to create a new list and add all elements
        ClassInfoList applicationPathClasses = scanResult.getClassesWithAnnotation(ApplicationPath.class);
        ClassInfoList pathClasses = scanResult.getClassesWithAnnotation(Path.class);

        // Create a combined list of ClassInfo objects
        List<ClassInfo> combinedList = new ArrayList<>();
        applicationPathClasses.forEach(combinedList::add);
        pathClasses.forEach(combinedList::add);

        // Filter out abstract classes, interfaces, inner classes, and static classes
        for (ClassInfo classInfo : combinedList) {
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
     * Scans a resource class for methods that declare HTTP method annotations.
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
     * Creates a resource instance via the Guice context.
     *
     * @param resourceClass The resource class
     * @return The resource instance
     */
    public static Object createResourceInstance(Class<?> resourceClass) {
        // Use GuiceRestInjectionProvider to get an instance of the resource class
        return com.guicedee.guicedservlets.rest.services.GuiceRestInjectionProvider.getInstance(resourceClass);
    }

    /**
     * Creates a {@link ResourceInfo} descriptor for a resource class.
     *
     * @param resourceClass The resource class
     * @return The resource information used for registration and invocation
     */
    public static ResourceInfo getResourceInfo(Class<?> resourceClass) {
        String basePath = PathHandler.getBasePath(resourceClass);
        List<Method> resourceMethods = scanForResourceMethods(resourceClass);
        Object resourceInstance = createResourceInstance(resourceClass);

        return new ResourceInfo(resourceClass, basePath, resourceMethods, resourceInstance);
    }

    /**
     * Resource metadata describing class-level paths, methods, and instance.
     */
    public static class ResourceInfo {
        private final Class<?> resourceClass;
        private final String basePath;
        private final List<Method> resourceMethods;
        private final Object resourceInstance;

        /**
         * Creates a new resource info object.
         *
         * @param resourceClass The resource class
         * @param basePath The base path for the resource class
         * @param resourceMethods The methods annotated with HTTP verbs
         * @param resourceInstance The instantiated resource object
         */
        public ResourceInfo(Class<?> resourceClass, String basePath, List<Method> resourceMethods, Object resourceInstance) {
            this.resourceClass = resourceClass;
            this.basePath = basePath;
            this.resourceMethods = resourceMethods;
            this.resourceInstance = resourceInstance;
        }

        /**
         * @return The resource class
         */
        public Class<?> getResourceClass() {
            return resourceClass;
        }

        /**
         * @return The base path for the resource class
         */
        public String getBasePath() {
            return basePath;
        }

        /**
         * @return The resource methods discovered on the class
         */
        public List<Method> getResourceMethods() {
            return resourceMethods;
        }

        /**
         * @return The instantiated resource object
         */
        public Object getResourceInstance() {
            return resourceInstance;
        }
    }
}
