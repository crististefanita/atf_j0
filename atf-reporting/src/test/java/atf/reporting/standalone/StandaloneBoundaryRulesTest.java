package atf.reporting.standalone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class StandaloneBoundaryRulesTest {
    @ParameterizedTest @ValueSource(strings={"atf.core.http.Client","atf.ui.network.Observation","atf.shop.config.Configuration",
            "atf.autoex.runtime.Run","atf.testkit.Scope","atf.console.Console"})
    void allExternalAtfNamespacesAreRejectedIncludingStaticImports(String type) {
        assertTrue(StandaloneBoundaryRules.externalAtfImport("import "+type+";"));
        assertTrue(StandaloneBoundaryRules.externalAtfImport("import static "+type+".value;"));
        assertFalse(StandaloneBoundaryRules.externalAtfImport("// import "+type+";"));
    }
    @Test void reportingImportsAndVendorDependenciesAreIndependentOfTheReactor() throws Exception {
        assertFalse(StandaloneBoundaryRules.externalAtfImport("import atf.reporting.contract.AtfReportStatus;"));
        assertFalse(StandaloneBoundaryRules.hasAtfDependency(pom("<dependencies><dependency><groupId>vendor</groupId><artifactId>render</artifactId></dependency></dependencies>")));
        assertFalse(StandaloneBoundaryRules.hasParent(pom("<!-- <parent>example</parent> -->")));
    }
    @Test void parentAndAtfDependenciesCannotHideBehindWhitespaceOrNamespacePrefixes() throws Exception {
        assertTrue(StandaloneBoundaryRules.hasAtfDependency(pom("<dependencies><dependency><groupId> atf.api </groupId><artifactId>atf-ui</artifactId></dependency></dependencies>")));
        assertTrue(StandaloneBoundaryRules.hasParent(pom("<parent><artifactId>atf</artifactId></parent>")));
        assertTrue(StandaloneBoundaryRules.hasParent("<m:project xmlns:m='urn:maven'><m:parent/></m:project>"));
    }
    private static String pom(String content) { return "<project xmlns='urn:maven'>"+content+"</project>"; }
}
