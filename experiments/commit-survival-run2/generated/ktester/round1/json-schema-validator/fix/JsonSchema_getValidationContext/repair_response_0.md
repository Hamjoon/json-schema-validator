```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import java.net.URI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JsonSchema#getValidationContext()}.
 *
 * <p>This test class does not rely on Mockito; instead it creates real
 * {@link ValidationContext}, {@link JsonMetaSchema} and {@link JsonNode}
 * instances that are sufficient for exercising the behaviour of
 * {@code JsonSchema#getValidationContext()}.</p>
 */
class JsonSchema_getValidationContext_Test {

    private ValidationContext validationContext;
    private JsonMetaSchema metaSchema;
    private JsonNode schemaNode;
    private JsonSchema jsonSchema;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Use a concrete meta‑schema (V7 is a common default)
        metaSchema = JsonMetaSchema.getV7();

        // Minimal configuration – the library works with a default config instance
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        // Build the ValidationContext that will be shared across the tests
        validationContext = new ValidationContext(metaSchema, config);

        // An empty JSON object is enough for the schema node in these tests
        ObjectMapper mapper = new ObjectMapper();
        schemaNode = mapper.readTree("{}");

        URI currentUri = URI.create("http://example.com/schema");

        // Create a JsonSchema instance via the public factory method.
        jsonSchema = JsonSchema.from(validationContext,
                "testSchemaPath",
                currentUri,
                schemaNode,          // schema node
                null,               // no parent schema
                false);             // do not suppress sub‑schema retrieval
    }

    @Test
    void getValidationContext_returnsTheSameInstanceProvidedInConstructor() {
        ValidationContext result = jsonSchema.getValidationContext();
        assertSame(validationContext, result,
                "JsonSchema#getValidationContext should return the ValidationContext supplied at construction");
    }

    @Test
    void getValidationContext_onChildSchema_returnsParentContext() throws Exception {
        // Arrange – create a child schema from the parent.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode childNode = mapper.readTree("{\"type\":\"string\"}");
        JsonSchema childSchema = jsonSchema.createChildSchema("childPath", childNode);

        // Act
        ValidationContext childContext = childSchema.getValidationContext();

        // Assert – the child must expose the exact same ValidationContext instance as its parent.
        assertSame(validationContext, childContext,
                "Child JsonSchema#getValidationContext should return the same ValidationContext as its parent");
    }

    @Test
    void getValidationContext_whenFactoryCalledWithNullContext_returnsNull() throws Exception {
        // Arrange – call the static factory with a null ValidationContext.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{}");
        URI currentUri = URI.create("http://example.com/nullContext");
        JsonSchema schemaWithNullContext = JsonSchema.from(null,
                "nullPath",
                currentUri,
                node,
                null,
                false);

        // Act
        ValidationContext result = schemaWithNullContext.getValidationContext();

        // Assert – the method should simply return null and not throw.
        assertNull(result,
                "JsonSchema#getValidationContext should return null when the schema was created with a null ValidationContext");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 5, 10})
    void getValidationContext_multipleSchemas_shareSameContext(int schemaCount) throws Exception {
        // Arrange – create several independent schemas using the same ValidationContext instance.
        JsonSchema[] schemas = new JsonSchema[schemaCount];
        URI baseUri = URI.create("http://example.com/multi");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{}");

        for (int i = 0; i < schemaCount; i++) {
            schemas[i] = JsonSchema.from(validationContext,
                    "path" + i,
                    baseUri.resolve("schema" + i),
                    node,
                    null,
                    false);
        }

        // Act & Assert – each schema must return the identical ValidationContext instance.
        for (int i = 0; i < schemaCount; i++) {
            assertSame(validationContext, schemas[i].getValidationContext(),
                    "Schema #" + i + " should share the same ValidationContext instance");
        }
    }

    @Test
    void getValidationContext_immutability_noNewInstanceCreated() {
        // Act – retrieve the context twice.
        ValidationContext firstCall = jsonSchema.getValidationContext();
        ValidationContext secondCall = jsonSchema.getValidationContext();

        // Assert – both calls must return the exact same object reference.
        assertSame(firstCall, secondCall,
                "Subsequent calls to getValidationContext() should return the original instance, not a copy");
        // No further interaction verification is required; the method is expected to be a simple getter.
    }
}
```