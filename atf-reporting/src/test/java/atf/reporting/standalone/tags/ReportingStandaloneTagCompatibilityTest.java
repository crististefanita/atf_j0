package atf.reporting.standalone.tags;

import atf.reporting.model.ReportTagNormalizer;
import atf.reporting.semantic.tags.ReportTags;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Independent expected values exercise the old binary name and its semantic owner. */
class ReportingStandaloneTagCompatibilityTest {
    @Test void legacyAndSemanticEntryPointsPreserveValuesOrderAndImmutableResults() {
        var input = new ArrayList<>(Arrays.asList(" API ", null, "", "UI", "api", " Mock "));
        var original = new ArrayList<>(input);
        for (var actual : List.of(ReportTags.normalize(input), ReportTagNormalizer.normalize(input))) {
            assertEquals(List.of("api", "ui", "mock"), actual);
            assertThrows(UnsupportedOperationException.class, () -> actual.add("other"));
        }
        assertEquals(original, input, "normalization never rewrites the caller's collection");
    }
    @Test void nullAndBlankTagsRemainEmptyAtBothEntryPoints() {
        assertEquals(List.of(), ReportTags.normalize(null));
        assertEquals(List.of(), ReportTagNormalizer.normalize(null));
        assertEquals(List.of(), ReportTagNormalizer.normalize(Arrays.asList(null, " ", "")));
    }
    @Test void compatibilitySurfaceIsOnePublicStaticMethodWithNoRemovalCommitment() throws Exception {
        var type = ReportTagNormalizer.class;
        var annotation = type.getAnnotation(Deprecated.class);
        assertNotNull(annotation);
        assertFalse(annotation.forRemoval());
        assertEquals(1, type.getDeclaredMethods().length);
        var method = type.getDeclaredMethod("normalize", Collection.class);
        assertTrue(Modifier.isPublic(method.getModifiers()));
        assertTrue(Modifier.isStatic(method.getModifiers()));
        assertEquals(List.class, method.getReturnType());
        assertTrue(Arrays.stream(type.getDeclaredConstructors()).allMatch(c -> Modifier.isPrivate(c.getModifiers())));
    }
}
