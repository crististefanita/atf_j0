package atf.reporting.api;

import atf.reporting.contract.AtfReportStatus;
import java.util.*;

/** Renderer-neutral selection identity; concrete annotation catalogs stay outside reporting. */
public record ReportScenarioSelection(Class<?> ownerType, String method, String family, String reportOwner,
                                      String scenarioTitle, List<String> reportScenarioTitles,
                                      List<AtfReportStatus> reportScenarioStatuses) {
    public ReportScenarioSelection(Class<?> ownerType,String method,String family,String reportOwner,String scenarioTitle){this(ownerType,method,family,reportOwner,scenarioTitle,List.of(scenarioTitle),List.of(AtfReportStatus.PASS));}
    public ReportScenarioSelection { ownerType=Objects.requireNonNull(ownerType); method=required(method,"method"); family=required(family,"family"); reportOwner=required(reportOwner,"reportOwner"); scenarioTitle=required(scenarioTitle,"scenarioTitle"); reportScenarioTitles=List.copyOf(reportScenarioTitles); reportScenarioStatuses=List.copyOf(reportScenarioStatuses); }
    public String reference(){return ownerType.getSimpleName()+"."+method;}
    private static String required(String v,String f){if(v==null||v.isBlank())throw new IllegalArgumentException(f+" must not be blank");return v.trim();}
}
