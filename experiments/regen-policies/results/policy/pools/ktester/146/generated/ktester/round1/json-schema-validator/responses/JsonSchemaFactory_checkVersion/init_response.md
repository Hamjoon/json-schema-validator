```java
package com.networknt.schema;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#checkVersion(SpecVersion.VersionFlag)}.
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

    @Test
    void whenVersionFlagIsNull_thenReturnsNull() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(null);
        assertNull(result, "checkVersion(null) should return null");
    }

    @Test
    void whenVersionFlagIsV202012_thenReturnsVersion202012() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(VersionFlag.V202012);
        assertNotNull(result, "Result must not be null for V202012");
        assertTrue(result instanceof Version202012,
                "Result should be an instance of Version202012 for V202012");
    }

    @Test
    void whenVersionFlagIsV201909_thenReturnsVersion201909() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(VersionFlag.V201909);
        assertNotNull(result, "Result must not be null for V201909");
        assertTrue(result instanceof Version201909,
                "Result should be an instance of Version201909 for V201909");
    }

    @Test
    void whenVersionFlagIsV7_thenReturnsVersion7() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(VersionFlag.V7);
        assertNotNull(result, "Result must not be null for V7");
        assertTrue(result instanceof Version7,
                "Result should be an instance of Version7 for V7");
    }

    @Test
    void whenVersionFlagIsV6_thenReturnsVersion6() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(VersionFlag.V6);
        assertNotNull(result, "Result must not be null for V6");
        assertTrue(result instanceof Version6,
                "Result should be an instance of Version6 for V6");
    }

    @Test
    void whenVersionFlagIsV4_thenReturnsVersion4() {
        JsonSchemaVersion result = JsonSchemaFactory.checkVersion(VersionFlag.V4);
        assertNotNull(result, "Result must not be null for V4");
        assertTrue(result instanceof Version4,
                "Result should be an instance of Version4 for V4");
    }

    @Test
    void whenVersionFlagIsUnsupported_thenThrowsIllegalArgumentException() {
        // Create a dummy enum constant that is not part of the switch.
        // Since the enum is final, we cannot instantiate a new constant.
        // Instead, we use reflection to pass a null‑safe but unknown value.
        // The simplest way is to use a mock of the enum and stub its name().
        VersionFlag unknown = mock(VersionFlag.class);
        // The switch will fall through to default and throw the exception.
        assertThrows(IllegalArgumentException.class,
                () -> JsonSchemaFactory.checkVersion(unknown),
                "Unsupported VersionFlag should cause IllegalArgumentException");
    }
}
```