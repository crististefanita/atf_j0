package atf.reporting.theme.tokens;

import atf.reporting.asset.ReportAssetComposer;

/** Stable facade for the renderer-neutral standalone fallback stylesheet. */
public final class AtfFallbackTheme {
    private AtfFallbackTheme() {
    }

    public static String css() {
        return ReportAssetComposer.fallbackCss();
    }
}
