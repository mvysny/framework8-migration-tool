package com.vaadin.framework8.migrate;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author mavi
 */
public class MigrationTool {
    private final VaadinClassList classList;
    private final Map<String, String> specialRenames = new HashMap<>();
    private final String vaadin8Version;
    private final File projectRoot;
    private final Charset charset;

    public MigrationTool(String vaadin8Version, File projectRoot, Charset charset) throws IOException {
        this.vaadin8Version = Objects.requireNonNull(vaadin8Version);
        this.projectRoot = Objects.requireNonNull(projectRoot);
        this.charset = Objects.requireNonNull(charset);
        classList = VaadinClassList.getForVaadin(vaadin8Version);

        specialRenames.put("com.vaadin.data.fieldgroup.PropertyId",
                "com.vaadin.annotations.PropertyId");
        specialRenames.put("com.vaadin.shared.ui.grid.Range",
                "com.vaadin.shared.Range");
    }

    public void migrate() throws Exception {
        String version = vaadin8Version;

        AtomicInteger javaCount = new AtomicInteger(0);
        AtomicInteger htmlCount = new AtomicInteger(0);
        migrateFiles(projectRoot, javaCount, htmlCount, version);

        System.out.println("Scanned " + javaCount.get() + " Java files");
        System.out.println("Scanned " + htmlCount.get() + " HTML files");
        System.out.println("Migration complete");
    }

    private void migrateFiles(File directory, AtomicInteger javaCount,
                                     AtomicInteger htmlCount, String version) throws IOException {
        assert directory.isDirectory();

        for (File f : directory.listFiles()) {
            if (f.isDirectory()) {
                migrateFiles(f, javaCount, htmlCount, version);
            } else if (isJavaFile(f)) {
                javaCount.incrementAndGet();
                migrateJava(f);
            } else if (isDeclarativeFile(f)) {
                htmlCount.incrementAndGet();
                migrateDeclarative(f, version);
            }
        }
    }

    private static boolean isJavaFile(File f) {
        return f.getName().endsWith(".java");
    }

    private static boolean isDeclarativeFile(File f) {
        return f.getName().endsWith(".html");
    }

    private void migrateJava(File f) throws IOException {
        String javaFile = IOUtils.toString(f.toURI(), charset);
        String migratedFile = modifyJava(javaFile);
        if (!javaFile.equals(migratedFile)) {
            FileUtils.write(f, migratedFile, charset);
        }
    }

    private void migrateDeclarative(File f, String version)
            throws IOException {
        String htmlFile = IOUtils.toString(f.toURI(), StandardCharsets.UTF_8);
        final String migratedFile = modifyDeclarative(htmlFile, version);
        if (!htmlFile.equals(migratedFile)) {
            IOUtils.write(migratedFile, new FileOutputStream(f), StandardCharsets.UTF_8);
        }
    }

    private String modifyJava(String javaFile) {
        for (String v7Class : classList.getAllClasses()) {

            String comvaadinClass = v7Class.replace("com.vaadin.v7.",
                    "com.vaadin.");
            javaFile = performReplacement(javaFile, comvaadinClass, v7Class);
        }

        for (Map.Entry<String, String> rename : specialRenames.entrySet()) {
            javaFile = performReplacement(javaFile, rename.getKey(),
                    rename.getValue());
        }

        javaFile = javaFile.replace("import com.vaadin.ui.*;", "import com.vaadin.v7.ui.*;");
        javaFile = javaFile.replace("import com.vaadin.data.*;", "import com.vaadin.v7.data.*;");
        javaFile = javaFile.replace("import com.vaadin.data.util.*;", "import com.vaadin.v7.data.util.*;");
        javaFile = javaFile.replace("import com.vaadin.data.validator.*;", "import com.vaadin.v7.data.validator.*;");
        javaFile = javaFile.replace("import com.vaadin.data.util.converter.*;", "import com.vaadin.v7.data.util.converter.*;");
        javaFile = javaFile.replace("import com.vaadin.data.util.filter.*;", "import com.vaadin.v7.data.util.filter.*;");
        javaFile = javaFile.replace("import com.vaadin.data.util.sqlcontainer.*;", "import com.vaadin.v7.data.util.sqlcontainer.*;");

        return javaFile;
    }

    private static String performReplacement(String javaFile,
                                             String comvaadinClass, String v7Class) {
        javaFile = javaFile.replace("import " + comvaadinClass + ";",
                "import " + v7Class + ";");
        javaFile = javaFile.replace("extends " + comvaadinClass + " ",
                "extends " + v7Class + " ");
        javaFile = javaFile.replace("implements " + comvaadinClass + " ",
                "implements " + v7Class + " ");
        javaFile = javaFile.replace("throws " + comvaadinClass + " ",
                "throws " + v7Class + " ");
        return javaFile;
    }

    private String modifyDeclarative(String htmlFile, String version) {
        for (String v7Class : classList.serverV7UIClasses) {
            String simpleClassName = v7Class
                    .substring(v7Class.lastIndexOf('.') + 1);
            String tagName = classNameToElementName(simpleClassName);

            String legacyStartTag = "<v-" + tagName + ">";
            String legacyStartTag2 = "<v-" + tagName + " ";
            String startTag = "<vaadin-" + tagName + ">";
            String startTag2 = "<vaadin-" + tagName + " ";
            String newStartTag = "<vaadin7-" + tagName + ">";
            String newStartTag2 = "<vaadin7-" + tagName + " ";
            String legacyEndTag = "</v-" + tagName + ">";
            String endTag = "</vaadin-" + tagName + ">";
            String newEndTag = "</vaadin7-" + tagName + ">";

            htmlFile = htmlFile.replace(legacyStartTag, newStartTag);
            htmlFile = htmlFile.replace(startTag, newStartTag);
            htmlFile = htmlFile.replace(legacyStartTag2, newStartTag2);
            htmlFile = htmlFile.replace(startTag2, newStartTag2);

            htmlFile = htmlFile.replace(legacyEndTag, newEndTag);
            htmlFile = htmlFile.replace(endTag, newEndTag);

            // Version
            htmlFile = htmlFile.replaceAll(
                    "<meta(.*)name=\"vaadin-version\"(.*)content=\"7.*\"(.*)>",
                    "<meta name=\"vaadin-version\" content=\"" + version
                            + "\">");
        }

        return htmlFile;
    }

    /**
     * From Design.java
     */
    private static String classNameToElementName(String className) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < className.length(); i++) {
            char c = className.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append("-");
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

}
