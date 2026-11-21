package atf.api.tests.governance.maintenance;

import atf.api.tests.governance.WholeAtfGovernanceSuiteTest;
import atf.testkit.AtfIntegrationTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/** Runs local J0 governance and read-only acceptance of the explicitly selected J1 workspace. */
@AtfIntegrationTest
@Suite
@SelectClasses({WholeAtfGovernanceSuiteTest.class, AtfMaintenanceWorkspacePairGovernanceTest.class})
public class AtfWorkspaceMaintenanceSuite { }
