```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchemaFactory#checkVersion(SpecVersion.VersionFlag)}.
 *
 * The tests cover:
 * <ul>
 *   <li>Null argument – the method should return {@code null}.</li>
 *   <li>Each supported {@code VersionFlag} – the method must return the
 *       corresponding {@link JsonSchemaVersion} implementation.</li>
 * </ul>
 *
 * No external resources or private members of {@code JsonSchemaFactory} are
 * accessed. The test class follows the provided template with empty lifecycle
 * methods that can be extended later if needed.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_checkVersion_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global test fixtures required for this static method.
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

    @Test
    void checkVersion_nullFlag_returnsNull() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(null);
        assertNull(result, "checkVersion(null) should return null");
    }

    @Test
    void checkVersion_V202012_returnsVersion202012() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(VersionFlag.V202012);
        assertNotNull(result, "Result must not be null for V202012");
        assertTrue(result instanceof JsonSchemaFactory.Version202012,
                "Result should be an instance of Version202012");
    }

    @Test
    void checkVersion_V201909_returnsVersion201909() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(VersionFlag.V201909);
        assertNotNull(result, "Result must not be null for V201909");
        assertTrue(result instanceof JsonSchemaFactory.Version201909,
                "Result should be an instance of Version201909");
    }

    @Test
    void checkVersion_V7_returnsVersion7() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(VersionFlag.V7);
        assertNotNull(result, "Result must not be null for V7");
        assertTrue(result instanceof JsonSchemaFactory.Version7,
                "Result should be an instance of Version7");
    }

    @Test
    void checkVersion_V6_returnsVersion6() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(VersionFlag.V6);
        assertNotNull(result, "Result must not be null for V6");
        assertTrue(result instanceof JsonSchemaFactory.Version6,
                "Result should be an instance of Version6");
    }

    @Test
    void checkVersion_V4_returnsVersion4() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(VersionFlag.V4);
        assertNotNull(result, "Result must not be null for V4");
        assertTrue(result instanceof JsonSchemaFactory.Version4,
                "Result should be an instance of Version4");
    }

    @Test
    void checkVersion_unsupportedFlag_throwsException() {
        // Create a dummy enum constant via an anonymous subclass – this is the only
        // realistic way to reach the default branch without modifying the library.
        VersionFlag unsupported = Enum.valueOf(VersionFlag.class, "V9999");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> JsonSchemaFactory.checkVersion(unsupported));
        assertTrue(ex.getMessage().contains("Unsupported value"),
                "Exception message should indicate unsupported value");
    }
}
```