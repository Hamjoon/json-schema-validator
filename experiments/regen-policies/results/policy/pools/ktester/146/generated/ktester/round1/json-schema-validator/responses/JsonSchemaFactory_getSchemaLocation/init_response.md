```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchemaLocation(SchemaLocation)}.
 *
 * The method is {@code protected}, but because this test class resides in the same
 * package ({@code com.networknt.schema}) it can be accessed directly.
 */
class JsonSchemaFactory_getSchemaLocation_Test {

    private JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a factory instance using the public builder API.
        // The default meta‑schema configuration is sufficient for this test.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
        factory = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void getSchemaLocation_returnsSameInstanceWhenNonNull() {
        // GIVEN a non‑null SchemaLocation (use the documented DOCUMENT constant)
        SchemaLocation given = SchemaLocation.DOCUMENT;

        // WHEN the method is invoked
        SchemaLocation result = factory.getSchemaLocation(given);

        // THEN the same instance is returned
        assertSame(given, result,
                "When a non‑null SchemaLocation is supplied, the same instance should be returned.");
    }

    @Test
    void getSchemaLocation_returnsDocumentWhenNull() {
        // GIVEN a null argument
        SchemaLocation given = null;

        // WHEN the method is invoked
        SchemaLocation result = factory.getSchemaLocation(given);

        // THEN the default DOCUMENT constant is returned
        assertSame(SchemaLocation.DOCUMENT, result,
                "When null is supplied, the method should return SchemaLocation.DOCUMENT.");
    }
}
```