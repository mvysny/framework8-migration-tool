package com.vaadin.framework8.migrate;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Scans Vaadin {@code vaadin-server.jar}, {@code vaadin-shared.jar} and {@code vaadin-client.jar} for Vaadin classes and builds
 * a list of v7 compat class names. See {@link #serverV7Classes}, {@link #sharedV7Classes}, {@link #serverV7UIClasses}
 * and {@link #clientV7Classes} for more details.
 * @author mavi
 */
public class VaadinClassList {
    /**
     * Contains full names of server v7 classes, such as "com.vaadin.v7.ui.Button".
     */
    public final Set<String> serverV7Classes = new HashSet<>();
    /**
     * Contains full names of shared v7 classes.
     */
    public final Set<String> sharedV7Classes = new HashSet<>();
    /**
     * A subset of {@link #serverV7Classes}, only contains "com.vaadin.v7.ui.*" classes.
     */
    public final Set<String> serverV7UIClasses = new HashSet<>();
    /**
     * Contains full names of client v7 classes.
     */
    public final Set<String> clientV7Classes = new HashSet<>();

    /**
     * Return full names of all Vaadin classes (classes in the "com.vaadin.ui" package). For example returns "com.vaadin.v7.ui.Button".
     * @return a set, not null, never empty.
     */
    public Set<String> getAllClasses() {
        return Stream.concat(Stream.concat(serverV7Classes.stream(),
                        sharedV7Classes.stream()), clientV7Classes.stream())
                .collect(Collectors.toSet());
    }

    public static VaadinClassList getForVaadin(String version) throws IOException {
        System.out.println("Scanning for compatibility classes for " + version
                + " version...");
        String compatServerFilename = VadinJarFinder
                .get("vaadin-compatibility-server", version);
        String compatSharedFilename = VadinJarFinder
                .get("vaadin-compatibility-shared", version);
        String compatClientFilename = VadinJarFinder
                .get("vaadin-compatibility-client", version);

        final VaadinClassList cl = new VaadinClassList();

        findV7Classes(compatServerFilename, cl.serverV7Classes);
        findV7Classes(compatSharedFilename, cl.sharedV7Classes);
        findV7Classes(compatClientFilename, cl.clientV7Classes);

        // This is used in interface and will break more than it fixes
        cl.clientV7Classes.remove("com.vaadin.v7.client.ComponentConnector");

        cl.serverV7UIClasses.addAll(cl.serverV7Classes.stream().filter(
                cls -> cls.matches("^com\\.vaadin\\.v7\\.ui\\.[^\\.]*$"))
                .collect(Collectors.toSet()));

        System.out.println("Found " + cl.serverV7Classes.size() + "+"
                + cl.sharedV7Classes.size() + " classes, including "
                + cl.serverV7UIClasses.size() + " UI classes");

        return cl;
    }

    private static void findV7Classes(String jarFilename, Set<String> target)
            throws ZipException, IOException {
        File serverFile = new File(jarFilename);
        try (ZipFile jar = new ZipFile(serverFile)) {
            Enumeration<? extends ZipEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                getVaadin7Class(entry).ifPresent(target::add);
            }
        }
    }

    private static Optional<String> getVaadin7Class(ZipEntry entry) {
        String name = entry.getName();
        if (name.startsWith("com/vaadin/v7") && name.endsWith(".class")) {
            name = name.replace('/', '.');
            name = name.replace('$', '.');
            name = name.replace(".class", "");
            return Optional.of(name);
        }

        return Optional.empty();
    }

    /**
     * Returns all classes matching given star import. For example, fetching 'com.vaadin.v7.ui.*' will return
     * 'com.vaadin.v7.ui.UI' but not 'com.vaadin.v7.ui.renderers.ImageRenderer`.
     * @param starImport the star import, must start with 'com.vaadin.v7.' and end with '*'
     * @return a set of matching class names, not null, may be empty.
     */
    public Set<String> getClassesMatchingStarImport(String starImport) {
        if (!starImport.startsWith("com.vaadin.v7.")) {
            throw new IllegalArgumentException("Parameter starImport: invalid value " + starImport + ": must start with com.vaadin.v7.");
        }
        if (!starImport.endsWith(".*")) {
            throw new IllegalArgumentException("Parameter starImport: invalid value " + starImport + ": must end with .*");
        }
        final Pattern pattern = Pattern.compile(starImport.replace(".", "\\.").replace("*", "[^\\.]+"));
        return getAllClasses().stream()
                .filter(it -> pattern.matcher(it).matches())
                .collect(Collectors.toSet());
    }
}
