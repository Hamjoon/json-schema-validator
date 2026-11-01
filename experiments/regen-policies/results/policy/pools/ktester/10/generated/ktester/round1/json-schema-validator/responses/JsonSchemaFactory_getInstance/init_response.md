```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for the static {@link JsonSchemaFactory#getInstance(SpecVersion.VersionFlag)} method.
 * The method is a simple factory entry‑point – it should always return a non‑null
 * {@link JsonSchemaFactory} instance for a valid {@link SpecVersion.VersionFlag}.
 *
 * No external resources or private members are accessed; the test works solely
 * with the public API.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getInstance_Test {

    /** Runs once before all tests – no global initialisation required. */
    @BeforeAll
    static void setupBeforeAll() {
        // No static resources to initialise for this test class.
    }

    /** Runs before each test – also empty because the method under test is static
     * and does not depend on mutable state. */
    @BeforeEach
    void setupBeforeEach() {
        // Nothing to set up per‑test.
    }

    /** Runs after each test – clean‑up placeholder. */
    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up.
    }

    /** Runs once after all tests – clean‑up placeholder. */
    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up.
    }

    /**
     * Verify that {@code getInstance} returns a non‑null factory for a supported
     * version flag (e.g., V7). The returned object must be an instance of
     * {@link JsonSchemaFactory}.
     */
    @Test
    void getInstance_returnsNonNullFactory() {
        // given
        SpecVersion.VersionFlag versionFlag = SpecVersion.VersionFlag.V7; // any supported flag

        // when
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(versionFlag);

        // then
        assertNotNull(factory, "JsonSchemaFactory.getInstance should not return null");
        assertTrue(factory instanceof JsonSchemaFactory,
                "Returned object should be an instance of JsonSchemaFactory");
    }

    /**
     * Verify that calling {@code getInstance} with {@code null} throws a
     * {@link NullPointerException}. The implementation validates the argument,
     * and the contract of the public API expects a non‑null flag.
     */
    @Test
    void getInstance_nullVersionFlag_throwsNPE() {
        // given
        SpecVersion.VersionFlag versionFlag = null;

        // when / then
        assertThrows(NullPointerException.class,
                () -> JsonSchemaFactory.getInstance(versionFlag),
                "Calling getInstance with null should throw NullPointerException");
    }

    /**
     * Ensure that two consecutive calls with the same version flag produce
     * distinct factory instances (the factory does not cache instances globally).
     * This behaviour is verified by reference inequality.
     */
    @Test
    void getInstance_multipleCalls_returnDistinctInstances() {
        // given
        SpecVersion.VersionFlag versionFlag = SpecVersion.VersionFlag.V4;

        // when
        JsonSchemaFactory first = JsonSchemaFactory.getInstance(versionFlag);
        JsonSchemaFactory second = JsonSchemaFactory.getInstance(versionFlag);

        // then
        assertNotSame(first, second,
                "Each call to getInstance should return a new JsonSchemaFactory instance");
    }
}
```