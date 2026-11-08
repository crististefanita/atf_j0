package atf.reporting.support;

import atf.core.http.response.ApiExecutionResult;
import atf.reporting.api.ReportModelAdapters;
import atf.reporting.api.evidence.ApiExchangeEvidence;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.ui.model.AtfUiActionRecord;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/** Test-scope bridge: keeps ATF lifecycle tests on the legacy core port while the renderer is standalone. */
public final class CoreReportPortsAdapter implements atf.core.reporting.contract.AtfReportPorts.Session {
    private final AtfReportPorts.Session delegate;
    private CoreReportPortsAdapter(AtfReportPorts.Session delegate){this.delegate=delegate;}
    public static CoreReportPortsAdapter wrap(AtfReportPorts.Session delegate){return new CoreReportPortsAdapter(delegate);}
    /** Preserve the complete typed snapshot when this legacy port wraps Reporting. */
    public static void publishEvidence(atf.core.reporting.contract.AtfReportPorts.Request target,
                                       ReportExecutionEvidence evidence,
                                       ApiExecutionResult legacy) {
        if (target instanceof Request adapted) {
            ApiExchangeEvidence.publish(adapted.delegate, evidence);
        } else {
            ApiExchangeEvidence.publishFields(evidence, target::detail, target::json);
            target.execution(java.util.Objects.requireNonNull(legacy, "legacy execution"));
        }
    }
    public Path reportPath(){return delegate.reportPath();}
    public atf.core.reporting.contract.AtfReportPorts.Scenario startScenario(String testClass, String scenario, Collection<String> tags){return new Scenario(delegate.startScenario(testClass,scenario,tags));}
    public atf.core.reporting.contract.AtfReportPorts.Scenario startScenario(String testClass, String testPackage, String scenario, Collection<String> tags){return new Scenario(delegate.startScenario(testClass,testPackage,scenario,tags));}
    public void flush(){delegate.flush();} public void close(){delegate.close();}
    private static AtfReportStatus status(atf.core.reporting.contract.AtfReportStatus s){return ReportModelAdapters.status(s);}
    private static AtfAssertion assertion(atf.core.reporting.contract.AtfAssertion a){return ReportModelAdapters.assertion(a);}
    private static AtfUiActionRecord action(atf.core.ui.AtfUiActionRecord a){return ReportModelAdapters.uiAction(a);}
    private static final class Scenario implements atf.core.reporting.contract.AtfReportPorts.Scenario {
        private final AtfReportPorts.Scenario delegate; Scenario(AtfReportPorts.Scenario d){delegate=d;}
        public String id(){return delegate.id();} public atf.core.reporting.contract.AtfReportPorts.Phase startPhase(String name){return new Phase(delegate.startPhase(name));}
        public void complete(atf.core.reporting.contract.AtfReportStatus s){delegate.complete(status(s));} public boolean hasWarnings(){return delegate.hasWarnings();} public void close(){delegate.close();}
    }
    private static final class Phase implements atf.core.reporting.contract.AtfReportPorts.Phase {
        private final AtfReportPorts.Phase delegate; Phase(AtfReportPorts.Phase d){delegate=d;}
        public atf.core.reporting.contract.AtfReportPorts.Request startRequest(String title, String method, String path){return new Request(delegate.startRequest(title,method,path));}
        public boolean supportsDetails(){return true;}
        public void detail(String name,String value){delegate.detail(name,value);} public void complete(atf.core.reporting.contract.AtfReportStatus s){delegate.complete(status(s));} public void close(){delegate.close();}
    }
    private static final class Request implements atf.core.reporting.contract.AtfReportPorts.Request {
        private final AtfReportPorts.Request delegate; Request(AtfReportPorts.Request d){delegate=d;}
        public void detail(String n,String v){delegate.detail(n,v);} public void json(String n,String v){delegate.json(n,v);} public void screenshot(String n,String v){delegate.screenshot(n,v);}
        public void uiActions(Collection<atf.core.ui.AtfUiActionRecord> a){delegate.uiActions(a==null?List.of():a.stream().map(CoreReportPortsAdapter::action).toList());}
        public void execution(ApiExecutionResult e){delegate.execution(ReportModelAdapters.execution(e));}
        public atf.core.reporting.contract.AtfReportPorts.Validation startValidation(String title){return new Validation(delegate.startValidation(title));}
        public void complete(atf.core.reporting.contract.AtfReportStatus s){delegate.complete(status(s));} public void close(){delegate.close();}
    }
    private static final class Validation implements atf.core.reporting.contract.AtfReportPorts.Validation {
        private final AtfReportPorts.Validation delegate; Validation(AtfReportPorts.Validation d){delegate=d;}
        public void assertions(Collection<atf.core.reporting.contract.AtfAssertion> a){delegate.assertions(a==null?List.of():a.stream().map(CoreReportPortsAdapter::assertion).toList());}
        public void uiActions(Collection<atf.core.ui.AtfUiActionRecord> a){delegate.uiActions(a==null?List.of():a.stream().map(CoreReportPortsAdapter::action).toList());}
        public void screenshot(String n,String v){delegate.screenshot(n,v);} public void mediaScreenshot(String n,String v){delegate.mediaScreenshot(n,v);}
        public void complete(atf.core.reporting.contract.AtfReportStatus s){delegate.complete(status(s));} public void close(){delegate.close();}
    }
}
