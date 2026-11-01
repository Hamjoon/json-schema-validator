package com.networknt.schema;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_checkVersion_Test {

    private static Stream<Arguments> supportedVersionProvider() {
        return Stream.of(Arguments.of(VersionFlag.V202012, Version202012.class), Arguments.of(VersionFlag.V201909, Version201909.class), Arguments.of(VersionFlag.V7, Version7.class), Arguments.of(VersionFlag.V6, Version6.class), Arguments.of(VersionFlag.V4, Version4.class));
    }

    @Test
    void whenVersionFlagIsNull_thenReturnsNull() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(null);
        assertNull(result, "checkVersion(null) should return null");
    }

    @Test
    void whenVersionFlagIsV202012_thenReturnsVersion202012() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(VersionFlag.V202012);
        assertNotNull(result, "Result must not be null for V202012");
        assertTrue(result instanceof Version202012, "Result should be an instance of Version202012 for V202012");
    }

    @Test
    void whenVersionFlagIsV201909_thenReturnsVersion201909() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(VersionFlag.V201909);
        assertNotNull(result, "Result must not be null for V201909");
        assertTrue(result instanceof Version201909, "Result should be an instance of Version201909 for V201909");
    }

    @Test
    void whenVersionFlagIsV7_thenReturnsVersion7() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(VersionFlag.V7);
        assertNotNull(result, "Result must not be null for V7");
        assertTrue(result instanceof Version7, "Result should be an instance of Version7 for V7");
    }

    @Test
    void whenVersionFlagIsV6_thenReturnsVersion6() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(VersionFlag.V6);
        assertNotNull(result, "Result must not be null for V6");
        assertTrue(result instanceof Version6, "Result should be an instance of Version6 for V6");
    }

    @Test
    void whenVersionFlagIsV4_thenReturnsVersion4() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(VersionFlag.V4);
        assertNotNull(result, "Result must not be null for V4");
        assertTrue(result instanceof Version4, "Result should be an instance of Version4 for V4");
    }

    @Test
    void whenVersionFlagIsUnsupported_thenThrowsIllegalArgumentException() {
        // Create a dummy enum constant that is not part of the switch.
        // Since the enum is final, we cannot instantiate a new constant.
        // Instead, we use reflection to pass a null‑safe but unknown value.
        // The simplest way is to use a mock of the enum and stub its name().
        VersionFlag unknown = mock(VersionFlag.class);
        // The switch will fall through to default and throw the exception.
        assertThrows(IllegalArgumentException.class, () -> JsonSchemaFactory.checkVersion(unknown), "Unsupported VersionFlag should cause IllegalArgumentException");
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
        assertTrue(expectedClass.isInstance(result), () -> "Result should be an instance of " + expectedClass.getSimpleName() + " for " + flag);
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
        assertTrue(result instanceof JsonSchemaVersion, "Result should implement JsonSchemaVersion interface");
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
        // assumed to exist
        VersionFlag unsupported = VersionFlag.UNSUPPORTED;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> JsonSchemaFactory.checkVersion(unsupported), "Unsupported VersionFlag should cause IllegalArgumentException");
        assertEquals("Unsupported valueUNSUPPORTED", ex.getMessage(), "Exception message must match exactly");
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
