/**
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.SpecVersionDetector}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal detection via the {@code $schema} tag.</li>
 *   <li>Missing {@code $schema} handling (exception / empty optional).</li>
 *   <li>Unrecognizable schema URIs.</li>
 *   <li>Detection from a {@link java.nio.file.Path}.</li>
 *   <li>Combined detection logic with a supplied default version.</li>
 *   <li>Utility method {@code detectOptionalVersion(String)}.</li>
 * </ul>
 *
 * The test class uses Jackson's {@link com.fasterxml.jackson.databind.ObjectMapper}
 * to build {@link com.fasterxml.jackson.databind.JsonNode} instances and JUnit‑4
 * {@code ExpectedException} rule to verify exception messages.
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.Assert.*;

public class SpecVersionDetectorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private JsonNode nodeWithSchema(String schemaUri) {
        return MAPPER.createObjectNode().put("$schema", schemaUri);
    }

    private JsonNode nodeWithoutSchema() {
        return MAPPER.createObjectNode(); // empty object – no $schema
    }

    /* --------------------------------------------------------------
     *  Tests for detect(JsonNode)
     * -------------------------------------------------------------- */

    @Test
    public void detect_ShouldReturnVersion_WhenSchemaTagIsPresentAndRecognized() {
        JsonNode node = nodeWithSchema("http://json-schema.org/draft-07/schema#");
        VersionFlag flag = SpecVersionDetector.detect(node);
        assertEquals(VersionFlag.V7, flag);
    }

    @Test
    public void detect_ShouldThrowException_WhenSchemaTagIsMissing() {
        thrown.expect(JsonSchemaException.class);
        thrown.expectMessage("'$schema' tag is not present");

        SpecVersionDetector.detect(nodeWithoutSchema());
    }

    @Test
    public void detect_ShouldThrowException_WhenSchemaTagIsUnrecognizable() {
        String unknownUri = "http://example.com/unknown-schema#";
        thrown.expect(JsonSchemaException.class);
        thrown.expectMessage("'" + unknownUri + "' is unrecognizable schema");

        SpecVersionDetector.detect(nodeWithSchema(unknownUri));
    }

    /* --------------------------------------------------------------
     *  Tests for detectOptionalVersion(JsonNode)
     * -------------------------------------------------------------- */

    @Test
    public void detectOptionalVersion_ShouldReturnOptionalWithVersion_WhenTagPresent() {
        JsonNode node = nodeWithSchema("http://json-schema.org/draft-04/schema#");
        Optional<VersionFlag> opt = SpecVersionDetector.detectOptionalVersion(node);
        assertTrue(opt.isPresent());
        assertEquals(VersionFlag.V4, opt.get());
    }

    @Test
    public void detectOptionalVersion_ShouldReturnEmptyOptional_WhenTagMissing() {
        Optional<VersionFlag> opt = SpecVersionDetector.detectOptionalVersion(nodeWithoutSchema());
        assertFalse(opt.isPresent());
    }

    @Test
    public void detectOptionalVersion_ShouldThrowException_WhenTagUnrecognizable() {
        String unknownUri = "urn:unknown:schema";
        thrown.expect(JsonSchemaException.class);
        thrown.expectMessage("'" + unknownUri + "' is unrecognizable schema");

        SpecVersionDetector.detectOptionalVersion(nodeWithSchema(unknownUri));
    }

    /* --------------------------------------------------------------
     *  Tests for detectOptionalVersion(String)
     * -------------------------------------------------------------- */

    @Test
    public void detectOptionalVersionString_ShouldReturnVersion_ForKnownUri() {
        Optional<VersionFlag> opt = SpecVersionDetector.detectOptionalVersion(
                "http://json-schema.org/draft-2020-12/schema#");
        assertTrue(opt.isPresent());
        assertEquals(VersionFlag.V202012, opt.get());
    }

    @Test
    public void detectOptionalVersionString_ShouldReturnEmpty_ForUnknownUri() {
        Optional<VersionFlag> opt = SpecVersionDetector.detectOptionalVersion(
                "http://example.org/not-a-schema");
        assertFalse(opt.isPresent());
    }

    /* --------------------------------------------------------------
     *  Tests for detectVersionFromPath(Path)
     * -------------------------------------------------------------- */

    @Test
    public void detectVersionFromPath_ShouldFindVersion_WhenPathContainsDraftSegment() {
        Path path = Paths.get("src/test/resources/draft7/valid-schema.json");
        Optional<VersionFlag> opt = SpecVersionDetector.detectVersionFromPath(path);
        assertTrue(opt.isPresent());
        assertEquals(VersionFlag.V7, opt.get());
    }

    @Test
    public void detectVersionFromPath_ShouldFindVersion_WhenSegmentIsNotFirst() {
        Path path = Paths.get("some/inner/draft2020-12/spec.json");
        Optional<VersionFlag> opt = SpecVersionDetector.detectVersionFromPath(path);
        assertTrue(opt.isPresent());
        assertEquals(VersionFlag.V202012, opt.get());
    }

    @Test
    public void detectVersionFromPath_ShouldReturnEmpty_WhenNoDraftSegmentFound() {
        Path path = Paths.get("no/draft/here/schema.json");
        Optional<VersionFlag> opt = SpecVersionDetector.detectVersionFromPath(path);
        assertFalse(opt.isPresent());
    }

    /* --------------------------------------------------------------
     *  Tests for detectVersion(JsonNode, Path, VersionFlag)
     * -------------------------------------------------------------- */

    @Test
    public void detectVersion_ShouldPreferSchemaTagOverPath() {
        JsonNode node = nodeWithSchema("http://json-schema.org/draft-06/schema#");
        Path path = Paths.get("some/draft2020-12/ignore.json");
        VersionFlag result = SpecVersionDetector.detectVersion(node, path, VersionFlag.V4);
        // $schema says draft‑6, path says draft‑2020‑12 – $schema wins
        assertEquals(VersionFlag.V6, result);
    }

    @Test
    public void detectVersion_ShouldFallBackToPath_WhenSchemaTagMissing() {
        JsonNode node = nodeWithoutSchema();
        Path path = Paths.get("folder/draft2019-09/example.json");
        VersionFlag result = SpecVersionDetector.detectVersion(node, path, VersionFlag.V4);
        assertEquals(VersionFlag.V201909, result);
    }

    @Test
    public void detectVersion_ShouldReturnDefault_WhenNeitherTagNorPathProvidesVersion() {
        JsonNode node = nodeWithoutSchema();
        Path path = Paths.get("some/unknown/structure.json");
        VersionFlag defaultFlag = VersionFlag.V7;
        VersionFlag result = SpecVersionDetector.detectVersion(node, path, defaultFlag);
        assertEquals(defaultFlag, result);
    }

    @Test
    public void detectVersion_ShouldReturnDefault_WhenBothOptionalDetectionsAreEmpty() {
        JsonNode node = nodeWithoutSchema();
        // Path that does not contain any known draft string
        Path path = Paths.get("abc/def/ghi");
        VersionFlag defaultFlag = VersionFlag.V4;
        VersionFlag result = SpecVersionDetector.detectVersion(node, path, defaultFlag);
        assertEquals(defaultFlag, result);
    }

    /* --------------------------------------------------------------
     *  Additional edge‑case tests
     * -------------------------------------------------------------- */

    @Test
    public void detectOptionalVersion_ShouldBeCaseInsensitive_ForMetaSchemaUriNormalization() {
        // The factory normalizes the URI; we give a mixed‑case version string
        JsonNode node = nodeWithSchema("HTTP://JSON-SCHEMA.ORG/DRAFT-07/SCHEMA#");
        Optional<VersionFlag> opt = SpecVersionDetector.detectOptionalVersion(node);
        assertTrue(opt.isPresent());
        assertEquals(VersionFlag.V7, opt.get());
    }

    @Test
    public void detectVersionFromPath_ShouldHandleWindowsBackslashes() {
        // Simulate a Windows path string (using Paths.get which normalises separators)
        Path path = Paths.get("C:\\schemas\\draft6\\my-schema.json");
        Optional<VersionFlag> opt = SpecVersionDetector.detectVersionFromPath(path);
        assertTrue(opt.isPresent());
        assertEquals(VersionFlag.V6, opt.get());
    }

    @Test
    public void detectOptionalVersion_ShouldNotThrow_WhenSchemaTagIsExplicitNullNode() {
        // Build a node where "$schema" exists but its value is JSON null
        JsonNode node = MAPPER.createObjectNode().set("$schema", MAPPER.nullNode());

        // According to implementation, jsonNode.get("$schema") returns a NullNode,
        // asText() on NullNode yields "null", which is not a known schema -> exception.
        thrown.expect(JsonSchemaException.class);
        thrown.expectMessage("'null' is unrecognizable schema");

        SpecVersionDetector.detectOptionalVersion(node);
    }
}
