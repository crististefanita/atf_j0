package atf.reporting.support;

/** Valid PNG fixtures shared by reporting tests; never use arbitrary Base64 bytes as an image. */
public final class ReportTestScreenshots {
    public static final String ONE_PIXEL_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";
    public static final String SECOND_PIXEL_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/0A0AAAAASUVORK5CYII=";

    private ReportTestScreenshots() {
    }
}
