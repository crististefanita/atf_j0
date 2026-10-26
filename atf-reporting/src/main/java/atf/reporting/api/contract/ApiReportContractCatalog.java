package atf.reporting.api.contract;

import java.util.List;

/** Port through which any source of expected API exchanges can be audited. */
@FunctionalInterface
public interface ApiReportContractCatalog {
    List<ApiReportRouteContract> routes();
}
