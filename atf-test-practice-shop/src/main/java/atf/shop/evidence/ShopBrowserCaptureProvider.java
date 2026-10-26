package atf.shop.evidence;

import atf.api.reporting.evidence.profile.ReportEvidenceCaptureProvider;
import atf.api.reporting.evidence.profile.ReportEvidenceCaptureSource;
import atf.api.reporting.model.ReportScreenshot;
import atf.api.ui.config.UiScreenValidationConfiguration;
import atf.api.ui.playwright.UiScreenValidation;
import atf.shop.locators.ShopLocators;
import com.microsoft.playwright.Page;

import java.util.Objects;

/** Thin Playwright adapter for the renderer-neutral capture provider contract. */
public record ShopBrowserCaptureProvider(Page page,
                                                 UiScreenValidationConfiguration configuration)
        implements ReportEvidenceCaptureProvider {
    public ShopBrowserCaptureProvider {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(configuration, "configuration");
    }

    @Override public ReportEvidenceCaptureSource source() {
        return ReportEvidenceCaptureSource.BROWSER;
    }

    @Override public ReportScreenshot capture(String title, String target) {
        var sensitiveFields = new ShopLocators().sensitiveFields(page);
        var evidence = UiScreenValidation.capture(page, configuration, title, sensitiveFields);
        return new ReportScreenshot(evidence.title(), evidence.base64Png());
    }
}
