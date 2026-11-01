```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.stream.Stream;

import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.Version202012;
import com.networknt.schema.Version201909;
import com.networknt.schema.Version7;
import com.networknt.schema.Version6;
import com.networknt.schema.Version4;
import com.networknt.schema.JsonSchemaVersion;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#checkVersion(SpecVersion.VersionFlag)}.
 *
 * Covered scenarios:
 * <ul>
 *   <li>Null argument – returns {@code null}.</li>
 *   <li>All supported {@code VersionFlag} values – returns a new instance of the
 *       corresponding {@link JsonSchemaVersion} implementation.</li>
 *   <li>Each call creates a distinct object (identity test).</li>
 *   <li>Unsupported flag – {@link IllegalArgumentException} with the expected
 *       message.</li>
 *   <li>Returned object implements {@link JsonSchemaVersion}.</li>
 * </ul>
 *
 * No private members of {@code JsonSchemaFactory} are accessed.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_checkVersion_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global fixtures required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test setup required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* --------------------------------------------------------------------- */
    /*  Null handling                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    void checkVersion_nullFlag_returnsNull() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(null);
        assertNull(result, "checkVersion(null) should return null");
    }

    /* --------------------------------------------------------------------- */
    /*  Supported version flags – type & interface compliance                */
    /* --------------------------------------------------------------------- */

    private static Stream<Arguments> supportedFlagsProvider() {
        return Stream.of(
                Arguments.of(VersionFlag.V202012, Version202012.class),
                Arguments.of(VersionFlag.V201909, Version201909.class),
                Arguments.of(VersionFlag.V7,      Version7.class),
                Arguments.of(VersionFlag.V6,      Version6.class),
                Arguments.of(VersionFlag.V4,      Version4.class)
        );
    }

    @ParameterizedTest(name = "checkVersion({0}) returns instance of {1}")
    @MethodSource("supportedFlagsProvider")
    void checkVersion_supportedFlag_returnsCorrectInstance(VersionFlag flag,
                                                            Class<? extends JsonSchemaVersion> expectedClass) {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(flag);
        assertNotNull(result, "Result must not be null for flag " + flag);
        assertTrue(expectedClass.isInstance(result),
                () -> "Result should be an instance of " + expectedClass.getSimpleName());
        // also verify it implements the JsonSchemaVersion interface
        assertTrue(result instanceof JsonSchemaVersion,
                "Result must implement JsonSchemaVersion");
    }

    /* --------------------------------------------------------------------- */
    /*  Instance uniqueness – each call returns a new object                */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "checkVersion({0}) returns distinct instances on successive calls")
    @MethodSource("supportedFlagsProvider")
    void checkVersion_multipleCalls_returnDistinctInstances(VersionFlag flag,
                                                             Class<? extends JsonSchemaVersion> expectedClass) {
        JsonSchemaVersion first = JsonSchemaFactory.checkVersion(flag);
        JsonSchemaVersion second = JsonSchemaFactory.checkVersion(flag);
        assertNotSame(first, second,
                "Two invocations with the same flag should produce different objects");
        assertTrue(expectedClass.isInstance(first));
        assertTrue(expectedClass.isInstance(second));
    }

    /* --------------------------------------------------------------------- */
    /*  Unsupported flag – default branch                                    */
    /* --------------------------------------------------------------------- */

    @Test
    void checkVersion_unsupportedFlag_throwsIllegalArgumentException() {
        // Create a mock enum constant; it will not match any case in the switch.
        VersionFlag unsupported = mock(VersionFlag.class);
        // Stub toString() so that the exception message contains a predictable name.
        when(unsupported.toString()).thenReturn("UNSUPPORTED");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> JsonSchemaFactory.checkVersion(unsupported),
                "Unsupported flag should trigger IllegalArgumentException");

        String expectedMessage = "Unsupported valueUNSUPPORTED";
        assertEquals(expectedMessage, ex.getMessage(),
                "Exception message must be exactly: \"" + expectedMessage + "\"");
    }

    /* --------------------------------------------------------------------- */
    /*  Exception message detail (separate test for clarity)                */
    /* --------------------------------------------------------------------- */

    @Test
    void checkVersion_unsupportedFlag_messageContainsFlagName() {
        VersionFlag unsupported = mock(VersionFlag.class);
        when(unsupported.toString()).thenReturn("UNKNOWN_FLAG");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> JsonSchemaFactory.checkVersion(unsupported));

        assertTrue(ex.getMessage().contains("Unsupported valueUNKNOWN_FLAG"),
                "Message should contain the concatenated text \"Unsupported value\" followed by the flag name");
    }
}
```