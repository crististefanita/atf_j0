package atf.ui.config.geometry;

import atf.ui.config.UiBrowserConfiguration;
import java.util.Locale;
import java.util.Objects;

/** Immutable browser geometry: output intent is separate from the native CSS layout and pixel density. */
public final class UiViewportScale {
    private final int outputWidth;
    private final int outputHeight;
    private final int layoutWidth;
    private final int layoutHeight;
    private final double deviceScaleFactor;

    public UiViewportScale(UiBrowserConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration");
        outputWidth = configuration.viewportWidth();
        outputHeight = configuration.viewportHeight();
        deviceScaleFactor = configuration.zoomPercent() / 100d;
        layoutWidth = layout(outputWidth, deviceScaleFactor);
        layoutHeight = layout(outputHeight, deviceScaleFactor);
    }

    private static int layout(int output, double factor) {
        try {
            return Math.max(1, Math.toIntExact(Math.round(output / factor)));
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("Scaled viewport exceeds the native integer range", overflow);
        }
    }

    public int layoutWidth() { return layoutWidth; }
    public int layoutHeight() { return layoutHeight; }
    public double deviceScaleFactor() { return deviceScaleFactor; }

    /** Configured intent, not a claim that a screenshot was captured or measured. */
    public String description() {
        return String.format(Locale.ROOT, "%d × %d requested output · %d × %d CSS · DPR %.2f",
                outputWidth, outputHeight, layoutWidth, layoutHeight, deviceScaleFactor);
    }
}
