package com.vaadin.framework8.migrate;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author mavi
 */
public class JavaFile {
    private String contents;

    public String getContents() {
        return contents;
    }

    public JavaFile(String contents) {
        this.contents = Objects.requireNonNull(contents);
    }

    public List<String> getLines() {
        return Arrays.asList(contents.split("\\r?\\n"));
    }

    /**
     * Returns all imports, e.g. ["com.vaadin.ui.UI", "com.vaadin.ui.*"].
     * @return a set of imports, not null, may be empty.
     */
    public Set<String> getImports() {
        return getLines().stream()
                .filter(it -> it.startsWith("import ") && it.endsWith(";"))
                .map(it -> it.substring("import ".length(), it.length() - 1))
                .collect(Collectors.toSet());
    }

    /**
     * Return only Vaadin star imports, e.g. ["com.vaadin.ui.*", "com.vaadin.data.*"].
     * @return a set of imports, not null, may be empty.
     */
    public Set<String> getVaadinStarImports() {
        return getImports().stream()
                .filter(it -> it.startsWith("com.vaadin.") && it.endsWith(".*"))
                .collect(Collectors.toSet());
    }

    public void performReplacement(String comvaadinClass, String v7Class) {
        contents = contents.replace("import " + comvaadinClass + ";",
                "import " + v7Class + ";");
        contents = contents.replace("extends " + comvaadinClass + " ",
                "extends " + v7Class + " ");
        contents = contents.replace("implements " + comvaadinClass + " ",
                "implements " + v7Class + " ");
        contents = contents.replace("throws " + comvaadinClass + " ",
                "throws " + v7Class + " ");
    }

    public void removeImport(String className) {
        contents = contents.replace("import " + className + ";\n", "");
    }

    public void addImportAbove(String className, String newClassName) {
        contents = contents.replace("import " + className + ";\n", "import " + newClassName + ";\nimport " + className + ";\n");
    }
}
