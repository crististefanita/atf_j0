package atf.reporting.artifact.contract;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

public record ReportArtifactContractEvaluation(
        Map<String, Boolean> checks,
        Map<String, String> owners) {
    public ReportArtifactContractEvaluation {
        checks = Collections.unmodifiableMap(new LinkedHashMap<>(checks));
        owners = Collections.unmodifiableMap(new LinkedHashMap<>(owners));
    }
}
