```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SpecVersionDetector#detectVersion(JsonNode, Path, SpecVersion.VersionFlag)}.
 *
 * <p>These tests cover the branching logic inside {@code detectVersion} by mocking the two
 * helper methods {@code detectOptionalVersion(JsonNode)} and {@code detectVersionFromPath(Path)}.
 * The real implementations are not required for the branch‑coverage tests.</p>
 */
@ExtendWith(MockitoExtension.class)
class SpecVersionDetector_detectVersion_Test {

    /** Shared mapper for parsing JSON strings used in the test data. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Default version used when detection fails – also used as a convenient constant
     *  in the {@code @MethodSource} data. */
    private static final SpecVersion.VersionFlag V4 = SpecVersion.VersionFlag.V4;
    private static final SpecVersion.VersionFlag V6 = SpecVersion.VersionFlag.V6;
    private static final SpecVersion.VersionFlag V7 = SpecVersion.VersionFlag.V7;
    private static final SpecVersion.VersionFlag V202012 = SpecVersion.VersionFlag.V202012;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* --------------------------------------------------------------------- */
    /* Existing explicit tests (kept unchanged)                             */
    /* --------------------------------------------------------------------- */

    /**
     * When the JSON node contains a recognizable {@code $schema} URI,
     * {@link SpecVersionDetector#detectVersion(JsonNode, Path, SpecVersion.VersionFlag)} must
     * return the version derived from the JSON, even if the supplied path also
     * hints at a different version.
     */
    @Test
    void detectVersion_jsonNodeVersionOverridesPath(@TempDir Path tempDir) throws IOException {
        String json = """
                {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "type": "object"
                }
                """;
        JsonNode node = MAPPER.readTree(json);

        Path path = tempDir.resolve("draft4-schema.json");
        Files.writeString(path, "{}");

        SpecVersion.VersionFlag result = SpecVersionDetector.detectVersion(node, path, SpecVersion.VersionFlag.V6);
        assertEquals(SpecVersion.VersionFlag.V7, result);
    }

    @Test
    void detectVersion_pathVersionUsedWhenJsonMissing(@TempDir Path tempDir) throws IOException {
        String json = """
                {
                  "type": "object"
                }
                """;
        JsonNode node = MAPPER.readTree(json);

        Path path = tempDir.resolve("draft2020-12-schema.json");
        Files.writeString(path, "{}");

        SpecVersion.VersionFlag result = SpecVersionDetector.detectVersion(node, path, SpecVersion.VersionFlag.V6);
        assertEquals(SpecVersion.VersionFlag.V202012, result);
    }

    @Test
    void detectVersion_returnsDefaultWhenNoVersionDetected(@TempDir Path tempDir) throws IOException {
        String json = """
                {
                  "type": "object"
                }
                """;
        JsonNode node = MAPPER.readTree(json);

        Path path = tempDir.resolve("some-random-file.json");
        Files.writeString(path, "{}");

        SpecVersion.VersionFlag result = SpecVersionDetector.detectVersion(node, path, SpecVersion.VersionFlag.V6);
        assertEquals(SpecVersion.VersionFlag.V6, result);
    }

    @Test
    void detectVersion_bothDetectionsEmpty_returnsDefault() {
        JsonNode emptyNode = MAPPER.createObjectNode();

        SpecVersion.VersionFlag result = SpecVersionDetector.detectVersion(emptyNode, null, SpecVersion.VersionFlag.V6);
        assertEquals(SpecVersion.VersionFlag.V6, result);
    }

    /* --------------------------------------------------------------------- */
    /* Parameterised branch‑coverage tests based on the supplied JSON data  */
    /* --------------------------------------------------------------------- */

    /**
     * Parameterised test that drives all scenarios described in the JSON test‑case
     * document.  Each {@link Arguments} entry contains:
     * <ul>
     *   <li>JSON representation of the {@code JsonNode} argument (or the literal {@code "null"}).</li>
     *   <li>String representation of the {@code Path} argument (or {@code "null"}).</li>
     *   <li>String representation of the {@code defaultVersion} argument
     *       (e.g. {@code "VersionFlag.V4"} or {@code "null"}).</li>
     *   <li>Expected outcome – either a {@code VersionFlag} name,
     *       {@code "null"} or an exception class name.</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("detectVersionBranchCoverageProvider")
    void detectVersion_branchCoverage(String jsonNodeStr,
                                      String pathStr,
                                      String defaultVersionStr,
                                      String expectedOutcome) throws Exception {

        // ----- Build arguments -------------------------------------------------
        JsonNode jsonNode = "null".equals(jsonNodeStr) ? null : MAPPER.readTree(jsonNodeStr);
        Path path = "null".equals(pathStr) ? null : Path.of(pathStr);
        SpecVersion.VersionFlag defaultVersion = "null".equals(defaultVersionStr)
                ? null
                : SpecVersion.VersionFlag.valueOf(defaultVersionStr.replace("VersionFlag.", ""));

        // ----- Decide what the mocked helpers should do -----------------------
        // Helper to decide the version that would be extracted from a JSON $schema.
        Optional<SpecVersion.VersionFlag> jsonDetection = detectVersionFromJsonString(jsonNodeStr);
        // Helper to decide the version that would be extracted from the path string.
        Optional<SpecVersion.VersionFlag> pathDetection = detectVersionFromPathString(pathStr);

        // ----- Prepare static mocks --------------------------------------------
        try (MockedStatic<SpecVersionDetector> mocked = mockStatic(SpecVersionDetector.class, CALLS_REAL_METHODS)) {

            // By default let the real implementations run; we only override the two helpers
            // when we have a concrete expectation for them.
            if (jsonNodeStr != null && !"null".equals(jsonNodeStr)) {
                mocked.when(() -> SpecVersionDetector.detectOptionalVersion(jsonNode))
                        .thenReturn(jsonDetection);
            } else if ("null".equals(jsonNodeStr) && expectsException(expectedOutcome, NullPointerException.class)) {
                // Let the real method be called – it will NPE inside detectOptionalVersion.
                mocked.when(() -> SpecVersionDetector.detectOptionalVersion(null))
                        .thenCallRealMethod();
            } else {
                mocked.when(() -> SpecVersionDetector.detectOptionalVersion(any()))
                        .thenReturn(Optional.empty());
            }

            if (pathStr != null && !"null".equals(pathStr)) {
                // Special case: malformed path that should cause IllegalArgumentException
                if (pathStr.startsWith(":::")) {
                    mocked.when(() -> SpecVersionDetector.detectVersionFromPath(path))
                            .thenThrow(new IllegalArgumentException("Malformed path"));
                } else {
                    mocked.when(() -> SpecVersionDetector.detectVersionFromPath(path))
                            .thenReturn(pathDetection);
                }
            } else if ("null".equals(pathStr) && expectsException(expectedOutcome, NullPointerException.class)) {
                mocked.when(() -> SpecVersionDetector.detectVersionFromPath(null))
                        .thenCallRealMethod(); // real method will NPE
            } else {
                mocked.when(() -> SpecVersionDetector.detectVersionFromPath(any()))
                        .thenReturn(Optional.empty());
            }

            // ----- Execute & verify ------------------------------------------------
            if (expectedOutcome.endsWith("Exception")) {
                Class<? extends Throwable> exClass = (Class<? extends Throwable>)
                        Class.forName("java.lang." + expectedOutcome);
                assertThrows(exClass,
                        () -> SpecVersionDetector.detectVersion(jsonNode, path, defaultVersion),
                        "Expected exception " + expectedOutcome);
            } else if ("null".equals(expectedOutcome)) {
                SpecVersion.VersionFlag result = SpecVersionDetector.detectVersion(jsonNode, path, defaultVersion);
                assertNull(result, "Expected null result");
            } else {
                SpecVersion.VersionFlag expected = SpecVersion.VersionFlag.valueOf(expectedOutcome.replace("VersionFlag.", ""));
                SpecVersion.VersionFlag actual = SpecVersionDetector.detectVersion(jsonNode, path, defaultVersion);
                assertEquals(expected, actual, "Version detection did not match expected");
            }
        }
    }

    /** Provides the raw data taken from the JSON test‑case document. */
    private static Stream<Arguments> detectVersionBranchCoverageProvider() {
        return Stream.of(
                // group: SpecVersionDetector.detectVersion - Branch Coverage
                Arguments.of("{\"$schema\": \"http://json-schema.org/draft-07/schema#\"}", "null", "VersionFlag.V4", "VersionFlag.V7"),
                Arguments.of("{}", "/tmp/schemas/draft-06/schema.json", "VersionFlag.V4", "VersionFlag.V6"),
                Arguments.of("{}", "/tmp/schemas/unknown/schema.json", "VersionFlag.V4", "VersionFlag.V4"),
                Arguments.of("null", "/tmp/schemas/draft-07/schema.json", "VersionFlag.V4", "VersionFlag.V7"),
                Arguments.of("{\"$schema\": \"http://json-schema.org/draft-04/schema#\"}", "/tmp/schemas/draft-07/schema.json", "VersionFlag.V6", "VersionFlag.V4"),
                Arguments.of("{}", "null", "VersionFlag.V6", "VersionFlag.V6"),

                // group: JSON overrides path (draft-06 vs draft-04)
                Arguments.of("{\"$schema\": \"http://json-schema.org/draft-06/schema#\"}", "/tmp/schemas/draft-04/schema.json", "VersionFlag.V7", "VersionFlag.V6"),

                // group: Both detections return same version (draft-06)
                Arguments.of("{\"$schema\": \"http://json-schema.org/draft-06/schema#\"}", "/tmp/schemas/draft-06/schema.json", "VersionFlag.V4", "VersionFlag.V6"),

                // group: Unknown path version falls back to default
                Arguments.of("{}", "/tmp/schemas/draft-99/schema.json", "VersionFlag.V4", "VersionFlag.V4"),

                // group: Null defaultVersion results in null return
                Arguments.of("{}", "null", "null", "null"),

                // group: Specification is a directory (no file name) falls back to default
                Arguments.of("{}", "/tmp/schemas/draft-07/", "VersionFlag.V4", "VersionFlag.V4"),

                // group: JSON node is an array, path provides version
                Arguments.of("[]", "/tmp/schemas/draft-07/schema.json", "VersionFlag.V4", "VersionFlag.V7"),

                // group: Null jsonNode causes NullPointerException
                Arguments.of("null", "/tmp/schemas/draft-07/schema.json", "VersionFlag.V4", "NullPointerException"),

                // group: Null specification causes NullPointerException
                Arguments.of("{\"$schema\": \"http://json-schema.org/draft-07/schema#\"}", "null", "VersionFlag.V4", "NullPointerException"),

                // group: Invalid Path throws IllegalArgumentException
                Arguments.of("{}", ":::invalid_path:::", "VersionFlag.V4", "IllegalArgumentException"),

                // group: detectOptionalVersion throws RuntimeException
                Arguments.of("{\"invalid\": \"@@@\"}", "/tmp/schemas/draft-07/schema.json", "VersionFlag.V4", "RuntimeException")
        );
    }

    /** Helper – returns the version that would be deduced from the