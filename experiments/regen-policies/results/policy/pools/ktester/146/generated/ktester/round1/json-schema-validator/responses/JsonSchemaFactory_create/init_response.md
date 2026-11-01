```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.util.Collections;

/**
 * Unit tests for {@link JsonSchemaFactory#create(ValidationContext, SchemaLocation, JsonNodePath, JsonNode, JsonSchema)}.
 */
class JsonSchemaFactory_create_Test {

    private static JsonSchemaFactory factory;
    private ValidationContext validationContext;
    private SchemaLocation schemaLocation;
    private JsonNodePath evaluationPath;
    private JsonNode schemaNode;
    private JsonSchema parentSchema;

    @BeforeAll
    static void setupBeforeAll() {
        /* Use the public factory method – it builds a fully‑initialised instance
         * with all required meta‑schemas, mappers and loaders. */
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /* --------------------------------------------------------------
         * Prepare the arguments required by the focal method.
         *
         * 1. ValidationContext – needs a JsonMetaSchema, the factory itself
         *    and a (possibly default) SchemaValidatorsConfig.
         * 2. SchemaLocation – a simple location; the constructor accepts a
         *    nullable AbsoluteIri, so we can safely pass null for the purpose of
         *    this test.
         * 3. EvaluationPath – not needed for the behaviour we verify, therefore
         *    we pass null.
         * 4. SchemaNode – a minimal JSON schema (type:string) parsed with the
         *    factory's ObjectMapper.
         * 5. ParentSchema – not required for this test, so null.
         * -------------------------------------------------------------- */
        JsonMetaSchema metaSchema = factory.getMetaSchema(
                "https://json-schema.org/draft/2020-12/schema",
                new SchemaValidatorsConfig());

        validationContext = new ValidationContext(
                metaSchema,
                factory,
                new SchemaValidatorsConfig());

        // a dummy location; the concrete value is irrelevant for the test
        schemaLocation = new SchemaLocation(null);

        // evaluationPath is optional for this test
        evaluationPath = null;

        // simple schema: {"type":"string"}
        ObjectMapper mapper = factory.getObjectMapper();
        schemaNode = mapper.readTree("{\"type\":\"string\"}");

        parentSchema = null;
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up for this test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    @Test
    void create_ShouldReturnNonNullJsonSchema() {
        JsonSchema result = factory.create(
                validationContext,
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema);

        assertNotNull(result, "The create method should return a non‑null JsonSchema instance");
        // Basic sanity check – the returned object must be an instance of JsonSchema
        assertTrue(result instanceof JsonSchema, "Result must be a JsonSchema");
    }

    @Test
    void create_WithSameParameters_ShouldReturnDistinctInstances() {
        JsonSchema first = factory.create(
                validationContext,
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema);
        JsonSchema second = factory.create(
                validationContext,
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema);

        assertNotNull(first);
        assertNotNull(second);
        // Two invocations with the same inputs should produce two distinct objects
        assertNotSame(first, second, "Each call to create should produce a new JsonSchema instance");
    }

    @Test
    void create_WithInvalidSchemaNode_ShouldThrowException() {
        // An invalid JSON schema (e.g., a non‑object node) should cause the factory
        // to fail while constructing the internal JsonSchema.
        JsonNode invalidNode = factory.getObjectMapper().readTree("\"just a string\"");

        assertThrows(RuntimeException.class, () -> {
            factory.create(
                    validationContext,
                    schemaLocation,
                    evaluationPath,
                    invalidNode,
                    parentSchema);
        }, "Creating a JsonSchema from an invalid schema node should result in an exception");
    }
}
```