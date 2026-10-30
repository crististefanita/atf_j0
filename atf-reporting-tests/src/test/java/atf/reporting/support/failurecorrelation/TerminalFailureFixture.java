package atf.reporting.support.failurecorrelation;

import atf.reporting.api.*;
import atf.reporting.contract.*;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.support.TestReportPaths;
import java.nio.file.Path;
import java.util.*;

/** A terminal request between successful neighbours; the expected rejection is a separate logical verdict. */
public final class TerminalFailureFixture {
    public enum Case { TERMINAL, ZERO_ATTEMPTS, NO_TERMINATION, WITH_ASSERTION, EXPECTED_PASS, EXPECTED_PASS_WITH_FAILED_SIBLING }
    public static final String SECRET = "terminal-index-private-canary";
    private TerminalFailureFixture() { }

    public static Path publish(boolean fallback, Case kind) {
        Path output=TestReportPaths.report("terminal-index-"+kind+"-"+fallback+"-"+UUID.randomUUID()+".html");
        try(AtfReportPorts.Session session=fallback?new HtmlReportSession(output):new ExtentReportSession(output)) {
            try(var sibling=session.startScenario("TerminalFixture","Passing neighbour",List.of("component"));
                var phase=sibling.startPhase("TEST BODY")) { phase.complete(AtfReportStatus.PASS);sibling.complete(AtfReportStatus.PASS); }
            try(var scenario=session.startScenario("TerminalFixture",kind.name(),List.of("component"));
                var phase=scenario.startPhase("TEST BODY")) {
                boolean expected=kind==Case.EXPECTED_PASS||kind==Case.EXPECTED_PASS_WITH_FAILED_SIBLING;
                request(phase,"/before",false,false,false);
                request(phase,"/terminal",true,expected,kind==Case.WITH_ASSERTION,kind);
                request(phase,"/after",false,false,kind==Case.EXPECTED_PASS_WITH_FAILED_SIBLING);
                phase.complete(expected?AtfReportStatus.PASS:AtfReportStatus.FAIL);
                scenario.complete(expected?AtfReportStatus.PASS:AtfReportStatus.FAIL);
            }
        }
        return output;
    }
    private static void request(AtfReportPorts.Phase phase,String path,boolean terminal,boolean expected,boolean assertion) {
        request(phase,path,terminal,expected,assertion,Case.TERMINAL);
    }
    private static void request(AtfReportPorts.Phase phase,String path,boolean terminal,boolean expected,boolean assertion,Case kind) {
        var request=ReportRequest.builder("GET",path).build();
        var response=terminal?null:new ReportResponse(200,"{}",Map.of(),null,"https://fixture.invalid"+path);
        var execution=new ReportExecutionEvidence(response,kind==Case.ZERO_ATTEMPTS?0:1,null,List.of(),
                terminal&&kind!=Case.ZERO_ATTEMPTS?List.of(new ReportAttemptRecord(1,-1,null,"FAILURE","terminal transport canary")):List.of(),
                request,ReportTransportProvenance.LIVE_HTTP);
        if(terminal&&kind!=Case.NO_TERMINATION) execution=execution.withTermination("IO_ERROR","terminal transport canary; token="+SECRET);
        try(var node=phase.startRequest("Observed exchange","GET",path)) {
            node.execution(execution);
            if(assertion) try(var validation=node.startValidation("Independent business comparison")) {
                validation.assertions(List.of(new AtfAssertion("Independent comparison","expected","different",false)));
                validation.complete(AtfReportStatus.FAIL);
            }
            node.complete(terminal&&!expected?AtfReportStatus.FAIL:AtfReportStatus.PASS);
        }
    }
}
