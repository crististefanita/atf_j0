package atf.api.tests.governance.consumers;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class ConsumerAuditSourceGraphTest {
    private static final Map<String, String> ANNOTATIONS = Map.of(
            "fixture.Alias", "package fixture;\n@ShopComponentTest\npublic @interface Alias {}",
            "fixture.ShopComponentTest", "package fixture;\n@AtfComponentTest\npublic @interface ShopComponentTest {}");
    @Test void documentationCannotExcludeAnExecutableClassFromTheAudit() {
        var sources = Map.of("fixture.Component", """
                package fixture;
                // @Suite abstract class Example {}
                /* @interface Example {} */
                @Alias
                class Component {
                    String example = "@Suite @interface abstract class Example";
                    @Suite static class NestedSuite { }
                    abstract static class NestedAbstract { }
                    @interface NestedAnnotation { }
                    @Test void test() { org.junit.jupiter.api.Assertions.assertTrue(true); }
                }
                """);
        assertFalse(ConsumerAuditSourceGraph.violations(sources, ANNOTATIONS).isEmpty());
        var fakeMarker = Map.of("fixture.Component", """
                package fixture;
                // @Alias
                @AtfComponentTest
                class Component { @Test void test() {} }
                """);
        assertTrue(ConsumerAuditSourceGraph.violations(fakeMarker, ANNOTATIONS).stream()
                .anyMatch(violation -> violation.contains("reporting marker")));
    }
    @Test void rejectsRawAssertionsThroughAnAliasAndAnImportedHelper() {
        var sources = Map.of("fixture.Component", """
                package fixture;
                @Alias
                class Component {
                    @Test void check() { Helper.check(); }
                }
                """, "fixture.Helper", "package fixture; class Helper { void check() { org.junit.jupiter.api.Assertions.assertTrue(true); } }");
        assertTrue(ConsumerAuditSourceGraph.violations(sources, ANNOTATIONS).stream()
                .anyMatch(violation -> violation.contains("fixture.Helper")));
    }
    @Test void followsInheritedComponentContractsAndIgnoresCommentExamples() {
        var sources = Map.of("fixture.Child", "package fixture; class Child extends Parent {}",
                "fixture.Parent", """
                package fixture;
                @Alias
                abstract class Parent {
                    // org.junit.jupiter.api.Assertions is not a call
                    @Test void check() { atf.testkit.assertions.AuditedAssertions.assertTrue(true); }
                }
                """);
        assertTrue(ConsumerAuditSourceGraph.violations(sources, ANNOTATIONS).isEmpty());
        var raw = Map.of("fixture.Child", sources.get("fixture.Child"), "fixture.Parent",
                sources.get("fixture.Parent").replace("atf.testkit.assertions.AuditedAssertions", "org.junit.jupiter.api.Assertions"));
        assertFalse(ConsumerAuditSourceGraph.violations(raw, ANNOTATIONS).isEmpty());
    }
    @Test void rawComponentMarkerStillRequiresItsConsumerReportingContract() {
        var source = Map.of("fixture.Bad", "package fixture;\n@AtfComponentTest\nclass Bad { @Test void test() {} }");
        assertTrue(ConsumerAuditSourceGraph.violations(source, Map.of()).stream()
                .anyMatch(violation -> violation.contains("reporting marker")));
    }

    @Test void followsImportedStaticHelperCalls() {
        var sources = Map.of("fixture.Component", """
                package fixture;
                import static fixture.Helper.check;
                @Alias
                class Component {
                    @Test void test() { check(); }
                }
                """, "fixture.Helper", "package fixture; class Helper { static void check() { org.junit.jupiter.api.Assertions.assertTrue(true); } }");
        assertFalse(ConsumerAuditSourceGraph.violations(sources, ANNOTATIONS).isEmpty());
    }

    @Test void followsHelpersImportedFromAnotherPackageIncludingWildcards() {
        for (String imported : java.util.List.of("other.Helper", "other.*")) {
            var sources = Map.of("fixture.Component", "package fixture;\nimport " + imported + ";\n"
                    + "@Alias class Component { @Test void test() { Helper.check(); } }",
                    "other.Helper", "package other; class Helper { static void check() { org.junit.jupiter.api.Assertions.assertTrue(true); } }");
            assertTrue(ConsumerAuditSourceGraph.violations(sources, ANNOTATIONS).stream()
                    .anyMatch(violation -> violation.contains("other.Helper")), imported);
        }
    }
}
