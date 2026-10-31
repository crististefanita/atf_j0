package atf.reporting.support.navigation;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class FailureLinkIdentityTest {
    @Test void additionalParametersPreserveTheExactScenarioAndOrdinal(){
        var link=FailureLinkIdentity.parse("#test-id=7&failure-ordinal=2&failure-kind=assertion&extra=retained");
        assertTrue(link.failureOf("test-id=7",2));
        assertTrue(link.hasParameter("extra","retained"));
    }
    @Test void serializedHtmlAndReloadEncodingKeepTheSameIdentity(){
        var serialized=FailureLinkIdentity.parse("#test-id=7&amp;failure-ordinal=2");
        var reloaded=FailureLinkIdentity.parse("https://example.test/report#test-id%3D7&failure-ordinal=2");
        assertTrue(serialized.failureOf("test-id=7",2));
        assertTrue(reloaded.failureOf("test-id=7",2));
    }
    @Test void aForeignScenarioOrOrdinalCannotSatisfyTheOracle(){
        var link=FailureLinkIdentity.parse("#test-id=7&failure-ordinal=2");
        assertFalse(link.failureOf("test-id=70",2));
        assertFalse(link.failureOf("test-id=7",1));
        assertFalse(link.failureOf("test-id=7",0));
    }
    @Test void nativeScenarioIndexDoesNotDependOnParameterPosition(){
        var link=FailureLinkIdentity.parse("#test-id=7&failure-kind=execution&scenario-index=3&failure-ordinal=2");
        assertTrue(link.hasParameter("scenario-index","3"));
        assertFalse(link.hasParameter("scenario-index","30"));
    }
    @Test void duplicateParametersAreRejectedInsteadOfSelectingOne(){
        assertThrows(IllegalArgumentException.class,()->FailureLinkIdentity.parse("#test-id=7&scenario-index=3&scenario-index=4"));
    }
    @Test void missingFragmentsAndEmptyParametersAreRejected(){
        assertThrows(IllegalArgumentException.class,()->FailureLinkIdentity.parse("report.html"));
        assertThrows(IllegalArgumentException.class,()->FailureLinkIdentity.parse("#test-id=7&=value"));
    }
}
