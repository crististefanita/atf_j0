package atf.ui.tests.extent.component.provenance;

import atf.testkit.AtfComponentTest;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/** Focused source/JAR acceptance for neutral UI attribution and captured failure navigation. */
@Suite
@SelectPackages({"atf.reporting.ui.contract", "atf.ui.tests.extent.component.provenance"})
@IncludeClassNamePatterns(".*\\.(UiActionAttributionContractTest|UiFailureCatalogueContractTest|UiActionAttributionComponentTest)")
@AtfComponentTest
public class UiActionAttributionComponentSuite { }
