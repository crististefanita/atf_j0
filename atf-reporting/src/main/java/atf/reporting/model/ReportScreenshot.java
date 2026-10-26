package atf.reporting.model;

import atf.reporting.markup.AtfReportLabels;

import java.util.Base64;
import java.util.Objects;

/** Immutable screenshot evidence item shared by Extent and fallback renderers. */
public record ReportScreenshot(String title, String base64Png) {
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    public ReportScreenshot {
        title = title == null || title.isBlank() ? AtfReportLabels.UI_SCREENSHOT : title.trim();
        base64Png = requirePayload(base64Png);
    }

    /** Checks Base64 encoding and the PNG signature, without decoding image pixels. */
    public static boolean hasPngSignature(String payload) {
        if (payload == null || payload.isBlank()) return false;
        final byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(payload.trim());
        } catch (IllegalArgumentException invalidBase64) {
            return false;
        }
        if (decoded.length < PNG_SIGNATURE.length) return false;
        for (int index = 0; index < PNG_SIGNATURE.length; index++) {
            if (decoded[index] != PNG_SIGNATURE[index]) return false;
        }
        return true;
    }

    private static String requirePayload(String value) {
        String normalized = Objects.requireNonNull(value, "base64Png").trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("base64Png must not be blank");
        }
        return normalized;
    }
}
