**1. Public methods in `SpecVersionDetector`**

| # | Method signature | Description |
|---|------------------|-------------|
| 1 | `public static VersionFlag detect(JsonNode jsonNode)` | Detects the schema version from the `$schema` tag, throws `JsonSchemaException` if the tag is missing. |
| 2 | `public static Optional<VersionFlag> detectOptionalVersion(JsonNode jsonNode)` | Same as above but returns an `Optional.empty()` when the tag is missing. |
| 3 | `public static VersionFlag detectVersion(JsonNode jsonNode, Path specification, VersionFlag defaultVersion)` | Returns the version found in the JSON node **or** from the supplied path; if neither is found, returns the supplied default. |
| 4 | `public static Optional<VersionFlag> detectVersionFromPath(Path path)` | Scans the supplied `Path` for any segment that matches a known draft name and returns the corresponding `VersionFlag`. |
| 5 | `public static Optional<VersionFlag> detectOptionalVersion(String schemaUri)` | Parses a raw schema URI string and returns the matching `VersionFlag` (or empty). |

---

**2. Basic JUnit‑4 test cases for each method**

| Method | Basic test idea |
|--------|-----------------|
| `detect(JsonNode)` | Provide a JSON node that contains a known `$schema` value (`"draft7"`). Verify the returned `VersionFlag.V7`. |
| `detectOptionalVersion(JsonNode)` | Same JSON as above, assert `Optional.isPresent()` and the correct flag. |
| `detectVersion(JsonNode, Path, VersionFlag)` | Supply a JSON node with `$schema="draft2020-12"` and a path that also contains a draft name; verify the flag from the JSON node is chosen. |
| `detectVersionFromPath(Path)` | Build a `Path` like `Paths.get("src/test/resources/draft6")` and assert the method returns `Optional.of(VersionFlag.V6)`. |
| `detectOptionalVersion(String)` | Call with `"http://json-schema.org/draft-04/schema#"` (or the short `"draft4"` after normalisation) and assert `Optional.of(VersionFlag.V4)`. |

---

**3. Edge‑case & exception scenarios that must be tested**

| Scenario | Why it matters | Expected result |
|----------|----------------|-----------------|
| **Missing `$schema` tag** | `detect` must fail, `detectOptionalVersion` must return empty. | `detect` → `JsonSchemaException` with message *"'$schema' tag is not present"*; `detectOptionalVersion` → `Optional.empty()`. |
| **Unrecognizable `$schema` value** | The code explicitly throws `JsonSchemaException` for unknown URIs. | `JsonSchemaException` with message containing the unknown value. |
| **`null` JsonNode argument** | The method calls `jsonNode.get(...)` → NPE. | `NullPointerException`. |
| **`null` Path argument** | `detectVersionFromPath` will NPE when `path.spliterator()` is called. | `NullPointerException`. |
| **Path without any supported draft segment** | Should fall back to default version in `detectVersion`. | Returned version equals the supplied default. |
| **Path containing several supported drafts** | `detectVersionFromPath` uses `findAny()`. The result must be one of the supported drafts present. | `Optional` present and value is either of the drafts found. |
| **`detectOptionalVersion(String)` with `null` or empty string** | `VersionFlag.fromId(null)` returns `Optional.empty()` (as per library implementation). | `Optional.empty()`. |
| **`detectOptionalVersion(String)` with malformed URI** | Should also return empty (no matching flag). | `Optional.empty()`. |

---

**4. JUnit‑4 test cases covering the edge cases**

*The test class below combines the basic tests and the edge‑case tests into a single, self‑contained file.*

```java
###Test START##
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
###Test END##
```