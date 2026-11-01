```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchema#getTypeValidator()}.
 *
 * <p>The tests verify that the method returns the appropriate {@link TypeValidator}
 * instance when a schema defines a {@code type} keyword and returns {@code null}
 * when the keyword is absent. They also check that the lazy‑initialisation logic
 * (i.e. calling {@code getValidators()} when {@code validators == null}) works
 * correctly.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getTypeValidator_Test {

    /** Simple schema that defines a primitive type – should yield a {@link TypeValidator}. */
    private static final String SCHEMA_WITH_TYPE = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "string"
            }
            """;

    /** Schema that does **not** define a {@code type} keyword – should yield {@code null}. */
    private static final String SCHEMA_WITHOUT_TYPE = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "properties": {
                "name": { "type": "string" }
              }
            }
            """;

    /** The factory used to create {@link JsonSchema} instances for the tests. */
    private JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh factory for each test – the factory is cheap and
        // guarantees a clean validation context.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the factory does not hold external resources.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    @Test
    void getTypeValidator_returnsNonNullWhenSchemaHasType() {
        // Arrange: obtain a JsonSchema that contains a top‑level "type" keyword.
        JsonSchema schema = factory.getSchema(SCHEMA_WITH_TYPE);

        // Act: invoke the method under test.
        TypeValidator typeValidator = schema.getTypeValidator();

        // Assert: a TypeValidator must be present and correctly initialised.
        assertNotNull(typeValidator, "Expected a non‑null TypeValidator for a schema with a 'type' keyword");
        // The validator should be of the concrete class TypeValidator (not a subclass).
        assertEquals(TypeValidator.class, typeValidator.getClass(),
                "Expected the validator to be an instance of TypeValidator");
        // Subsequent calls must return the same instance (lazy loading caches the result).
        assertSame(typeValidator, schema.getTypeValidator(),
                "Subsequent calls to getTypeValidator() should return the cached instance");
    }

    @Test
    void getTypeValidator_returnsNullWhenSchemaHasNoType() {
        // Arrange: obtain a JsonSchema that does NOT contain a top‑level "type" keyword.
        JsonSchema schema = factory.getSchema(SCHEMA_WITHOUT_TYPE);

        // Act: invoke the method under test.
        TypeValidator typeValidator = schema.getTypeValidator();

        // Assert: the method should return null because no type validator was created.
        assertNull(typeValidator, "Expected null TypeValidator for a schema without a top‑level 'type' keyword");
    }
}
```