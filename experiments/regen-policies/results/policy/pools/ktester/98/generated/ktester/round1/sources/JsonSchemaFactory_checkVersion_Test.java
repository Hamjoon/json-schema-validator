package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import static org.mockito.Mockito.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_checkVersion_Test {

    private static Stream<Arguments> supportedFlagsProvider() {
        return Stream.of(Arguments.of(VersionFlag.V202012, Version202012.class), Arguments.of(VersionFlag.V201909, Version201909.class), Arguments.of(VersionFlag.V7, Version7.class), Arguments.of(VersionFlag.V6, Version6.class), Arguments.of(VersionFlag.V4, Version4.class));
    }

    @Test
    void checkVersion_nullFlag_returnsNull() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(null);
        assertNull(result, "checkVersion(null) should return null");
    }

    @Test
    void checkVersion_V202012_returnsVersion202012() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(VersionFlag.V202012);
        assertNotNull(result, "Result must not be null for V202012");
        assertTrue(result instanceof JsonSchemaFactory.Version202012, "Result should be an instance of Version202012");
    }

    @Test
    void checkVersion_V201909_returnsVersion201909() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(VersionFlag.V201909);
        assertNotNull(result, "Result must not be null for V201909");
        assertTrue(result instanceof JsonSchemaFactory.Version201909, "Result should be an instance of Version201909");
    }

    @Test
    void checkVersion_V7_returnsVersion7() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(VersionFlag.V7);
        assertNotNull(result, "Result must not be null for V7");
        assertTrue(result instanceof JsonSchemaFactory.Version7, "Result should be an instance of Version7");
    }

    @Test
    void checkVersion_V6_returnsVersion6() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(VersionFlag.V6);
        assertNotNull(result, "Result must not be null for V6");
        assertTrue(result instanceof JsonSchemaFactory.Version6, "Result should be an instance of Version6");
    }

    @Test
    void checkVersion_V4_returnsVersion4() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(VersionFlag.V4);
        assertNotNull(result, "Result must not be null for V4");
        assertTrue(result instanceof JsonSchemaFactory.Version4, "Result should be an instance of Version4");
    }

    @Test
    void checkVersion_unsupportedFlag_throwsException() {
        // Create a dummy enum constant via an anonymous subclass – this is the only
        // realistic way to reach the default branch without modifying the library.
        VersionFlag unsupported = Enum.valueOf(VersionFlag.class, "V9999");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> JsonSchemaFactory.checkVersion(unsupported));
        assertTrue(ex.getMessage().contains("Unsupported value"), "Exception message should indicate unsupported value");
    }

    @ParameterizedTest(name = "checkVersion({0}) returns instance of {1}")
    @MethodSource("supportedFlagsProvider")
    void checkVersion_supportedFlag_returnsCorrectInstance(VersionFlag flag, Class<? extends JsonSchemaVersion> expectedClass) {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(flag);
        assertNotNull(result, "Result must not be null for flag " + flag);
        assertTrue(expectedClass.isInstance(result), () -> "Result should be an instance of " + expectedClass.getSimpleName());
        // also verify it implements the JsonSchemaVersion interface
        assertTrue(result instanceof JsonSchemaVersion, "Result must implement JsonSchemaVersion");
    }

    @ParameterizedTest(name = "checkVersion({0}) returns distinct instances on successive calls")
    @MethodSource("supportedFlagsProvider")
    void checkVersion_multipleCalls_returnDistinctInstances(VersionFlag flag, Class<? extends JsonSchemaVersion> expectedClass) {
        JsonSchemaVersion first = JsonSchemaFactory.checkVersion(flag);
        JsonSchemaVersion second = JsonSchemaFactory.checkVersion(flag);
        assertNotSame(first, second, "Two invocations with the same flag should produce different objects");
        assertTrue(expectedClass.isInstance(first));
        assertTrue(expectedClass.isInstance(second));
    }

    @Test
    void checkVersion_unsupportedFlag_throwsIllegalArgumentException() {
        // Create a mock enum constant; it will not match any case in the switch.
        VersionFlag unsupported = mock(VersionFlag.class);
        // Stub toString() so that the exception message contains a predictable name.
        when(unsupported.toString()).thenReturn("UNSUPPORTED");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> JsonSchemaFactory.checkVersion(unsupported), "Unsupported flag should trigger IllegalArgumentException");
        String expectedMessage = "Unsupported valueUNSUPPORTED";
        assertEquals(expectedMessage, ex.getMessage(), "Exception message must be exactly: \"" + expectedMessage + "\"");
    }

    @Test
    void checkVersion_unsupportedFlag_messageContainsFlagName() {
        VersionFlag unsupported = mock(VersionFlag.class);
        when(unsupported.toString()).thenReturn("UNKNOWN_FLAG");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> JsonSchemaFactory.checkVersion(unsupported));
        assertTrue(ex.getMessage().contains("Unsupported valueUNKNOWN_FLAG"), "Message should contain the concatenated text \"Unsupported value\" followed by the flag name");
    }
}
