package atf.ui.tests.extent.support.section.module;

import atf.reporting.ui.audit.plan.ExtentSection;

import java.util.EnumMap;
import java.util.Map;

/** Explicit registry: adding a section does not modify a global rule switch. */
public final class SectionAuditRegistry {
    private final Map<ExtentSection, ExtentSectionAuditModule> modules;

    public SectionAuditRegistry() {
        EnumMap<ExtentSection, ExtentSectionAuditModule> configured = new EnumMap<>(ExtentSection.class);
        register(configured, new OverviewSectionAuditModule());
        register(configured, new TestsSectionAuditModule());
        register(configured, new CategoriesSectionAuditModule());
        register(configured, new FailsSectionAuditModule());
        register(configured, new EvidenceSectionAuditModule());
        register(configured, new ScreenValidationSectionAuditModule());
        modules = Map.copyOf(configured);
    }

    public ExtentSectionAuditModule module(ExtentSection section) {
        ExtentSectionAuditModule module = modules.get(section);
        if (module == null) throw new IllegalArgumentException(
                "No section module registered for " + section + "; implement the vertical slice first");
        return module;
    }

    private static void register(Map<ExtentSection, ExtentSectionAuditModule> modules,
                                 ExtentSectionAuditModule module) {
        if (modules.put(module.section(), module) != null) {
            throw new IllegalStateException("Duplicate section module: " + module.section());
        }
    }
}

