package atf.reporting.extent.session;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.reporting.security.SensitiveDataRedactor;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.html.HtmlReportSession;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Constructor refusal cannot acquire a workspace or change an existing artifact. */
@AtfComponentTest
class ExtentReportConstructionComponentTest {
    @TempDir Path directory;

    @Test void invalidDisclosureIsRejectedBeforeCreatingTheOutputParent() {
        Path output=directory.resolve("absent/report.html");
        var failure=assertThrows(NullPointerException.class,()->new ExtentReportSession(output,
                ReportingSettings.defaults(),new SensitiveDataRedactor(),AtfConsoleSink.noOp(),Map.of(),null));
        assertEquals("detailsMode",failure.getMessage());
        assertFalse(Files.exists(output.getParent()));
    }

    @Test void invalidLifecycleProbePreservesThePreviousArtifactAndAcquiresNothing() throws Exception {
        Path output=directory.resolve("report.html");Files.writeString(output,"previous artifact");
        assertThrows(NullPointerException.class,()->new ExtentReportSession(output,
                ReportingSettings.defaults(),new SensitiveDataRedactor(),AtfConsoleSink.noOp(),Map.of(),
                ReportUiActionDetailsMode.INCLUDED_COLLAPSED,null));
        assertEquals("previous artifact",Files.readString(output));
        try(var files=Files.list(directory)){assertEquals(List.of(output),files.toList());}
    }

    @Test void metadataFailurePreservesItsExactCauseBeforeAcquisition() throws Exception {
        Path output=directory.resolve("report.html");Files.writeString(output,"previous artifact");
        var cause=new IllegalStateException("metadata refused");
        Map<String,String> metadata=new AbstractMap<>() {
            @Override public Set<Entry<String,String>> entrySet(){throw cause;}
        };
        assertSame(cause,assertThrows(IllegalStateException.class,()->new ExtentReportSession(output,
                ReportingSettings.defaults(),new SensitiveDataRedactor(),AtfConsoleSink.noOp(),metadata)));
        assertEquals("previous artifact",Files.readString(output));
        try(var files=Files.list(directory)){assertEquals(List.of(output),files.toList());}
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void rendererFlushAndRepeatedCloseRespectTheirDeclaredPublicationBoundary(boolean fallback) throws Exception {
        Path output=directory.resolve("report.html");Files.writeString(output,"previous artifact");
        var session=open(output,fallback);
        try(session){
            var scenario=session.startScenario("LifecycleCases","completed case",List.of("component"));
            scenario.startPhase("TEST BODY").complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
            session.flush();
            assertEquals(fallback,Files.readString(output).contains("completed case"));
            if(!fallback)assertEquals("previous artifact",Files.readString(output));
        }
        byte[] published=Files.readAllBytes(output);
        assertTrue(Files.readString(output).contains("completed case"));
        session.close();
        assertArrayEquals(published,Files.readAllBytes(output));
        try(var files=Files.list(directory)){assertEquals(List.of(output),files.toList());}
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void serializedForeignThreadAccessMatchesTheRendererContract(boolean fallback) throws Exception {
        try(var session=open(directory.resolve("report.html"),fallback);
            var executor=java.util.concurrent.Executors.newSingleThreadExecutor()){
            var operation=executor.submit(session::flush);
            if(fallback)operation.get(10,java.util.concurrent.TimeUnit.SECONDS);
            else {
                var refusal=assertThrows(java.util.concurrent.ExecutionException.class,
                        ()->operation.get(10,java.util.concurrent.TimeUnit.SECONDS));
                assertInstanceOf(IllegalStateException.class,refusal.getCause());
                assertTrue(refusal.getCause().getMessage().contains("thread-confined"));
            }
        }
    }

    private static AtfReportPorts.Session open(Path path,boolean fallback){
        return fallback?new HtmlReportSession(path):new ExtentReportSession(path);
    }
}
