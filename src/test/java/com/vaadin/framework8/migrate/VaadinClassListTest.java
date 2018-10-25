package com.vaadin.framework8.migrate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author mavi
 */
public class VaadinClassListTest {
    private VaadinClassList cl;
    @BeforeEach
    public void load() throws Exception {
        cl = VaadinClassList.getForVaadin("8.5.2");
    }

    @Test
    public void starImportMatcher() throws Exception {
        Set<String> set = cl.getClassesMatchingStarImport("com.vaadin.v7.ui.*");
        assertTrue(set.contains("com.vaadin.v7.ui.Field"), "" + set);
        // UI is not in the compat package.
        assertFalse(set.contains("com.vaadin.v7.ui.UI"), "" + set);
        assertFalse(set.contains("com.vaadin.v7.ui.renderers.ClickableRenderer"), "" + set);
        set = cl.getClassesMatchingStarImport("com.vaadin.v7.ui.renderers.*");
        assertFalse(set.contains("com.vaadin.v7.ui.Field"), "" + set);
        assertTrue(set.contains("com.vaadin.v7.ui.renderers.ClickableRenderer"), "" + set);
    }
}
