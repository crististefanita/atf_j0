package atf.reporting.gherkin;

import atf.reporting.contract.AtfGherkinKeyword;
import java.util.Objects;
import static atf.reporting.contract.AtfGherkinKeyword.AND;
import static atf.reporting.contract.AtfGherkinKeyword.BUT;

/** Scenario-owned wording state; declarations retain their meaning while repeated anchors become continuations. */
public final class ReportGherkinSequence {
    private AtfGherkinKeyword anchor;

    public AtfGherkinKeyword next(AtfGherkinKeyword declared) {
        Objects.requireNonNull(declared, "declared");
        if (declared == AND || declared == BUT) return declared;
        if (declared == anchor) return AND;
        anchor = declared;
        return declared;
    }
}
