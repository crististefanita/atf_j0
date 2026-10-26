package atf.reporting.ui.audit.runtime;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.audit.ValidationResult;
import atf.reporting.ui.audit.plan.ExtentSection;
import atf.reporting.ui.audit.plan.ExtentSectionCheck;

import java.util.List;
import java.util.Objects;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Maps declarative section checks to the assertions owned by a section. */
public final class SectionAuditCheckPolicy {
    private SectionAuditCheckPolicy() { }

    public static ValidationResult select(ExtentSection section,
                                          Set<ExtentSectionCheck> requested,
                                          ValidationResult validation) {
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(requested, "requested");
        Objects.requireNonNull(validation, "validation");
        List<AtfAssertion> selected = validation.assertions().stream()
                .filter(assertion -> requested.stream().anyMatch(check -> owns(section, check, assertion.name())))
                .toList();
        Set<String> allAssertionNames = validation.assertions().stream()
                .map(AtfAssertion::name).collect(Collectors.toSet());
        Set<String> selectedNames = selected.stream().map(AtfAssertion::name)
                .collect(Collectors.toSet());
        List<String> selectedViolations = validation.violations().stream()
                .filter(violation -> allAssertionNames.stream().noneMatch(name -> violation.startsWith(name + ":"))
                        || selectedNames.stream().anyMatch(name -> violation.startsWith(name + ":")))
                .toList();
        // A raw violation has no assertion name; never hide it behind annotation selection.
        return new ValidationResult(selected.stream().allMatch(AtfAssertion::passed)
                && selectedViolations.isEmpty(), selected, selectedViolations);
    }

    /** Returns the checks owned by this section from the declared plan. */
    public static Set<ExtentSectionCheck> applicableChecks(
            ExtentSection section, Set<ExtentSectionCheck> requested) {
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(requested, "requested");
        EnumSet<ExtentSectionCheck> applicable = EnumSet.noneOf(ExtentSectionCheck.class);
        for (ExtentSectionCheck check : requested) {
            if (ownsCheck(section, check)) applicable.add(check);
        }
        return Set.copyOf(applicable);
    }

    private static boolean ownsCheck(ExtentSection section, ExtentSectionCheck check) {
        return switch (section) {
            case OVERVIEW -> check == ExtentSectionCheck.STRUCTURE
                    || check == ExtentSectionCheck.CROSS_SECTION;
            case TESTS -> check == ExtentSectionCheck.STRUCTURE
                    || check == ExtentSectionCheck.CROSS_SECTION
                    || check == ExtentSectionCheck.JSON_CONTRACT
                    || check == ExtentSectionCheck.MOCK_RECONCILIATION
                    || check == ExtentSectionCheck.COLLAPSED_BY_DEFAULT;
            case CATEGORIES -> check == ExtentSectionCheck.STRUCTURE
                    || check == ExtentSectionCheck.STYLE;
            case FAILS -> check == ExtentSectionCheck.STRUCTURE
                    || check == ExtentSectionCheck.CROSS_SECTION
                    || check == ExtentSectionCheck.COLLAPSED_BY_DEFAULT;
            case EVIDENCE -> check == ExtentSectionCheck.STRUCTURE
                    || check == ExtentSectionCheck.JSON_CONTRACT
                    || check == ExtentSectionCheck.MOCK_RECONCILIATION;
            case UI_VALIDATION -> check == ExtentSectionCheck.STRUCTURE
                    || check == ExtentSectionCheck.SCREENSHOT_LINK
                    || check == ExtentSectionCheck.INLINE_ALIGNMENT;
        };
    }

    private static boolean owns(ExtentSection section, ExtentSectionCheck check, String name) {
        return switch (section) {
            case OVERVIEW -> check == (name.equals("Expected report projection")
                    ? ExtentSectionCheck.CROSS_SECTION : ExtentSectionCheck.STRUCTURE);
            case TESTS -> check == ExtentSectionCheck.STRUCTURE
                    || (check == ExtentSectionCheck.CROSS_SECTION && name.contains("projection"));
            case CATEGORIES -> (name.equals("Category surface")
                    ? check == ExtentSectionCheck.STYLE : check == ExtentSectionCheck.STRUCTURE);
            case FAILS -> (name.equals("Collapsed details")
                    ? check == ExtentSectionCheck.COLLAPSED_BY_DEFAULT
                    : (name.contains("deep-link") || name.contains("failure metric"))
                    ? check == ExtentSectionCheck.CROSS_SECTION
                    : check == ExtentSectionCheck.STRUCTURE);
            case EVIDENCE -> (name.equals("Request JSON") || name.equals("Response JSON")
                    || name.equals("JSON violations"))
                    ? (check == ExtentSectionCheck.JSON_CONTRACT
                    || check == ExtentSectionCheck.MOCK_RECONCILIATION)
                    : check == ExtentSectionCheck.STRUCTURE;
            case UI_VALIDATION -> (name.equals("Inline screenshot")
                    ? check == ExtentSectionCheck.SCREENSHOT_LINK
                    : (name.equals("Sibling rows") || name.equals("Vertical composition"))
                    ? check == ExtentSectionCheck.INLINE_ALIGNMENT
                    : check == ExtentSectionCheck.STRUCTURE);
        };
    }
}
