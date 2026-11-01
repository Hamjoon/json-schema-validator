```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchema#getValidators()}.
 *
 * <p>This test class verifies that:
 * <ul>
 *   <li>The returned list is never {@code null}.</li>
 *   <li>The list is unmodifiable (attempting to modify it throws {@link UnsupportedOperationException}).</li>
 *   <li>Repeated calls return the same cached instance.</li>
 *   <li>A schema containing a property yields at least one validator.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getValidators_Test {

    private static JsonSchemaFactory factory;
    private JsonSchema schema;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a recent draft version – the concrete version is not important for the test.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // A minimal but valid JSON schema that defines an object with a single string property.
        String schemaData = "{"
                + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "   \"name\":{"
                + "       \"type\":\"string\""
                + "   }"
                + "}"
                + "}";
        // The factory creates a JsonSchema instance that internally builds its validators.
        schema = factory.getSchema(schemaData);
        assertNotNull(schema, "Factory should return a non‑null JsonSchema instance");
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the test tidy.
        schema = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        factory = null;
    }

    @Test
    void getValidatorsReturnsNonNullAndUnmodifiableList() {
        List<JsonValidator> validators = schema.getValidators();

        // The list must be non‑null.
        assertNotNull(validators, "getValidators() must never return null");

        // The list must be unmodifiable – adding an element should throw.
        assertThrows(UnsupportedOperationException.class, () -> validators.add(null),
                "The validators list should be unmodifiable");
    }

    @Test
    void getValidatorsCachesResult() {
        List<JsonValidator> firstCall = schema.getValidators();
        List<JsonValidator> secondCall = schema.getValidators();

        // The same instance should be returned on subsequent calls.
        assertSame(firstCall, secondCall, "getValidators() should cache and return the same list");
    }

    @Test
    void schemaWithPropertyHasAtLeastOneValidator() {
        List<JsonValidator> validators = schema.getValidators();

        // The schema defines a property, therefore at least one validator (e.g., a PropertyValidator)
        // must be present. The exact number depends on the implementation, but it must be > 0.
        assertFalse(validators.isEmpty(),
                "A schema that defines a property should produce at least one validator");
    }
}
```