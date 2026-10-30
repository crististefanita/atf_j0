package atf.reporting.configuration;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class ReportingNumericConfigurationTest {
    @TempDir Path directory;
    @ParameterizedTest @ValueSource(strings={"", "null", "{}", "reporting:\n  extent: null"})
    void absentConfigurationValuesKeepTheDocumentedDefaults(String yaml) throws Exception {
        var config=directory.resolve("defaults.yaml");Files.writeString(config,yaml);
        assertEquals(ReportingSettings.defaults(),AtfReportConfigurationLoader.load(config).reportingSettings());
    }
    @ParameterizedTest @ValueSource(strings={"1.9","1.0","2147483648","'240'","true","-1","[]","{}"})
    void previewLimitsRejectNonIntegralOrOutOfRangeYamlValues(String value) throws Exception {
        for(String key:new String[]{"assertionMaxDisplayCharacters","bodyMaxDisplayCharacters"}) {
            var config=directory.resolve(key+".yaml");
            Files.writeString(config,"reporting:\n  "+key+": "+value+"\n");
            var failure=assertThrows(IllegalStateException.class,()->AtfReportConfigurationLoader.load(config));
            assertTrue(failure.getMessage().contains("reporting."+key));
        }
    }
    @ParameterizedTest @ValueSource(strings={"reporting: []","reporting: false","reporting:\n  extent: []","reporting:\n  extent: 1"})
    void scalarOrListSectionsCannotSilentlyRestoreDefaults(String yaml) throws Exception {
        var config=directory.resolve("invalid-section.yaml");Files.writeString(config,yaml);
        var failure=assertThrows(IllegalStateException.class,()->AtfReportConfigurationLoader.load(config));
        assertTrue(failure.getMessage().contains("reporting"));
    }
    @Test void zeroDisablesOnlyTheBodyBudgetAndValidOverridesRemainExact() throws Exception {
        var config=directory.resolve("valid.yaml");
        Files.writeString(config,"reporting:\n  assertionMaxDisplayCharacters: 240\n  bodyMaxDisplayCharacters: 0\n");
        var settings=AtfReportConfigurationLoader.load(config).reportingSettings();
        assertEquals(new ReportingSettings(240,0),settings);
        Files.writeString(config,"reporting:\n  assertionMaxDisplayCharacters: 0\n");
        assertThrows(IllegalStateException.class,()->AtfReportConfigurationLoader.load(config));
    }
    @Test void misspelledOwnedKeysFailButUnrelatedModuleSectionsStayOutsideTheSchema() throws Exception {
        var config=directory.resolve("typo.yaml");
        Files.writeString(config,"reporting:\n  assertionMaxDisplayCharacter: 999\n");
        var failure=assertThrows(IllegalStateException.class,()->AtfReportConfigurationLoader.load(config));
        assertTrue(failure.getMessage().contains("reporting.assertionMaxDisplayCharacter"));
        Files.writeString(config,"api:\n  consumerOption: true\nreporting:\n  console:\n    enabled: true\n");
        assertEquals(240,AtfReportConfigurationLoader.load(config).reportingSettings().assertionMaxDisplayCharacters());
    }
}
