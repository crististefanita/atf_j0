package atf.ui.tests.extent.e2e;
import atf.testkit.AtfE2eTest;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;

import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/** Single IntelliJ entry point for the browser-backed Extent reporting E2E contract. */
@Suite
@SelectPackages("atf.ui.tests.extent.e2e")
@IncludeTags("e2e")
@ExcludeClassNamePatterns(".*Suite")
@AtfE2eTest
@AtfTestChannel(TestChannel.UI)
public class ExtentReportingE2eSuite { }
