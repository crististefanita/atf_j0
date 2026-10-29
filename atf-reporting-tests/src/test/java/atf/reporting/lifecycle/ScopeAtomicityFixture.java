package atf.reporting.lifecycle;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.security.SensitiveDataRedactor;
import atf.reporting.semantic.AtfReportSemanticSnapshot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** Public-port fixture; production lifecycle decisions are never reproduced in test support. */
final class ScopeAtomicityFixture implements AutoCloseable {
    enum Backend { EXTENT, HTML }
    final AtfReportPorts.Session session;
    final AtfReportPorts.Scenario scenario;
    final AtfReportPorts.Phase phase;
    final AtfReportPorts.Request request;
    final Path file;

    ScopeAtomicityFixture(Backend backend, String name) throws Exception {
        this(backend, name, AtfConsoleSink.noOp(), false);
    }

    ScopeAtomicityFixture(Backend backend, String name, AtfConsoleSink console, boolean blocked) throws Exception {
        Path directory = Files.createDirectories(Path.of("target", "scope-atomicity-evidence", UUID.randomUUID().toString()));
        file = directory.resolve(backend.name().toLowerCase() + "-" + name + ".html");
        if (blocked) Files.createDirectory(file);
        session = backend == Backend.HTML
                ? new HtmlReportSession(file, ReportingSettings.defaults(), console)
                : new ExtentReportSession(file, ReportingSettings.defaults(), new SensitiveDataRedactor(), console);
        scenario = session.startScenario("ScopeAtomicityFixture", "atf.reporting.lifecycle", name, List.of("component", "scope-atomicity"));
        phase = scenario.startPhase("TEST BODY");
        request = phase.startRequest("Audited request", "GET", "https://scope.fixture.test/read");
    }

    AtfReportSemanticSnapshot snapshot() {
        return session instanceof HtmlReportSession html ? html.semanticSnapshot()
                : ((ExtentReportSession) session).semanticSnapshot();
    }

    void finishRequest(AtfReportStatus status) {
        request.complete(status);
        finishParents();
    }

    void finishParents() {
        phase.complete(AtfReportStatus.PASS);
        scenario.complete(AtfReportStatus.PASS);
        session.close();
    }

    AtfReportStatus status() {
        return snapshot().classes().getFirst().scenarios().getFirst().status();
    }

    @Override public void close() { session.close(); }
}

