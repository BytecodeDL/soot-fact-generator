package org.clyze.doop.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/**
 * This class gathers Java-specific code (such as JAR handling).
 */
public class BasicJavaSupport {

    protected final Set<String> classesInApplicationJars = ConcurrentHashMap.newKeySet();
    protected final Set<String> classesInLibraryJars = ConcurrentHashMap.newKeySet();
    protected final Set<String> classesInDependencyJars = ConcurrentHashMap.newKeySet();
    private final PropertyProvider propertyProvider = new PropertyProvider();
    private final Parameters parameters;
    private final ArtifactScanner artScanner;
    // Executor for async big tasks such as apk decoding or library scanning.
    private final ExecutorService exec = Executors.newFixedThreadPool(3);
    public final Collection<String> xmlRoots = ConcurrentHashMap.newKeySet();

    private static final Logger logger = LogManager.getLogger(BasicJavaSupport.class);

    public BasicJavaSupport(Parameters parameters, ArtifactScanner artScanner) {
        this.parameters = parameters;
        this.artScanner = artScanner;
    }

    public ArtifactScanner getArtifactScanner() {
        return artScanner;
    }

    public ExecutorService getExecutor() {
        return exec;
    }

    /**
     * Helper method to read classes and resources from input archives.
     */
    public void preprocessInputs(Database db, Set<String> tmpDirs) throws IOException {
        Set<String> tmpClasses = ConcurrentHashMap.newKeySet();
        for (String filename : parameters.getInputs()) {
            logger.info("Preprocessing application: " + filename);
            preprocessInput(db, tmpDirs, tmpClasses, filename);
        }
        for (String filename : parameters.getPlatformLibs()) {
            logger.info("Preprocessing platform library: " + filename);
            preprocessInput(db, tmpDirs, classesInLibraryJars, filename);
        }
        for (String filename : parameters.getDependencies()) {
            logger.info("Preprocessing dependency: " + filename);
            preprocessInput(db, tmpDirs, classesInDependencyJars, filename);
        }

        classifyClasses(tmpClasses);
    }

    public void classifyClasses(Set<String> tmpClasses){
        for (String filename : tmpClasses) {
            if (parameters.isApplicationClass(filename)){
                classesInApplicationJars.add(filename);
            }else{
                classesInLibraryJars.add(filename);
            }
        }
    }

    /**
     * Preprocess an input archive.
     *
     * @param db         the database object to use
     * @param tmpDirs    the temporary directories set (for clean up)
     * @param classSet   appropriate set to add class names
     * @param filename   the input filename
     */
    private void preprocessInput(Database db, Set<String> tmpDirs,
                                 Collection<String> classSet, String filename) throws IOException {
        String filenameL = filename.toLowerCase();
        boolean isAar = filenameL.endsWith(".aar");
        boolean isJar = filenameL.endsWith(".jar");
        boolean isWar = filenameL.endsWith(".war");
        boolean isZip = filenameL.endsWith(".zip");
        boolean isClass = filenameL.endsWith(".class");
        boolean isApk = filenameL.endsWith(".apk");
        boolean isSpringBoot = parameters.isSpringBootJar(filename);

        ArtifactScanner.EntryProcessor gProc = (jarFile, entry, entryName) -> {
            if (entryName.endsWith(".properties"))
                propertyProvider.addProperties(jarFile.getInputStream(entry), filename);
            else if ((isJar || isAar || isZip || isWar) && entryName.endsWith(".xml")) {
                // We only handle .xml entries inside JAR archives here.
                // APK archives may contain binary XML and need decoding.
                File xmlTmpFile = ArtifactScanner.extractZipEntryAsFile("xml-file", jarFile, entry, entryName);
                if (parameters._debug)
                    logger.info("Processing XML entry (in " + filename + "): " + entryName);
                XMLFactGenerator.processFile(xmlTmpFile, db, "", parameters._debug);
            }
        };

        if (isWar) {
            logger.info("Processing WAR: " + filename);
            // Process WAR inputs.
            parameters.processFatArchives(tmpDirs);
        }else if (isSpringBoot) {
            logger.info("Processing springBoot: " + filename);
            parameters.processSpringBootArchives(tmpDirs, filename);
            parameters.getInputs().forEach(file -> {
                try{
//                    logger.info(file);
                    artScanner.processArchive(file, classSet::add, gProc);
                }catch (Exception e){
                    e.printStackTrace();
                }
            });
            artScanner.processArchive(filename, classSet::add, gProc);
        }else if (isJar || isApk || isZip || isWar)
        {
            artScanner.processArchive(filename, classSet::add, gProc);
        } else if (isClass) {
            File f = new File(filename);
            try (FileInputStream fis = new FileInputStream(f)) {
                artScanner.processClass(fis, f, classSet::add);
            }
        } else
            logger.warn("WARNING: artifact scanner skips " + filename);
    }

    public PropertyProvider getPropertyProvider() {
        return propertyProvider;
    }

    public Set<String> getClassesInApplicationJars() {
        return classesInApplicationJars;
    }

    public Set<String> getClassesInLibraryJars() {
        return classesInLibraryJars;
    }

    public Set<String> getClassesInDependencyJars() {
        return classesInDependencyJars;
    }
}
