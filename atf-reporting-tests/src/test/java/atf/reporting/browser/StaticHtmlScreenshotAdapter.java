package atf.reporting.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Test-scope browser seam for proving that a local report can become a real
 * PNG without calling a service or a Practice Shop endpoint.
 *
 * <p>Playwright stays at this boundary. Production reporting remains
 * renderer-neutral and receives no browser dependency.</p>
 */
public final class StaticHtmlScreenshotAdapter {
    private static final int PNG_SIGNATURE_LENGTH = 8;
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
    };

    public Capture capture(Path html, Path output, int viewportWidth, int viewportHeight) {
        Path input = requireRegularFile(html, "HTML report");
        if (viewportWidth < 1 || viewportHeight < 1) {
            throw new IllegalArgumentException("viewport dimensions must be positive");
        }
        Objects.requireNonNull(output, "output");
        try {
            if (output.getParent() != null) {
                Files.createDirectories(output.getParent());
            }
            try (Playwright playwright = Playwright.create();
                 Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                         .setHeadless(true))) {
                Page page = browser.newPage(new Browser.NewPageOptions()
                        .setViewportSize(viewportWidth, viewportHeight));
                page.navigate(input.toUri().toString());
                page.screenshot(new Page.ScreenshotOptions()
                        .setPath(output)
                        .setFullPage(true));
            }
            return new Capture(output.toAbsolutePath().normalize(), viewportWidth, viewportHeight,
                    isPng(output));
        } catch (IOException error) {
            throw new IllegalStateException("Cannot create static report screenshot " + output, error);
        }
    }

    private static Path requireRegularFile(Path path, String label) {
        Path value = Objects.requireNonNull(path, label).toAbsolutePath().normalize();
        if (!Files.isRegularFile(value)) {
            throw new IllegalArgumentException(label + " does not exist: " + value);
        }
        return value;
    }

    private static boolean isPng(Path output) throws IOException {
        if (!Files.isRegularFile(output) || Files.size(output) < PNG_SIGNATURE_LENGTH) {
            return false;
        }
        byte[] header = Files.readAllBytes(output);
        if (header.length < PNG_SIGNATURE.length) return false;
        for (int index = 0; index < PNG_SIGNATURE.length; index++) {
            if (header[index] != PNG_SIGNATURE[index]) return false;
        }
        return true;
    }

    public record Capture(Path output, int viewportWidth, int viewportHeight, boolean png) {
        public Capture {
            output = Objects.requireNonNull(output, "output");
            if (viewportWidth < 1 || viewportHeight < 1) {
                throw new IllegalArgumentException("viewport dimensions must be positive");
            }
        }
    }
}
