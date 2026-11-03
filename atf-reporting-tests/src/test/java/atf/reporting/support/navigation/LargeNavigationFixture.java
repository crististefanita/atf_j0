package atf.reporting.support.navigation;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.*;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.*;
import atf.reporting.support.TestReportPaths;
import atf.reporting.support.screenshot.ScreenshotViewerFixture;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.ui.UiExtentReportSession;
import java.nio.file.Path;
import java.util.*;

/** Neutral dense evidence: no Android, external app or transport dependencies. */
public final class LargeNavigationFixture {
    private LargeNavigationFixture() { }
    public static Path publish() throws Exception {
        Path file=TestReportPaths.report("large-navigation.html");
        String png=ScreenshotViewerFixture.png("portrait");
        try(var session=new ExtentReportSession(file,new ReportingSettings(240,32768))) {
            for(int owner=0;owner<4;owner++) {
                var raw=session.startScenario("DenseOwner"+owner,"atf.fixture","Captured evidence "+owner,List.of("component","synthetic"));
                try(var scenario=UiExtentReportSession.adaptScenario(raw,
                        ReportEvidenceProfile.forScreenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT),"Synthetic capture")) {
                    try(var phase=raw.startPhase("TEST BODY")) {
                    for(int group=0;group<150;group++) {
                        var rows=new ArrayList<AtfAssertion>();
                        for(int row=0;row<20;row++)rows.add(new AtfAssertion("Captured "+owner+" / "+group+" / "+row,"-"+row,"-"+row,true));
                        try(var step=phase.startAssertionStep("Comparisons "+group)){step.assertions(rows);step.complete(AtfReportStatus.PASS);}
                        var fields=new ArrayList<ReportField>();
                        for(int field=0;field<8;field++)fields.add(new ReportField("Input "+field,"owner "+owner+", group "+group+", input "+field));
                        phase.observation(new ReportObservation("Captured inputs "+group,fields,List.of(),List.of()));
                    }
                    phase.complete(AtfReportStatus.PASS);
                    }
                    try(var phase=scenario.startPhase("Captured media")) {
                    phase.uiValidation("Owner "+owner+" / final frame","Complete original portrait",png,
                            List.of(new AtfAssertion("Final checkpoint identity","owner "+owner,"owner "+owner,true)),AtfReportStatus.PASS);
                    phase.complete(AtfReportStatus.PASS);
                    }
                    scenario.complete(AtfReportStatus.PASS);
                }
            }
        }
        return file;
    }
}
