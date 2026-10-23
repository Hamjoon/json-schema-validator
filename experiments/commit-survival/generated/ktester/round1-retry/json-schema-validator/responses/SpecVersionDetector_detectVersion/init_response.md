```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SpecVersionDetector_detectVersion_Test {

    /** Shared mapper for all tests. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Default version used when detection fails. */
    private static final SpecVersion.VersionFlag DEFAULT_VERSION = SpecVersion.VersionFlag.V6;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise for this utility class.
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

    /**
     * When the JSON node contains a recognizable {@code $schema} URI,
     * {@link SpecVersionDetector#detectVersion(JsonNode, Path, VersionFlag)} must
     * return the version derived from the JSON, even if the supplied path also
     * hints at a different version.
     */
    @Test
    void detectVersion_jsonNodeVersionOverridesPath(@TempDir Path tempDir) throws IOException {
        // JSON with a $schema that maps to V7 (draft‑07)
        String json = """
                {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "type": "object"
                }
                """;
        JsonNode node = MAPPER.readTree(json);

        // Create a file whose name suggests a different version (draft‑04)
        Path path = tempDir.resolve("draft4-schema.json");
        Files.writeString(path, "{}");

        // Invoke the method under test
        SpecVersion.VersionFlag result = SpecVersionDetector.detectVersion(node, path, DEFAULT_VERSION);

        // The version from the JSON should win
        assertEquals(SpecVersion.VersionFlag.V7, result);
    }

    /**
     * When the JSON node does **not** contain a {@code $schema} tag but the
     * supplied {@link Path} contains a recognizable version string, the version
     * extracted from the path must be returned.
     */
    @Test
    void detectVersion_pathVersionUsedWhenJsonMissing(@TempDir Path tempDir) throws IOException {
        // JSON without any $schema tag
        String json = """
                {
                  "type": "object"
                }
                """;
        JsonNode node = MAPPER.readTree(json);

        // File name contains "draft2020-12" which maps to V202012
        Path path = tempDir.resolve("draft2020-12-schema.json");
        Files.writeString(path, "{}");

        SpecVersion.VersionFlag result = SpecVersionDetector.detectVersion(node, path, DEFAULT_VERSION);

        assertEquals(SpecVersion.VersionFlag.V202012, result);
    }

    /**
     * When neither the JSON node nor the path provide a recognizable version,
     * the supplied default version must be returned.
     */
    @Test
    void detectVersion_returnsDefaultWhenNoVersionDetected(@TempDir Path tempDir) throws IOException {
        // JSON without $schema
        String json = """
                {
                  "type": "object"
                }
                """;
        JsonNode node = MAPPER.readTree(json);

        // Path that does not contain any known version identifier
        Path path = tempDir.resolve("some-random-file.json");
        Files.writeString(path, "{}");

        SpecVersion.VersionFlag result = SpecVersionDetector.detectVersion(node, path, DEFAULT_VERSION);

        assertEquals(DEFAULT_VERSION, result);
    }

    /**
     * Verify that the method correctly propagates an {@link Optional#empty}
     * result from both detection strategies and still returns the default.
     */
    @Test
    void detectVersion_bothDetectionsEmpty_returnsDefault() {
        // Empty JSON node (no $schema) – we can use an empty object node.
        JsonNode emptyNode = MAPPER.createObjectNode();

        // Path that is {@code null} is allowed; the detector should simply ignore it.
        // (The implementation guards against NPE by checking Optional::isPresent.)
        SpecVersion.VersionFlag result = SpecVersionDetector.detectVersion(emptyNode, null, DEFAULT_VERSION);

        assertEquals(DEFAULT_VERSION, result);
    }
}
```