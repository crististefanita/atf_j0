package atf.reporting.runtime.core;

import atf.reporting.asset.ReportAssetComposer;

/**
 * Shared DOM primitives exposed by three native JavaScript seams.
 *
 * <p>The public runtime contract remains one script and one module owner;
 * asset composition is kept here in the same dependency order as the
 * browser vocabulary.</p>
 */
public final class ExtentReportRuntimeCore {
    private ExtentReportRuntimeCore() {
    }

    public static String script() {
        return String.join("", ReportAssetComposer.runtimeCoreDomScript(),
                ReportAssetComposer.runtimeCoreScenariosScript(),
                ReportAssetComposer.runtimeCoreMetadataScript());
    }
}

