package atf.reporting.standalone.resources;

import atf.reporting.artifact.resources.ReportArtifactResources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Parsed DOM references, rather than string canaries or a dependency on ATF Core/Kit. */
class ReportingStandaloneResourceAutonomyTest {
    @ParameterizedTest @ValueSource(strings = {
            "<a href='file:///missing/a.xml'>Evidence</a>", "<a href='C:\\missing\\a.xml'>Evidence</a>",
            "<a href='//host/share/a.xml'>Evidence</a>", "<a href='a.xml'>Evidence</a>",
            "<a href='https://example.org/a.json?token=secret'>Evidence</a>",
            "<div data-atf-node-role='observation'><a href='https://example.org/evidence'>Artifact</a></div>",
            "<script src='missing.js'></script>", "<link rel='stylesheet' href='missing.css'>",
            "<img src='https://example.org/a.png'>", "<img src='file:///missing/a.png'>",
            "<img srcset='data:image/png;base64,AAAA 1x, a.png 2x'>",
            "<video poster='missing.png'></video>", "<audio src='missing.wav'></audio>",
            "<svg><use href='missing.svg#icon'/></svg>", "<object data='missing.xml'></object>",
            "<style>@import 'missing.css';</style>", "<style>p{background:url(missing.png)}</style>",
            "<div style='background:u\\72l(missing.png)'></div>", "<base href='https://example.org/'>",
            "<iframe srcdoc=\"&lt;img src='missing.png'&gt;\"></iframe>" })
    void operativeDependenciesAreRefused(String html) {
        var result = ReportArtifactResources.inspect(html);
        assertFalse(result.isAutonomous(), html);
        assertFalse(result.diagnostic().passed());
        assertTrue(result.diagnostic().evidence().contains("["));
        assertFalse(result.diagnostic().evidence().contains("token=secret"));
    }

    @ParameterizedTest @ValueSource(strings = {
            "<pre>file:///original/evidence.xml C:\\work\\evidence.xml</pre>",
            "<pre>&lt;img src='missing.png'&gt;</pre>",
            "<script>var literal=\"<a href='file:///evidence.xml'>x</a>\";</script>",
            "<!-- <img src='missing.png'> --><a href='https://example.org/guide'>Guide</a>",
            "<style>/* url(missing.png) */ p:after{content:'url(missing.png)'}</style>",
            "<img srcset='data:image/png;base64,AAAA 1x, data:image/png;base64,BBBB 2x'>",
            "<svg><clipPath id='clip'/></svg><div style='clip-path:url(#clip)'></div>",
            "<script src='data:text/javascript,void(0)'></script><link rel='stylesheet' href='data:text/css,p{}'>" })
    void embeddedResourcesAndLiteralPathsRemainValid(String html) { assertTrue(ReportArtifactResources.inspect(html).isAutonomous(), html); }

    @Test void canonicalScreenshotMustBeUniqueAndEmbedded() {
        String link = "<a data-atf-screenshot-ref='one' href='#atf-screenshot-one'>Same frame</a>";
        String target = "<a id='atf-screenshot-one' data-atf-screenshot-ref='one' href='data:image/png;base64,AAAA'>Frame</a>";
        assertTrue(ReportArtifactResources.inspect(link + target).isAutonomous());
        assertFalse(ReportArtifactResources.inspect(link).isAutonomous());
        assertFalse(ReportArtifactResources.inspect(link + target + target).isAutonomous());
        assertFalse(ReportArtifactResources.inspect(link + target.replace("data:image/png;base64,AAAA", "frame.png")).isAutonomous());
    }
}
