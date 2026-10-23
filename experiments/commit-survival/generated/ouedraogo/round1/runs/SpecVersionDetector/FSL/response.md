###Test START##
```java
package com.networknt.schema;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Comprehensive JUnit‑4 test suite for {@link SpecVersionDetector}.
 *
 * The tests cover:
 * <ul>
 *   <li>Detection of a schema version from a {@link JsonNode} (both present and absent).</li>
 *   <li>Behaviour when the "$schema" tag value is unrecognizable.</li>
 *   <li>Detection of a version from a {@link Path} (including mixed‑case and deep paths).</li>
 *   <li>Combined detection logic that prefers the JSON tag over the path and falls back to a default.</li>
 *   <li>Direct string‑based detection via {@link SpecVersionDetector#detectOptionalVersion(String)}.</li>
 * </ul>
 */
public class SpecVersionDetectorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /* --------------------------------------------------------------------- */
    /*  detectOptionalVersion(JsonNode)                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void detectOptionalVersion_shouldReturnVersionWhenTagPresent() {
        JsonNode node = MAPPER.createObjectNode().put("$schema", "draft7");
        Optional<VersionFlag> result = SpecVersionDetector.detectOptionalVersion(node);
        assertTrue(result.isPresent());
        assertEquals(VersionFlag.V7, result.get());
    }

    @Test
    public void detectOptionalVersion_shouldReturnEmptyWhenTagMissing() {
        JsonNode node = MAPPER.createObjectNode(); // no "$schema"
        Optional<VersionFlag> result = SpecVersionDetector.detectOptionalVersion(node);
        assertFalse(result.isPresent());
    }

    @Test(expected = JsonSchemaException.class)
    public void detectOptionalVersion_shouldThrowWhenTagUnrecognizable() {
        JsonNode node = MAPPER.createObjectNode().put("$schema", "unknown-draft");
        // The method is expected to throw JsonSchemaException for an unknown identifier.
        SpecVersionDetector.detectOptionalVersion(node);
    }

    /* --------------------------------------------------------------------- */
    /*  detect(JsonNode)                                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void detect_shouldReturnVersionWhenTagPresent() {
        JsonNode node = MAPPER.createObjectNode().put("$schema", "draft2020-12");
        VersionFlag version = SpecVersionDetector.detect(node);
        assertEquals(VersionFlag.V202012, version);
    }

    @Test(expected = JsonSchemaException.class)
    public void detect_shouldThrowWhenTagMissing() {
        JsonNode node = MAPPER.createObjectNode(); // no "$schema"
        SpecVersionDetector.detect(node);
    }

    /* --------------------------------------------------------------------- */
    /*  detectOptionalVersion(String)                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void detectOptionalVersionString_validId() {
        Optional<VersionFlag> result = SpecVersionDetector.detectOptionalVersion("draft4");
        assertTrue(result.isPresent());
        assertEquals(VersionFlag.V4, result.get());
    }

    @Test
    public void detectOptionalVersionString_invalidId() {
        Optional<VersionFlag> result = SpecVersionDetector.detectOptionalVersion("not-a-draft");
        assertFalse(result.isPresent());
    }

    /* --------------------------------------------------------------------- */
    /*  detectVersionFromPath(Path)                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void detectVersionFromPath_shouldFindVersionInPathSegments() {
        // Path contains the version identifier as a middle segment.
        Path path = Paths.get("some", "folder", "draft7", "my-schema.json");
        Optional<VersionFlag> result = SpecVersionDetector.detectVersionFromPath(path);
        assertTrue(result.isPresent());
        assertEquals(VersionFlag.V7, result.get());
    }

    @Test
    public void detectVersionFromPath_shouldBeCaseSensitive() {
        // The map keys are lower‑case; a mismatching case must not be detected.
        Path path = Paths.get("some", "DRAFT7", "file.json");
        Optional<VersionFlag> result = SpecVersionDetector.detectVersionFromPath(path);
        assertFalse(result.isPresent());
    }

    @Test
    public void detectVersionFromPath_shouldReturnEmptyWhenNoVersionFound() {
        Path path = Paths.get("no", "version", "here.txt");
        Optional<VersionFlag> result = SpecVersionDetector.detectVersionFromPath(path);
        assertFalse(result.isPresent());
    }

    /* --------------------------------------------------------------------- */
    /*  detectVersion(JsonNode, Path, VersionFlag)                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void detectVersion_prefersJsonTagOverPath() {
        JsonNode node = MAPPER.createObjectNode().put("$schema", "draft6");
        Path path = Paths.get("some", "draft7", "schema.json");
        VersionFlag result = SpecVersionDetector.detectVersion(node, path, VersionFlag.V4);
        // JSON tag wins even though the path also contains a draft identifier.
        assertEquals(VersionFlag.V6, result);
    }

    @Test
    public void detectVersion_usesPathWhenJsonTagMissing() {
        JsonNode node = MAPPER.createObjectNode(); // no "$schema"
        Path path = Paths.get("tests", "draft2019-09", "example.json");
        VersionFlag result = SpecVersionDetector.detectVersion(node, path, VersionFlag.V4);
        assertEquals(VersionFlag.V201909, result);
    }

    @Test
    public void detectVersion_returnsDefaultWhenNoInfoAvailable() {
        JsonNode node = MAPPER.createObjectNode(); // no "$schema"
        Path path = Paths.get("some", "random", "dir");
        VersionFlag defaultVersion = VersionFlag.V7;
        VersionFlag result = SpecVersionDetector.detectVersion(node, path, defaultVersion);
        assertEquals(defaultVersion, result);
    }

    @Test(expected = JsonSchemaException.class)
    public void detectVersion_throwsIfJsonTagUnrecognizableEvenIfPathHasVersion() {
        JsonNode node = MAPPER.createObjectNode().put("$schema", "nonexistent-draft");
        Path path = Paths.get("draft4", "schema.json");
        // The JSON tag is parsed first; its unrecognizable value must trigger an exception.
        SpecVersionDetector.detectVersion(node, path, VersionFlag.V202012);
    }

    /* --------------------------------------------------------------------- */
    /*  Helper – normalise meta‑schema URIs for the tests (if needed)        */
    /* --------------------------------------------------------------------- */

    /**
     * The production code uses {@link JsonSchemaFactory#normalizeMetaSchemaUri(String)}.
     * In this test suite we deliberately feed the detector with values that are already
     * in the canonical form (e.g., {@code "draft7"}).  This avoids a hard dependency on the
     * implementation of {@code normalizeMetaSchemaUri} while still exercising the full
     * detection logic.
     */
}
```
###Test END##