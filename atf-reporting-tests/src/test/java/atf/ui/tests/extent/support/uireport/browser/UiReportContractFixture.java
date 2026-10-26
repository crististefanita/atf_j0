package atf.ui.tests.extent.support.uireport.browser;
import atf.ui.tests.extent.support.audit.ReportAuditContext;
import atf.reporting.ui.audit.input.ReportAuditInput;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;
import atf.reporting.ui.contract.UiReportContractSpec;
import atf.ui.tests.extent.support.uireport.contract.UiReportInput;
import atf.ui.tests.extent.support.uireport.contract.UiReportInputResolver;
import atf.reporting.ui.audit.vocabulary.UiAuditGherkin;

import atf.ui.tests.extent.support.config.UiConfiguration;
import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.ui.tests.extent.support.section.extract.ScreenValidationSectionExtractor;

import java.util.Objects;

/** Composition root for one read-only static UI report audit. */
public final class UiReportContractFixture implements AutoCloseable {
    private final UiReportContractSpec spec;
    private final UiReportInput input;
    private final ReportAuditContext context;
    private final UiAuditFlow audit;
    private final ReportUiLocators locators;
    private final ScreenValidationSectionExtractor screenValidation;
    private final UiReportNavigationFlow navigation;

    private UiReportContractFixture(UiReportContractSpec spec, UiReportInput input,
                                    ReportAuditContext context, UiAuditFlow audit,
                                    ReportUiLocators locators,
                                    ScreenValidationSectionExtractor screenValidation,
                                    UiReportNavigationFlow navigation) {
        this.spec = spec;
        this.input = input;
        this.context = context;
        this.audit = audit;
        this.locators = locators;
        this.screenValidation = screenValidation;
        this.navigation = navigation;
    }

    public static UiReportContractFixture open(Class<?> owner, UiConfiguration configuration) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(configuration, "configuration");
        UiReportContractSpec spec = UiReportContractSpec.resolve(owner);
        UiReportInput input = UiReportInputResolver.resolve(owner, spec, configuration);
        return open(owner, configuration, input);
    }

    public static UiReportContractFixture open(Class<?> owner, UiConfiguration configuration,
                                               UiReportInput input) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(configuration, "configuration");
        Objects.requireNonNull(input, "input");
        UiReportContractSpec spec = UiReportContractSpec.resolve(owner);
        ReportAuditContext context = ReportAuditContext.open(configuration,
                ReportAuditInput.existing(input.report()), spec.id());
        ReportUiLocators locators = new ReportUiLocators();
        UiAuditFlow audit = new UiAuditFlow(context, configuration, owner.getSimpleName(),
                owner.getPackageName(),
                java.util.List.of("ui", "extent", "static", "contract"),
                UiAuditGherkin.STATIC_UI_REPORT_READY,
                "UI Extent contract failed", spec.id());
        return new UiReportContractFixture(spec, input, context, audit, locators,
                new ScreenValidationSectionExtractor(audit, locators),
                new UiReportNavigationFlow(audit, locators));
    }

    public UiReportContractSpec spec() { return spec; }
    public UiReportInput input() { return input; }
    public ReportAuditContext context() { return context; }
    public UiAuditFlow audit() { return audit; }
    public ReportUiLocators locators() { return locators; }
    public ScreenValidationSectionExtractor screenValidation() { return screenValidation; }
    public UiReportNavigationFlow navigation() { return navigation; }

    public com.microsoft.playwright.Page openTab(ReportUiContract.Tab tab) {
        return audit.openTab(tab.hash());
    }

    @Override
    public void close() {
        context.close();
    }
}
