package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.lang.reflect.Constructor;
import java.util.stream.Stream;

class JsonSchemaFactory_checkVersion_Test {

    /**
     * Creates a synthetic {@code VersionFlag} enum constant that is not part of the
     * original enum definition. This allows us to trigger the {@code default} branch
     * of the switch statement in {@link JsonSchemaFactory#checkVersion}.
     */
    private static SpecVersion.VersionFlag createUnsupportedFlag() {
        try {
            java.lang.reflect.Constructor<SpecVersion.VersionFlag> ctor = SpecVersion.VersionFlag.class.getDeclaredConstructor(long.class, String.class);
            ctor.setAccessible(true);
            // Use a value that does not clash with any existing enum constant.
            return ctor.newInstance(Long.MAX_VALUE, "UNSUPPORTED");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create synthetic VersionFlag for testing", e);
        }
    }

    private static Stream<Arguments> supportedFlagProvider() {
        return Stream.of(Arguments.of(SpecVersion.VersionFlag.V202012, Version202012.class), Arguments.of(SpecVersion.VersionFlag.V201909, Version201909.class), Arguments.of(SpecVersion.VersionFlag.V7, Version7.class), Arguments.of(SpecVersion.VersionFlag.V6, Version6.class), Arguments.of(SpecVersion.VersionFlag.V4, Version4.class));
    }

    @Test
    void checkVersion_nullArgument_returnsNull() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(null);
        assertNull(result, "When the version flag is null the method should return null");
    }

    @Test
    void checkVersion_V202012_returnsVersion202012() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V202012);
        assertNotNull(result, "Result must not be null for V202012");
        assertTrue(result instanceof Version202012, "Result should be an instance of Version202012 for V202012 flag");
    }

    @Test
    void checkVersion_V201909_returnsVersion201909() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V201909);
        assertNotNull(result, "Result must not be null for V201909");
        assertTrue(result instanceof Version201909, "Result should be an instance of Version201909 for V201909 flag");
    }

    @Test
    void checkVersion_V7_returnsVersion7() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7);
        assertNotNull(result, "Result must not be null for V7");
        assertTrue(result instanceof Version7, "Result should be an instance of Version7 for V7 flag");
    }

    @Test
    void checkVersion_V6_returnsVersion6() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V6);
        assertNotNull(result, "Result must not be null for V6");
        assertTrue(result instanceof Version6, "Result should be an instance of Version6 for V6 flag");
    }

    @Test
    void checkVersion_V4_returnsVersion4() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4);
        assertNotNull(result, "Result must not be null for V4");
        assertTrue(result instanceof Version4, "Result should be an instance of Version4 for V4 flag");
    }

    @Test
    void checkVersion_unsupportedFlag_throwsIllegalArgumentException() {
        // The enum currently defines all supported values; to simulate an unsupported value,
        // we use reflection to create a synthetic enum constant. This is safe for unit‑test
        // purposes because the constant is never exposed outside the test.
        SpecVersion.VersionFlag unsupported = createUnsupportedFlag();
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> JsonSchemaFactory.checkVersion(unsupported), "Unsupported flag should cause IllegalArgumentException");
        assertTrue(thrown.getMessage().contains("Unsupported value"), "Exception message should indicate unsupported value");
    }

    @ParameterizedTest(name = "[{index}] flag={0} → {1}")
    @MethodSource("supportedFlagProvider")
    void checkVersion_supportedFlags_returnCorrectInstance(SpecVersion.VersionFlag flag, Class<? extends JsonSchemaVersion> expectedClass) {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(flag);
        assertNotNull(result, "Result must not be null for flag " + flag);
        assertTrue(expectedClass.isInstance(result), () -> "Result should be an instance of " + expectedClass.getSimpleName() + " for flag " + flag);
    }

    @ParameterizedTest(name = "[{index}] flag={0} – distinct instances")
    @MethodSource("supportedFlagProvider")
    void checkVersion_instancesAreDistinct(SpecVersion.VersionFlag flag, Class<? extends JsonSchemaVersion> ignored) {
        JsonSchemaVersion first = JsonSchemaFactory.checkVersion(flag);
        JsonSchemaVersion second = JsonSchemaFactory.checkVersion(flag);
        assertNotSame(first, second, () -> "Two calls with the same flag (" + flag + ") must return distinct objects");
    }
}
