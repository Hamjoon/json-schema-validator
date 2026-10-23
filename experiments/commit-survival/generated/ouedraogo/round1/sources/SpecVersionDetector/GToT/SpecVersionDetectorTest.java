package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.*;

public class SpecVersionDetectorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    /*** Helper to build a JsonNode containing a $schema tag ***/
    private static JsonNode nodeWithSchema(String schemaValue) throws IOException {
        String json = String.format("{\"$schema\":\"%s\"}", schemaValue);
        return MAPPER.readTree(json);
    }

    /*** 1. detect(JsonNode) – happy path ***/
    @Test
    public void detect_shouldReturnVersion_whenSchemaTagPresent() throws IOException {
        JsonNode node = nodeWithSchema("draft7");
        VersionFlag flag = SpecVersionDetector.detect(node);
        assertEquals(VersionFlag.V7, flag);
    }

    /*** 2. detect(JsonNode) – missing $schema ***/
    @Test(expected = JsonSchemaException.class)
    public void detect_shouldThrow_whenSchemaTagMissing() throws IOException {
        JsonNode node = MAPPER.readTree("{\"type\":\"object\"}");
        try {
            SpecVersionDetector.detect(node);
        } catch (JsonSchemaException ex) {
            assertEquals("'$schema' tag is not present", ex.getMessage());
            throw ex;
        }
    }

    /*** 3. detect(JsonNode) – null argument ***/
    @Test(expected = NullPointerException.class)
    public void detect_shouldThrowNPE_whenJsonNodeIsNull() {
        SpecVersionDetector.detect(null);
    }

    /*** 4. detectOptionalVersion(JsonNode) – present ***/
    @Test
    public void detectOptionalVersion_shouldReturnOptional_whenTagPresent() throws IOException {
        JsonNode node = nodeWithSchema("draft2020-12");
        Optional<VersionFlag> opt = SpecVersionDetector.detectOptionalVersion(node);
        assertTrue(opt.isPresent());
        assertEquals(VersionFlag.V202012, opt.get());
    }

    /*** 5. detectOptionalVersion(JsonNode) – missing ***/
    @Test
    public void detectOptionalVersion_shouldReturnEmpty_whenTagMissing() throws IOException {
        JsonNode node = MAPPER.readTree("{\"title\":\"test\"}");
        Optional<VersionFlag> opt = SpecVersionDetector.detectOptionalVersion(node);
        assertFalse(opt.isPresent());
    }

    /*** 6. detectOptionalVersion(JsonNode) – unrecognizable value ***/
    @Test(expected = JsonSchemaException.class)
    public void detectOptionalVersion_shouldThrow_whenTagUnrecognizable() throws IOException {
        JsonNode node = nodeWithSchema("unknown-draft");
        try {
            SpecVersionDetector.detectOptionalVersion(node);
        } catch (JsonSchemaException ex) {
            assertTrue(ex.getMessage().contains("'unknown-draft' is unrecognizable schema"));
            throw ex;
        }
    }

    /*** 7. detectVersion – $schema wins over path ***/
    @Test
    public void detectVersion_shouldPreferSchemaTag_overPath() throws IOException {
        JsonNode node = nodeWithSchema("draft6");          // V6 from $schema
        Path path = tmpFolder.getRoot().toPath().resolve("draft7"); // path would suggest V7
        VersionFlag result = SpecVersionDetector.detectVersion(node, path, VersionFlag.V4);
        assertEquals(VersionFlag.V6, result); // $schema should win
    }

    /*** 8. detectVersion – falls back to path when $schema absent ***/
    @Test
    public void detectVersion_shouldUsePath_whenSchemaTagMissing() throws IOException {
        JsonNode node = MAPPER.readTree("{\"type\":\"object\"}"); // no $schema
        Path path = tmpFolder.getRoot().toPath().resolve("draft4");
        VersionFlag result = SpecVersionDetector.detectVersion(node, path, VersionFlag.V7);
        assertEquals(VersionFlag.V4, result); // version taken from path
    }

    /*** 9. detectVersion – returns default when nothing is found ***/
    @Test
    public void detectVersion_shouldReturnDefault_whenNoVersionDetected() throws IOException {
        JsonNode node = MAPPER.readTree("{\"type\":\"array\"}");
        Path path = tmpFolder.getRoot().toPath(); // no draft segment
        VersionFlag result = SpecVersionDetector.detectVersion(node, path, VersionFlag.V201909);
        assertEquals(VersionFlag.V201909, result);
    }

    /*** 10. detectVersionFromPath – finds a draft name in any segment ***/
    @Test
    public void detectVersionFromPath_shouldFindVersionInPathSegments() throws IOException {
        // create a nested path: /tmp/abc/draft7/xyz
        Path path = tmpFolder.getRoot().toPath()
                .resolve("abc")
                .resolve("draft7")
                .resolve("xyz");
        Optional<VersionFlag> opt = SpecVersionDetector.detectVersionFromPath(path);
        assertTrue(opt.isPresent());
        assertEquals(VersionFlag.V7, opt.get());
    }

    /*** 11. detectVersionFromPath – returns empty when no known segment ***/
    @Test
    public void detectVersionFromPath_shouldReturnEmpty_whenNoSupportedSegment() throws IOException {
        Path path = tmpFolder.getRoot().toPath().resolve("some").resolve("random");
        Optional<VersionFlag> opt = SpecVersionDetector.detectVersionFromPath(path);
        assertFalse(opt.isPresent());
    }

    /*** 12. detectVersionFromPath – null argument ***/
    @Test(expected = NullPointerException.class)
    public void detectVersionFromPath_shouldThrowNPE_whenPathIsNull() {
        SpecVersionDetector.detectVersionFromPath(null);
    }

    /*** 13. detectOptionalVersion(String) – known URI ***/
    @Test
    public void detectOptionalVersionString_shouldReturnVersion_forKnownUri() {
        // canonical URI for draft‑07 (the actual value used by VersionFlag.fromId)
        String uri = "http://json-schema.org/draft-07/schema#";
        Optional<VersionFlag> opt = SpecVersionDetector.detectOptionalVersion(uri);
        assertTrue(opt.isPresent());
        assertEquals(VersionFlag.V7, opt.get());
    }

    /*** 14. detectOptionalVersion(String) – unknown URI ***/
    @Test
    public void detectOptionalVersionString_shouldReturnEmpty_forUnknownUri() {
        Optional<VersionFlag> opt = SpecVersionDetector.detectOptionalVersion("http://example.com/unknown");
        assertFalse(opt.isPresent());
    }

    /*** 15. detectOptionalVersion(String) – null or empty ***/
    @Test
    public void detectOptionalVersionString_shouldReturnEmpty_forNullOrEmpty() {
        assertFalse(SpecVersionDetector.detectOptionalVersion(null).isPresent());
        assertFalse(SpecVersionDetector.detectOptionalVersion("").isPresent());
    }
}
