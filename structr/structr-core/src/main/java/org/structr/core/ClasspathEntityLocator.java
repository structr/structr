/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.entity.StructrNode;

/**
 * An entity locator that scans every WAR, JAR and CLASS file in java.class.path and
 * java.library.path for an entity that matches a given type.
 *
 * @author cmorgner
 */
public class ClasspathEntityLocator {

    private static final Logger logger = Logger.getLogger(ClasspathEntityLocator.class.getName());
    private static Map<Class, Set<Class>> entityCache = new ConcurrentHashMap<Class, Set<Class>>();
    private static Set<String> cachedClassNameCollection = null;
    private static Set<Class> cachedClassCollection = null;
    private static Set<String> entityLocations = null;

    private ClasspathEntityLocator() {
    }

    public static void setLocations(Set<String> locations) {
        entityLocations = locations;
    }

    /**
     * Call this method in the initialization phase of your application.
     */
    public static void prefetchCoreEntities() {
        logger.log(Level.FINE, "Prefetching core entities");

        Class[] prefetchEntities = {
            StructrNode.class,
            Command.class,
            Service.class
        };

        for (int i = 0; i < prefetchEntities.length; i++) {
            locateEntitiesByType(prefetchEntities[i]);
        }
    }

    /**
     * Returns a Set of Class objects that match the class or interface given by
     * <code>type</code>.
     *
     * @param type the class or interface to search for
     * @return a Set of Class objects matching <code>type</code>
     */
    public static Set<Class> locateEntitiesByType(Class type) {
        return (locateEntitiesByType(type, false));
    }

    /**
     * Returns a Set of Class objects that match the class or interface given by
     * <code>type</code>.
     *
     * @param type the class or interface to search for
     * @param forceReload whether to re-scan class and library path before searching
     * @return a Set of Class objects matching <code>type</code>
     * @return
     */
    public static Set<Class> locateEntitiesByType(Class type, boolean forceReload) {
        logger.log(Level.FINE, "Locating entites for type ", type.getName());

        initializeCachedClassCollection(forceReload);

        Set<Class> ret = entityCache.get(type);

        if (ret == null) {
            ret = new LinkedHashSet<Class>();

            // try initialization
            for (Class classCandidate : cachedClassCollection) {

                try {
                    if (classCandidate != null && !classCandidate.isInterface()) {
                        ret.add(classCandidate.asSubclass(type));
                        logger.log(Level.FINER, "Found valid entity for type ", type.getName());
                    }

                } catch (Throwable t) {
                    // we don't care about the type of exception
                    classCandidate = null;
                }
            }

            // cache results..
            entityCache.put(type, ret);
        }

        return (ret);
    }

    // <editor-fold defaultstate="collapsed" desc="private methods">
    /**
     * Scans java.class.path and java.library.path for JAR and CLASS files, storing
     * the results in a Set for later use.
     *
     * @param reload whether to discard a previously created collection or not
     */
    private static synchronized void initializeCachedClassNameCollection(boolean reload) {
        if (cachedClassNameCollection == null || reload) {
            logger.log(Level.FINE, "Initializing entities from class path");

            if (cachedClassNameCollection != null) {
                cachedClassNameCollection.clear();
            } else {
                cachedClassNameCollection = new HashSet<String>();
            }

        } else {
            // nothing to do, return!
            return;
        }

        String pathSeparator = System.getProperty("path.separator");
        StringBuilder classPathBuffer = new StringBuilder();

        if (entityLocations != null) {
            for (String location : entityLocations) {
                classPathBuffer.append(location);
                classPathBuffer.append(pathSeparator).append(" ");
            }
        }

        String classPath = classPathBuffer.toString();
        String[] classPathElementArray = classPath.split("[" + pathSeparator + ";]+");

        Set<File> searchableFileElements = new LinkedHashSet<File>();
        Set<File> searchablePathElements = new LinkedHashSet<File>();

        collectStringArrayElements(classPathElementArray, searchablePathElements);

        // recursively list files from all directories
        collectPathElements(searchablePathElements, searchableFileElements);

        // searchableFileElements should now contain every relevant file
        // in every directory in the class path.. (?)
        for (File file : searchableFileElements) {

            String filePath = file.getAbsolutePath();
            String lowerCaseFilePath = filePath.toLowerCase();

            // examine jar files..
            if (lowerCaseFilePath.endsWith(".jar")) {
                try {
                    JarFile jarFile = new JarFile(file);

                    for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {
                        JarEntry entry = e.nextElement();
                        String entryName = entry.getName().toLowerCase();

                        if (entryName.endsWith(".class") && !entryName.contains("$")) {
                            String fileEntry = entry.getName().replaceAll("[/]+", ".");
                            cachedClassNameCollection.add(fileEntry);

                            logger.log(Level.FINEST, "Adding class to entity collection", fileEntry);
                        }
                    }

                } catch (IOException ioex) {
                    logger.log(Level.WARNING, "Exception reading JAR file", ioex);
                }

            } else if (lowerCaseFilePath.endsWith(".class") && !lowerCaseFilePath.contains("$")) {
                String fileEntry = filePath.replaceAll("[/]+", ".").replaceAll("[\\\\]+", ".");
                cachedClassNameCollection.add(fileEntry);

                logger.log(Level.FINEST, "Adding class to entity collection", fileEntry);
            }
        }
    }

    private static void initializeCachedClassCollection(boolean forceReload) {
        initializeCachedClassNameCollection(forceReload);

        if (cachedClassCollection == null || forceReload) {
            if (cachedClassCollection != null) {
                cachedClassCollection.clear();
            } else {
                cachedClassCollection = new HashSet<Class>();
            }

        } else {
            // nothing to do, return!
            return;
        }

        ClassLoader classLoader = ClasspathEntityLocator.class.getClassLoader();
        for (String className : cachedClassNameCollection) {

            /**
             * className ends with ".class"
             * className contains a fully-qualified java class name
             * className may contain additional file system path elements
             */
            String[] parts = className.split("[\\.]+");
            Class classCandidate = null;

            // try to instantiate given class, starting from the shortest possible path.
            for (int startIndex = parts.length - 2; (classCandidate == null && startIndex >= 0); startIndex--) {
                String fullClassName = concatClassName(parts, startIndex);

                try {
                    classCandidate = Class.forName(fullClassName, false, classLoader);
                    cachedClassCollection.add(classCandidate);

                } catch (Throwable t) {
                    // we don't care about the type of exception
                    classCandidate = null;
                }
            }
        }
    }

    /**
     * Concatenates elements of the <code>source</code> array, starting at <code>startIndex</code>,
     * to the returned string.
     *
     * @param source the input array
     * @param startIndex the start index
     * @return a string consisting of array elements startIndex to source.length - 1 separated by "."
     */
    private static String concatClassName(String[] source, int startIndex) {
        StringBuilder ret = new StringBuilder();

        ret.append(source[startIndex]);

        for (int i = startIndex + 1; i < source.length - 1; i++) {
            ret.append(".");
            ret.append(source[i]);
        }

        return (ret.toString());
    }

    /**
     * Adds all elements of the input array <code>elementArray</code> to the returned
     * set of strings.
     *
     * @param elementArray the input array
     * @param destinationSet the output set of strings
     */
    private static void collectStringArrayElements(String[] elementArray, Set<File> destinationSet) {
        for (int i = 0; i < elementArray.length; i++) {
            String element = elementArray[i].trim();

            if (element.length() > 0) {
                destinationSet.add(new File(element));
            }
        }
    }

    /**
     * Adds all elements of the input array <code>elementArray</code> to the returned
     * set of files.
     *
     * @param elementArray the input array
     * @return a set containing all elements of the input array
     */
    private static Set<File> collectFileArrayElements(File[] elementArray) {
        Set<File> destinationSet = new LinkedHashSet<File>();

        if (elementArray != null) {
            destinationSet.addAll(Arrays.asList(elementArray));
        }

        return (destinationSet);
    }

    /**
     * Recursively traverses the input set <code>searchablePathElements</code>, adding JAR and CLASS files to
     * <code>fileElements</code>.
     *
     * @param searchablePathElements the input set of files and directories
     * @param fileElements the output set of JAR and CLASS files
     */
    private static void collectPathElements(Set<File> searchablePathElements, Set<File> fileElements) {
        for (File file : searchablePathElements) {

            if (file.isDirectory()) {
                // recursively dive into subdirs
                collectPathElements(collectFileArrayElements(file.listFiles(new SearchFileFilter())), fileElements);

            } else if (file.isFile()) {
                fileElements.add(file);

                logger.log(Level.FINEST, "Found searchable path element ", file.getAbsolutePath());
            }
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="nested classes">
    /**
     * A file filter that includes directories, JAR files and CLASS files.
     */
    private static class SearchFileFilter implements FileFilter {

        @Override
        public boolean accept(File pathName) {
            String internalPathName = pathName.getAbsolutePath().toLowerCase();

            return (pathName.isDirectory()
                    || internalPathName.endsWith(".jar")
                    || internalPathName.endsWith(".class"));
        }
    }
    // </editor-fold>
}
