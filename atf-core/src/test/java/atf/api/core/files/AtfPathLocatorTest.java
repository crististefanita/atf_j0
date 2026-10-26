package atf.api.core.files;

import atf.api.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class AtfPathLocatorTest {
    @TempDir
    Path temp;

    @Test
    void findsConfiguredFileFromNestedWorkingDirectory() throws Exception {
        Path project = Files.createDirectories(temp.resolve("project"));
        Path nested = Files.createDirectories(project.resolve("module/target"));
        Path config = Files.createFile(project.resolve("atf.yaml"));

        assertEquals(config, AtfPathLocator.firstRegularFileUpwards(
                nested, List.of("atf.yaml", "atf.yml")));
    }

    @Test
    void resolvesTheSourceRootRatherThanTheRequiredPackageDirectory() throws Exception {
        Path project = Files.createDirectories(temp.resolve("project"));
        Path sourceRoot = Files.createDirectories(project.resolve("atf-reporting/src/test/java"));
        Files.createDirectories(sourceRoot.resolve("atf/ui/tests"));

        assertEquals(sourceRoot, AtfPathLocator.locateSourceRoot(
                project.resolve("atf-reporting"), Path.of("atf", "ui", "tests"),
                List.of(Path.of("src", "test", "java"),
                        Path.of("atf-reporting", "src", "test", "java"))));
    }

    @Test
    void rejectsAbsoluteSourceRootCandidates() {
        assertThrows(IllegalArgumentException.class, () -> AtfPathLocator.locateSourceRoot(
                temp, Path.of("atf"), List.of(temp.resolve("src/test/java"))));
    }

    @Test
    void locatesTheAtfTestRootWithoutDuplicatingModuleCandidates() throws Exception {
        Path project = Files.createDirectories(temp.resolve("project"));
        Path sourceRoot = Files.createDirectories(project.resolve("atf-reporting/src/test/java"));
        Files.createDirectories(sourceRoot.resolve("atf/ui/tests"));

        assertEquals(sourceRoot, AtfPathLocator.locateAtfTestSourceRoot(
                project.resolve("atf-reporting"), Path.of("atf", "ui", "tests")));
    }

    @Test
    void locatesProductionSourceFilesAcrossNestedModuleAndReactorLayouts() throws Exception {
        Path project = Files.createDirectories(temp.resolve("project"));
        Path module = Files.createDirectories(project.resolve("atf-reporting/src/main/java"));
        Files.createDirectories(module.resolve("atf/api"));
        Path source = Files.createFile(module.resolve("atf/api/Renderer.java"));

        assertEquals(source, AtfPathLocator.locateSourceFile(
                project.resolve("atf-reporting/target/classes"), Path.of("atf/api/Renderer.java"),
                List.of(Path.of("src/main/java"), Path.of("atf-reporting/src/main/java"))));
    }

    @Test
    void sourceFileLookupRejectsTraversalAndMissingFiles() throws Exception {
        Path project = Files.createDirectories(temp.resolve("project"));
        Files.createDirectories(project.resolve("module/src/main/java/atf/api"));

        assertThrows(IllegalArgumentException.class, () -> AtfPathLocator.locateSourceFile(
                project, Path.of("../outside/Secret.java"), List.of(Path.of("module/src/main/java"))));
        assertThrows(IllegalStateException.class, () -> AtfPathLocator.locateSourceFile(
                project, Path.of("atf/api/Missing.java"), List.of(Path.of("module/src/main/java"))));
    }

    @Test
    void locatesModuleRootFromNestedWorkingDirectory() throws Exception {
        Path project = Files.createDirectories(temp.resolve("project"));
        Path module = Files.createDirectories(project.resolve("atf-test-practice-shop/src/main/java"));
        Files.createDirectories(module.resolve("atf/shop"));

        assertEquals(project.resolve("atf-test-practice-shop"), AtfPathLocator.locateModuleRoot(
                module.resolve("atf/shop"), Path.of("atf-test-practice-shop"),
                Path.of("src/main/java/atf/shop")));
    }

    @Test
    void rejectsUnknownModuleRoot() throws Exception {
        Path project = Files.createDirectories(temp.resolve("project"));

        assertThrows(IllegalStateException.class, () -> AtfPathLocator.locateModuleRoot(
                project, Path.of("missing-module"), Path.of("src/main/java")));
    }

    @Test
    void resolvesConventionalAndLegacyModuleSourceRoots() throws Exception {
        Path project = Files.createDirectories(temp.resolve("project"));
        Path conventional = Files.createDirectories(project.resolve("reporting/src/main/java"));
        Files.createDirectories(project.resolve("tests/src/test"));

        assertEquals(conventional, AtfPathLocator.resolveModuleSourceRoot(
                project, "reporting", false));
        assertEquals(project.resolve("tests/src/test"), AtfPathLocator.resolveModuleSourceRoot(
                project, "tests", true));
    }
}
