package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.Assert.*;

public class SpecVersionDetectorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private JsonNode jsonWithDraft7;
    private JsonNode jsonWithDraft2020_12;
    private JsonNode jsonWithUnknownDraft;
    private JsonNode jsonWithoutSchema;

    @Before
    public void setUp() throws Exception {
        jsonWithDraft7 = MAPPER.readTree("{\"$schema\":\"draft7\"}");
        jsonWithDraft2020_12 = MAPPER.readTree("{\"$schema\":\"draft2020-12\"}");
        jsonWithUnknownDraft = MAPPER.readTree("{\"$schema\":\"http://example.com/unknown\"}");
        jsonWithoutSchema = MAPPER.readTree("{\"title\":\"No schema tag\"}");
    }

    /* -------------------------------------------------
     *  Basic functionality tests
     * ------------------------------------------------- */

    @Test
    public void testDetectReturnsCorrectVersion() {
        VersionFlag flag = SpecVersionDetector.detect(jsonWithDraft7);
        assertEquals(VersionFlag.V7, flag);
    }

    @Test
    public void testDetectOptionalVersionPresent() {
        Optional<VersionFlag> opt = SpecVersionDetector.detectOptionalVersion(jsonWithDraft2020_12);
        assertTrue(opt.isPresent());
        assertEquals(VersionFlag.V202012, opt.get());
    }

    @Test
    public void testDetectVersionPrefersJsonOverPath() {
        Path path = Paths.get("some", "draft4", "folder");
        VersionFlag result = SpecVersionDetector.detectVersion(jsonWithDraft2020_12, path, VersionFlag.V4);
        // JSON node contains draft2020-12, so it should win over the draft4 found in the path
        assertEquals(VersionFlag.V202012, result);
    }

    @Test
    public void testDetectVersionFromPathFound() {
        Path path = Paths.get("schemas", "draft6", "example.json");
        Optional<VersionFlag> opt = SpecVersionDetector.detectVersionFromPath(path);
        assertTrue(opt.isPresent());
        assertEquals(VersionFlag.V6, opt.get());
    }

    @Test
    public void testDetectOptionalVersionFromString() {
        Optional<VersionFlag> opt = SpecVersionDetector.detectOptionalVersion("draft4");
        assertTrue(opt.isPresent());
        assertEquals(VersionFlag.V4, opt.get());
    }

    /* -------------------------------------------------
     *  Edge‑case / exception tests
     * ------------------------------------------------- */

    @Test
    public void testDetectThrowsWhenSchemaTagMissing() {
        thrown.expect(JsonSchemaException.class);
        thrown.expectMessage("'$schema' tag is not present");
        SpecVersionDetector.detect(jsonWithoutSchema);
    }

    @Test
    public void testDetectOptionalVersionEmptyWhenTagMissing() {
        Optional<VersionFlag> opt = SpecVersionDetector.detectOptionalVersion(jsonWithoutSchema);
        assertFalse(opt.isPresent());
    }

    @Test
    public void testDetectThrowsWhenSchemaTagUnrecognizable() {
        thrown.expect(JsonSchemaException.class);
        thrown.expectMessage("'http://example.com/unknown' is unrecognizable schema");
        SpecVersionDetector.detect(jsonWithUnknownDraft);
    }

    @Test(expected = NullPointerException.class)
    public void testDetectWithNullJsonNode() {
        SpecVersionDetector.detect(null);
    }

    @Test(expected = NullPointerException.class)
    public void testDetectVersionFromPathWithNullPath() {
        SpecVersionDetector.detectVersionFromPath(null);
    }

    @Test
    public void testDetectVersionFallsBackToDefaultWhenNothingFound() {
        Path path = Paths.get("some", "folder", "without", "draft");
        VersionFlag defaultVersion = VersionFlag.V7;
        VersionFlag result = SpecVersionDetector.detectVersion(jsonWithoutSchema, path, defaultVersion);
        assertEquals(defaultVersion, result);
    }

    @Test
    public void testDetectVersionFromPathMultipleSupportedSegments() {
        // Path contains both draft4 and draft7; findAny() may return either.
        Path path = Paths.get("base", "draft4", "sub", "draft7");
        Optional<VersionFlag> opt = SpecVersionDetector.detectVersionFromPath(path);
        assertTrue(opt.isPresent());
        // The result must be one of the two supported drafts present in the path
        assertTrue(opt.get() == VersionFlag.V4 || opt.get() == VersionFlag.V7);
    }

    @Test
    public void testDetectOptionalVersionStringWithNullReturnsEmpty() {
        Optional<VersionFlag> opt = SpecVersionDetector.detectOptionalVersion((String) null);
        assertFalse(opt.isPresent());
    }

    @Test
    public void testDetectOptionalVersionStringWithEmptyReturnsEmpty() {
        Optional<VersionFlag> opt = SpecVersionDetector.detectOptionalVersion("");
        assertFalse(opt.isPresent());
    }

    @Test
    public void testDetectOptionalVersionStringWithMalformedUriReturnsEmpty() {
        Optional<VersionFlag> opt = SpecVersionDetector.detectOptionalVersion("not-a-valid-schema");
        assertFalse(opt.isPresent());
    }
}
