package atf.reporting.contract.evidence;

import java.util.Locale;

/** Assertion ownership is semantic, independent of labels and HTTP outcome. */
public enum ReportEvidenceOwner {
    UNSPECIFIED, UI, API;

    public String attributeValue() { return name().toLowerCase(Locale.ROOT); }
}
