package atf.ui.tests.extent.support.section.module;
import atf.reporting.ui.contract.ReportUiContract;

import atf.reporting.ui.audit.plan.ExtentSection;
import atf.reporting.ui.audit.plan.ExtentSectionGherkin;
import atf.reporting.ui.audit.plan.SectionAuditSpec;
import atf.ui.tests.extent.support.section.runtime.SectionAuditFixture;
import atf.reporting.ui.audit.result.SectionAuditResult;
import com.microsoft.playwright.Page;

/** One plug-and-play vertical slice for one report section. */
public interface ExtentSectionAuditModule {
    ExtentSection section();

    SectionAuditResult audit(SectionAuditFixture fixture, SectionAuditSpec spec);

    default String auditTitle() {
        return ExtentSectionGherkin.proposition(section());
    }

    default String evidenceTitle() {
        // UiAuditFlow adds the THEN keyword at publication time.
        return ExtentSectionGherkin.thenStatement(section());
    }

    default String evidenceTarget() {
        return evidenceTab().hash();
    }

    default Page evidencePage(SectionAuditFixture fixture) {
        return fixture.audit().evidencePage(evidenceTab());
    }

    default ReportUiContract.Tab evidenceTab() {
        return switch (section()) {
            case OVERVIEW -> ReportUiContract.Tab.OVERVIEW;
            case TESTS, EVIDENCE, UI_VALIDATION -> ReportUiContract.Tab.TESTS;
            case CATEGORIES -> ReportUiContract.Tab.CATEGORIES;
            case FAILS -> ReportUiContract.Tab.FAILS;
        };
    }
}

