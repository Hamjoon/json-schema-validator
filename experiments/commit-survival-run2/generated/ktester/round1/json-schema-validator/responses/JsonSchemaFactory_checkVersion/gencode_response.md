```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import java.lang.reflect.Constructor;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchemaFactory#checkVersion(SpecVersion.VersionFlag)}.
 *
 * The test suite covers:
 * <ul>
 *   <li>Guard clause – null argument returns {@code null}.</li>
 *   <li>Mapping of each supported {@code VersionFlag} to the correct {@link JsonSchemaVersion} implementation.</li>
 *   <li>Verification that two subsequent calls produce distinct objects (no caching).</li>
 *   <li>Behaviour for an unsupported enum constant – {@link IllegalArgumentException} with a proper message.</li>
 * </ul>
 *
 * No external dependencies are required, therefore no Mockito usage is necessary.
 */
class JsonSchemaFactory_checkVersion_Test {

    /*======================================================================
     *  Test fixtures (none needed for static method)
     *======================================================================*/

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these static tests
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

    /*======================================================================
     *  Guard clause
     *======================================================================*/

    @Test
    void checkVersion_nullArgument_returnsNull() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(null);
        assertNull(result, "When the version flag is null the method should return null");
    }

    /*======================================================================
     *  Supported flags – parameterised test for correctness of returned type
     *======================================================================*/

    @ParameterizedTest(name = "[{index}] flag={0} → {1}")
    @MethodSource("supportedFlagProvider")
    void checkVersion_supportedFlags_returnCorrectInstance(SpecVersion.VersionFlag flag,
                                                            Class<? extends JsonSchemaVersion> expectedClass) {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(flag);
        assertNotNull(result, "Result must not be null for flag " + flag);
        assertTrue(expectedClass.isInstance(result),
                () -> "Result should be an instance of " + expectedClass.getSimpleName() + " for flag " + flag);
    }

    private static Stream<Arguments> supportedFlagProvider() {
        return Stream.of(
                Arguments.of(SpecVersion.VersionFlag.V202012, Version202012.class),
                Arguments.of(SpecVersion.VersionFlag.V201909, Version201909.class),
                Arguments.of(SpecVersion.VersionFlag.V7, Version7.class),
                Arguments.of(SpecVersion.VersionFlag.V6, Version6.class),
                Arguments.of(SpecVersion.VersionFlag.V4, Version4.class)
        );
    }

    /*======================================================================
     *  Instance uniqueness – each call must create a new object
     *======================================================================*/

    @ParameterizedTest(name = "[{index}] flag={0} – distinct instances")
    @MethodSource("supportedFlagProvider")
    void checkVersion_instancesAreDistinct(SpecVersion.VersionFlag flag,
                                           Class<? extends JsonSchemaVersion> ignored) {
        JsonSchemaVersion first = JsonSchemaFactory.checkVersion(flag);
        JsonSchemaVersion second = JsonSchemaFactory.checkVersion(flag);
        assertNotSame(first, second,
                () -> "Two calls with the same flag (" + flag + ") must return distinct objects");
    }

    /*======================================================================
     *  Unsupported flag – default branch of the switch
     *======================================================================*/

    @Test
    void checkVersion_unsupportedFlag_throwsIllegalArgumentException() {
        SpecVersion.VersionFlag unsupported = createUnsupportedFlag();

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> JsonSchemaFactory.checkVersion(unsupported),
                "Unsupported flag should cause IllegalArgumentException");

        // The message must contain the enum constant name we created (UNSUPPORTED)
        assertTrue(thrown.getMessage().contains(unsupported.name()),
                () -> "Exception message should contain the unsupported enum constant name: " + unsupported.name());
    }

    /**
     * Creates a synthetic {@code VersionFlag} enum constant that is not part of the original enum.
     * This forces the {@code default} branch of {@code JsonSchemaFactory.checkVersion}
     * to be exercised.
     */
    private static SpecVersion.VersionFlag createUnsupportedFlag() {
        try {
            Constructor<SpecVersion.VersionFlag> ctor =
                    SpecVersion.VersionFlag.class.getDeclaredConstructor(long.class, String.class);
            ctor.setAccessible(true);
            // Use a value that does not clash with any existing enum constant.
            return ctor.newInstance(Long.MAX_VALUE, "UNSUPPORTED");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create synthetic VersionFlag for testing", e);
        }
    }
}
```