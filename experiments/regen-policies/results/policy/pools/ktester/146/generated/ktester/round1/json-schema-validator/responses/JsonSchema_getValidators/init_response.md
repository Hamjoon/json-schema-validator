```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchema#getValidators()}.
 *
 * The tests verify that:
 * <ul>
 *   <li>The returned list is never {@code null}.</li>
 *   <li>The list is unmodifiable (attempting to modify it throws
 *       {@link UnsupportedOperationException}).</li>
 *   <li>The same list instance is returned on subsequent calls (caching).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getValidators_Test {

    private JsonSchemaFactory factory;
    private JsonSchema schema;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a factory for a recent draft (the exact version is not important for this test).
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // A minimal schema that will produce at least one validator (type validator).
        String minimalSchema = "{\"type\":\"object\"}";
        schema = factory.getSchema(minimalSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the factory and schema are lightweight.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    @Test
    void getValidators_returnsUnmodifiableAndCached() {
        // First invocation – should initialise the validator list.
        List<JsonValidator> first = schema.getValidators();
        assertNotNull(first, "Validators list must not be null on first call");

        // The list returned by JsonSchema is expected to be unmodifiable.
        assertThrows(UnsupportedOperationException.class,
                () -> first.add(null),
                "The validators list should be unmodifiable");

        // Subsequent invocation – should return the same (cached) list instance.
        List<JsonValidator> second = schema.getValidators();
        assertSame(first, second, "Subsequent calls must return the same list instance");
    }
}
```