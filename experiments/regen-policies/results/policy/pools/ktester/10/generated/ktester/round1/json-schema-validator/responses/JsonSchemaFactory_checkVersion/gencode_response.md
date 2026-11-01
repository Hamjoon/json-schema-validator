```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#checkVersion(SpecVersion.VersionFlag)}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Null input – should return {@code null}.</li>
 *   <li>All supported {@link SpecVersion.VersionFlag} values – each must return an
 *       instance of the corresponding {@link JsonSchemaVersion} implementation.</li>
 *   <li>Any enum value that is not part of the {@code switch} – should trigger
 *       {@link IllegalArgumentException}. This is simulated by mocking an enum
 *       constant (requires Mockito‑inline to mock final classes/enums).</li>
 * </ul>
 *
 * <p>The test class follows the provided framework and does not modify the
 * {@code @BeforeAll}, {@code @BeforeEach}, {@code @AfterEach} or {@code @AfterAll}
 * methods.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_checkVersion_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
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

    /* --------------------------------------------------------------------- */
    /* 1. Null input                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    void whenVersionFlagIsNull_thenReturnsNull() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(null);
        assertNull(result, "checkVersion(null) should return null");
    }

    /* --------------------------------------------------------------------- */
    /* 2. Supported flags – parameterized test                               */
    /* --------------------------------------------------------------------- */

    /**
     * Provides each supported {@link SpecVersion.VersionFlag} together with the
     * expected concrete {@link JsonSchemaVersion} class.
     */
    private static Stream<Arguments> supportedFlagsProvider() {
        return Stream.of(
                Arguments.of(SpecVersion.VersionFlag.V202012, Version202012.class),
                Arguments.of(SpecVersion.VersionFlag.V201909, Version201909.class),
                Arguments.of(SpecVersion.VersionFlag.V7, Version7.class),
                Arguments.of(SpecVersion.VersionFlag.V6, Version6.class),
                Arguments.of(SpecVersion.VersionFlag.V4, Version4.class)
        );
    }

    @ParameterizedTest(name = "whenVersionFlagIs{0}_thenReturns{1}")
    @MethodSource("supportedFlagsProvider")
    void whenVersionFlagIsSupported_thenReturnsCorrespondingVersion(
            SpecVersion.VersionFlag flag,
            Class<? extends JsonSchemaVersion> expectedClass) {

        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(flag);

        assertNotNull(result, "Result must not be null for flag " + flag);
        assertEquals(expectedClass, result.getClass(),
                "Expected a " + expectedClass.getSimpleName() + " instance for flag " + flag);
    }

    /* --------------------------------------------------------------------- */
    /* 3. Unsupported flag – simulate with a mocked enum constant            */
    /* --------------------------------------------------------------------- */

    @Test
    void whenVersionFlagIsUnsupported_thenThrowsIllegalArgumentException() {
        // Mockito can mock final enums when the inline mock maker is on the classpath.
        // The mock does not equal any of the real enum constants, therefore the
        // default branch of the switch statement is executed.
        SpecVersion.VersionFlag mockedFlag = Mockito.mock(SpecVersion.VersionFlag.class);

        // The mock's toString() is used only for the exception message; we give it a
        // readable name to make debugging easier.
        Mockito.when(mockedFlag.toString()).thenReturn("MOCKED_UNSUPPORTED_FLAG");

        assertThrows(IllegalArgumentException.class,
                () -> JsonSchemaFactory.checkVersion(mockedFlag),
                "Unsupported enum values should cause IllegalArgumentException");
    }
}
```