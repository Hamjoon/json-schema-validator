```java
package com.networknt.schema;

import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SpecVersionDetector#detectVersionFromPath(Path)}.
 *
 * The test suite covers all scenarios described in the provided JSON test matrix,
 * using real {@link Path} objects where possible and Mockito‑based fakes when a
 * special {@link Spliterator} behaviour is required (null elements, exceptions, etc.).
 *
 * The static {@code supportedVersions} map is populated once in {@code @BeforeAll}
 * and restored in {@code @AfterAll}. Individual tests may temporarily clear or
 * replace the map via reflection – the original content is restored afterwards
 * to keep the suite isolated.
 */
class SpecVersionDetector_detectVersionFromPath_Test {

    private static Map<String, VersionFlag> originalSupportedVersions;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Preserve the original map content
        originalSupportedVersions = new HashMap<>(SpecVersionDetector.supportedVersions);

        // Populate the map with known versions (used by many test cases)
        SpecVersionDetector.supportedVersions.put("draft4", VersionFlag.V4);
        SpecVersionDetector.supportedVersions.put("draft6", VersionFlag.V6);
        SpecVersionDetector.supportedVersions.put("draft7", VersionFlag.V7);
        SpecVersionDetector.supportedVersions.put("draft2019-09", VersionFlag.V201909);
        SpecVersionDetector.supportedVersions.put("draft2020-12", VersionFlag.V202012);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original static map state
        SpecVersionDetector.supportedVersions.clear();
        SpecVersionDetector.supportedVersions.putAll(originalSupportedVersions);
    }

    /* --------------------------------------------------------------------- */
    /* Existing simple positive/negative cases (kept unchanged)            */
    /* --------------------------------------------------------------------- */

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
        Path path = Paths.get("unknown", "draft2020-12", "another");
        Optional<VersionFlag> result = SpecVersionDetector.detectVersionFromPath(path);
        assertEquals(Optional.of(VersionFlag.V202012), result);
    }

    @Test
    void detectVersionFromPath_multipleSupportedElements_returnsAnySupportedFlag() {
        Path path = Paths.get("draft4", "draft6");
        Optional<VersionFlag> result = SpecVersionDetector.detectVersionFromPath(path);
        assertTrue(result.isPresent());
        assertTrue(result.get() == VersionFlag.V4 || result.get() == VersionFlag.V6);
    }

    /* --------------------------------------------------------------------- */
    /* Additional cases derived from the JSON matrix                         */
    /* --------------------------------------------------------------------- */

    @Test
    void detectVersionFromPath_emptyPath_returnsEmptyOptional() {
        // Mock a Path whose spliterator yields no elements
        Path emptyPath = mock(Path.class);
        when(emptyPath.spliterator()).thenReturn(Collections.emptyIterator().spliterator());

        Optional<VersionFlag> result = SpecVersionDetector.detectVersionFromPath(emptyPath);
        assertEquals(Optional.empty(), result);
    }

    @Test
    void detectVersionFromPath_pathWithUnsupported_returnsEmptyOptional() {
        Path path = Paths.get("nonexistent");
        Optional<VersionFlag> result = SpecVersionDetector.detectVersionFromPath(path);
        assertEquals(Optional.empty(), result);
    }

    @Test
    void detectVersionFromPath_pathWithSupported_returnsCorrespondingFlag() {
        Path path = Paths.get("draft4");
        Optional<VersionFlag> result = SpecVersionDetector.detectVersionFromPath(path);
        assertEquals(Optional.of(VersionFlag.V4), result);
    }

    @Test
    void detectVersionFromPath_pathMixedSupportedFirstUnsupportedSecond_returnsSupportedFlag() {
        Path path = Paths.get("unknown", "draft7");
        Optional<VersionFlag> result = SpecVersionDetector.detectVersionFromPath(path);
        assertEquals(Optional.of(VersionFlag.V7), result);
    }

    @Test
    void detectVersionFromPath_pathWithMultipleSupported_returnsAnySupportedFlag() {
        Path path = Paths.get("draft4", "draft7");
        Optional<VersionFlag> result = SpecVersionDetector.detectVersionFromPath(path);
        assertTrue(result.isPresent());
        assertTrue(result.get() == VersionFlag.V4 || result.get() == VersionFlag.V7);
    }

    @Test
    void detectVersionFromPath_pathWithNullElement_throwsNullPointerException() {
        // Build a Spliterator that yields a single null element
        Iterator<Path> iterator = new Iterator<>() {
            private boolean delivered = false;
            @Override public boolean hasNext() { return !delivered; }
            @Override public Path next() { delivered = true; return null; }
        };
        Spliterator<Path> spliterator = Spliterators.spliteratorUnknownSize(iterator, 0);

        Path path = mock(Path.class);
        when(path.spliterator()).thenReturn(spliterator);

        assertThrows(NullPointerException.class,
                () -> SpecVersionDetector.detectVersionFromPath(path));
    }

    @Test
    void detectVersionFromPath_nullPath_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> SpecVersionDetector.detectVersionFromPath(null));
    }

    @Test
    void detectVersionFromPath_pathWithEmptyString_returnsEmptyOptional() {
        // Single element whose toString() is the empty string
        Path emptyStringElement = mock(Path.class);
        when(emptyStringElement.toString()).thenReturn("");

        Spliterator<Path> spliterator = Spliterators.spliterator(
                Collections.singletonList(emptyStringElement).iterator(),
                0);

        Path path = mock(Path.class);
        when(path.spliterator()).thenReturn(spliterator);

        Optional<VersionFlag> result = SpecVersionDetector.detectVersionFromPath(path);
        assertEquals(Optional.empty(), result);
    }

    @Test
    void detectVersionFromPath_pathWithCaseMismatchSupported_returnsEmptyOptional() {
        Path path = Paths.get("DRAFT4"); // map only stores lower‑case keys
        Optional<VersionFlag> result = SpecVersionDetector.detectVersionFromPath(path);
        assertEquals(Optional.empty(), result);
    }

    @Test
    void detectVersionFromPath_pathWithManyUnsupportedThenSupported_returnsSupportedFlag() {
        Path path = Paths.get("foo", "bar", "baz", "draft2020-12");
        Optional<VersionFlag> result = SpecVersionDetector.detectVersionFromPath(path);
        assertEquals(Optional.of(VersionFlag.V202012), result);
    }

    @Test
    void detectVersionFromPath_pathWithDuplicateSupported_returnsThatFlag() {
        Path path = Paths.get("draft7", "draft7");
        Optional<VersionFlag> result = SpecVersionDetector.detectVersionFromPath(path);
        assertEquals(Optional.of(VersionFlag.V7), result);
    }

    @Test
    void detectVersionFromPath_pathWithSupportedWhenMapCleared_returnsEmptyOptional() throws Exception {
        // Clear the static map via reflection
        Field field = SpecVersionDetector.class.getDeclaredField("supportedVersions");
        field.setAccessible(true);
        Map<String, VersionFlag> saved = (Map<String, VersionFlag>) field.get(null);
        try {
            saved.clear(); // now the detector sees no versions
            Path path = Paths.get("draft4");
            Optional<VersionFlag> result = SpecVersionDetector.detectVersionFromPath(path);
            assertEquals(Optional.empty(), result);
        } finally {
            // restore map content for other tests
            saved.putAll(originalSupportedVersions);
        }
    }

    @Test
    void detectVersionFromPath_pathWhenMapNull_throwsNullPointerException() throws Exception {
        // Replace the static map with null
        Field field = SpecVersionDetector.class.getDeclaredField("supportedVersions");
        field.setAccessible(true);
        Map<String, VersionFlag> backup = (Map<String, VersionFlag>) field.get(null);
        try {
            field.set(null, null);
            Path path = Paths.get("draft4");
            assertThrows(NullPointerException.class,
                    () -> SpecVersionDetector.detectVersionFromPath(path));
        } finally {
            // Reinstate the original map to avoid side‑effects
            field.set(null, backup);
        }
    }

    @Test
    void detectVersionFromPath_pathWithSpliteratorException_propagatesRuntimeException() {
        // Spliterator that throws a RuntimeException during tryAdvance
        Spliterator<Path> badSpliterator = new Spliterator<Path>() {
            @Override public boolean tryAdvance(java.util.function.Consumer<? super Path> action) {
                throw new RuntimeException("spliterator failure");
            }
            @Override public Spliterator<Path> trySplit() { return null; }
            @Override public long estimateSize() { return Long.MAX_VALUE; }
            @Override public int characteristics() { return 0; }
        };

        Path path = mock(Path.class);
        when(path.spliterator()).thenReturn(badSpliterator);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> SpecVersionDetector.detectVersionFromPath(path));
        assertEquals("spliterator failure", ex.getMessage());
    }
}
```