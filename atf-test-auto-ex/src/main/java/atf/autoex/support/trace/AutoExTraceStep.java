package atf.autoex.support.trace;

import atf.reporting.contract.AtfGherkinKeyword;

/** A site-owned business checkpoint, independent of the JUnit/report composition root. */
public interface AutoExTraceStep {
    AtfGherkinKeyword keyword();
    String title();
}
