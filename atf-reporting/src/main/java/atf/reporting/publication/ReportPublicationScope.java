package atf.reporting.publication;

/** Internal publication protocol shared by direct and delegated evidence publishers. */
public final class ReportPublicationScope {
    private ReportPublicationScope() { }

    /** One attempt and one close; the primary failure retains its identity, including self-suppression. */
    public static void publishScope(Runnable body, Runnable close) {
        Throwable primary = null;
        try {
            body.run();
        } catch (RuntimeException | Error failure) {
            primary = failure;
            throw failure;
        } finally {
            try {
                close.run();
            } catch (RuntimeException | Error cleanup) {
                if (primary == null) throw cleanup;
                if (cleanup != primary) primary.addSuppressed(cleanup);
            }
        }
    }
}
