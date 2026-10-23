```java
package com.networknt.schema;

import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SpecVersionDetector_detectVersionFromPath_Test {

    private static Map<String, VersionFlag> originalSupportedVersions;

    @BeforeAll
    static void setupBeforeAll() {
        // Preserve the original content of the static map
        originalSupportedVersions = new HashMap<>(SpecVersionDetector.supportedVersions);

        // Populate the map with the versions that the detector recognises
        SpecVersionDetector.supportedVersions.put("draft4", VersionFlag.V4);
        SpecVersionDetector.supportedVersions.put("draft6", VersionFlag.V6);
        SpecVersionDetector.supportedVersions.put("draft7", VersionFlag.V7);
        SpecVersionDetector.supportedVersions.put("draft2019-09", VersionFlag.V201909);
        SpecVersionDetector.supportedVersions.put("draft2020-12", VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required at the moment
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original map state so other test suites are not affected
        SpecVersionDetector.supportedVersions.clear();
        SpecVersionDetector.supportedVersions.putAll(originalSupportedVersions);
    }

    @Test
    void detectVersionFromPath_singleSupportedElement_returnsCorrespondingFlag() {
        Path path = Paths.get("draft7");
        Optional<VersionFlag> result = SpecVersionDetector.detectVersionFromPath(path);
        assertEquals(Optional.of(VersionFlag.V7), result);
    }

    @Test
    void detectVersionFromPath_unsupportedElement_returnsEmpty() {
        Path path = Paths.get("unknown-draft");
        Optional<VersionFlag> result = SpecVersionDetector.detectVersionFromPath(path);
        assertEquals(Optional.empty(), result);
    }

    @Test
    void detectVersionFromPath_multipleElements_oneSupported_returnsThatFlag() {
        // First element is unknown, second is a supported version
        Path path = Paths.get("unknown", "draft2020-12", "another");
        Optional<VersionFlag> result = SpecVersionDetector.detectVersionFromPath(path);
        assertEquals(Optional.of(VersionFlag.V202012), result);
    }

    @Test
    void detectVersionFromPath_multipleSupportedElements_returnsAnySupportedFlag() {
        // Both elements are supported; the method uses findAny(), so any of them is acceptable.
        Path path = Paths.get("draft4", "draft6");
        Optional<VersionFlag> result = SpecVersionDetector.detectVersionFromPath(path);
        // The result must be one of the two supported flags
        assertTrue(result.isPresent());
        assertTrue(result.get() == VersionFlag.V4 || result.get() == VersionFlag.V6);
    }
}
```