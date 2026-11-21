package atf.api.tests.governance.maintenance;

import atf.testkit.AtfIntegrationTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/** Explicit read-only gate for one producer/consumer workspace pair. */
@AtfIntegrationTest
@Suite
@SelectClasses(AtfMaintenanceWorkspacePairGovernanceTest.class)
public class AtfMaintenanceWorkspacePairSuite { }
