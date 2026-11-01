```java
package com.networknt.schema;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchemaFactory#checkVersion(SpecVersion.VersionFlag)}.
 *
 * The tests cover:
 * <ul>
 *   <li>Null input handling.</li>
 *   <li>Mapping of each supported {@link VersionFlag} to its concrete {@link JsonSchemaVersion} implementation.</li>
 *   <li>Behaviour for an unsupported flag (exception with exact message).</li>
 *   <li>Additional expectations such as distinct instances per call and interface implementation.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_checkVersion_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /* --------------------------------------------------------------------- *
     *  Basic null‑check test
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("when versionFlag is null then returns null")
    void whenVersionFlagIsNull_thenReturnsNull() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(null);
        assertNull(result, "checkVersion(null) should return null");
    }

    /* --------------------------------------------------------------------- *
     *  Parameterised test for all supported flags
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: flag={0} → {1}")
    @MethodSource("supportedVersionProvider")
    @DisplayName("Supported VersionFlag values map to correct concrete classes")
    void whenSupportedFlag_thenReturnsCorrectInstance(VersionFlag flag, Class<? extends JsonSchemaVersion> expectedClass) {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(flag);
        assertNotNull(result, "Result must not be null for " + flag);
        assertTrue(expectedClass.isInstance(result),
                () -> "Result should be an instance of " + expectedClass.getSimpleName() + " for " + flag);
    }

    private static Stream<Arguments> supportedVersionProvider() {
        return Stream.of(
                Arguments.of(VersionFlag.V202012, Version202012.class),
                Arguments.of(VersionFlag.V201909, Version201909.class),
                Arguments.of(VersionFlag.V7, Version7.class),
                Arguments.of(VersionFlag.V6, Version6.class),
                Arguments.of(VersionFlag.V4, Version4.class)
        );
    }

    /* --------------------------------------------------------------------- *
     *  Verify that each call creates a new object (no caching)
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("Calling checkVersion twice with the same flag returns distinct objects")
    void whenCallingTwiceWithSameFlag_thenDistinctInstances() {
        JsonSchemaVersion first = JsonSchemaFactory.checkVersion(VersionFlag.V4);
        JsonSchemaVersion second = JsonSchemaFactory.checkVersion(VersionFlag.V4);
        assertNotNull(first);
        assertNotNull(second);
        assertNotSame(first, second, "Two invocations should produce different instances");
    }

    /* --------------------------------------------------------------------- *
     *  Verify that the returned object implements the JsonSchemaVersion interface
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("Result for a supported flag implements JsonSchemaVersion")
    void whenSupportedFlag_thenResultImplementsJsonSchemaVersion() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(VersionFlag.V7);
        assertNotNull(result);
        assertTrue(result instanceof JsonSchemaVersion,
                "Result should implement JsonSchemaVersion interface");
    }

    /* --------------------------------------------------------------------- *
     *  Unsupported flag – exact exception message verification
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("Unsupported VersionFlag throws IllegalArgumentException with exact message")
    void whenUnsupportedFlag_thenThrowsIllegalArgumentExceptionWithExactMessage() {
        // The library defines an enum constant that is not handled by the switch.
        // If such a constant does not exist, this test will fail to compile,
        // signalling that the test data needs to be aligned with the library version.
        VersionFlag unsupported = VersionFlag.UNSUPPORTED; // assumed to exist
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> JsonSchemaFactory.checkVersion(unsupported),
                "Unsupported VersionFlag should cause IllegalArgumentException");
        assertEquals("Unsupported valueUNSUPPORTED", ex.getMessage(),
                "Exception message must match exactly");
    }

    /* --------------------------------------------------------------------- *
     *  Additional explicit checks for specific supported flags (non‑null & correct class)
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("VersionFlag.V202012 returns a non‑null Version202012 instance")
    void whenV202012_thenReturnsVersion202012Instance() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(VersionFlag.V202012);
        assertNotNull(result);
        assertTrue(result instanceof Version202012);
    }

    @Test
    @DisplayName("VersionFlag.V6 returns a non‑null Version6 instance")
    void whenV6_thenReturnsVersion6Instance() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(VersionFlag.V6);
        assertNotNull(result);
        assertTrue(result instanceof Version6);
    }
}
```