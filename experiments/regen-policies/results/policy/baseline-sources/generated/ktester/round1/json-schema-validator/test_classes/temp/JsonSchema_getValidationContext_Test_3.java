package com.networknt.schema;

import java.net.URI;
import com.fasterxml.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getValidationContext_Test {

    @Mock
    ValidationContext validationContext;

    @Mock
    JsonMetaSchema metaSchema;

    @Mock
    JsonNode schemaNode;

    JsonSchema jsonSchema;

    @BeforeEach
    void setupBeforeEach() {
        // Use the V7 meta‑schema – the most common default.
        metaSchema = JsonMetaSchema.getV7();
        // Create a simple ValidationContext instance. The library provides a
        // public no‑arg constructor for this purpose.
        validationContext = new ValidationContext();
        // Minimal schema node – an empty JSON object is sufficient.
        ObjectMapper mapper = new ObjectMapper();
        schemaNode = mapper.readTree("{}");
        URI currentUri = URI.create("http://example.com/schema");
        // Create the JsonSchema instance via the public factory method.
        jsonSchema = JsonSchema.from(validationContext, "testSchemaPath", currentUri, schemaNode, // do not suppress sub‑schema retrieval
        null, false);
    }

    @Test
    void getValidationContext_returnsTheSameInstanceProvidedInConstructor() {
        ValidationContext result = jsonSchema.getValidationContext();
        assertSame(validationContext, result, "JsonSchema#getValidationContext should return the ValidationContext supplied at construction");
    }

    /**
     * Group: testGetValidationContextOnChildSchema **
     */
    @Test
    void getValidationContext_onChildSchema_returnsParentContext() {
        // Arrange – create a child schema from the parent.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode childNode = mapper.readTree("{\"type\":\"string\"}");
        JsonSchema childSchema = jsonSchema.createChildSchema("childPath", childNode);
        // Act
        ValidationContext childContext = childSchema.getValidationContext();
        // Assert – the child must expose the exact same ValidationContext instance as its parent.
        assertSame(validationContext, childContext, "Child JsonSchema#getValidationContext should return the same ValidationContext as its parent");
    }

    /**
     * Group: testGetValidationContextWhenNullContext **
     */
    @Test
    void getValidationContext_whenFactoryCalledWithNullContext_returnsNull() {
        // Arrange – call the static factory with a null ValidationContext.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{}");
        URI currentUri = URI.create("http://example.com/nullContext");
        JsonSchema schemaWithNullContext = JsonSchema.from(null, "nullPath", currentUri, node, null, false);
        // Act
        ValidationContext result = schemaWithNullContext.getValidationContext();
        // Assert – the method should simply return null and not throw.
        assertNull(result, "JsonSchema#getValidationContext should return null when the schema was created with a null ValidationContext");
    }

    /**
     * Group: testGetValidationContextAfterReusingContextAcrossMultipleSchemas **
     */
    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 5, 10 })
    void getValidationContext_multipleSchemas_shareSameContext(int schemaCount) {
        // Arrange – create several independent schemas using the same ValidationContext instance.
        JsonSchema[] schemas = new JsonSchema[schemaCount];
        URI baseUri = URI.create("http://example.com/multi");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{}");
        for (int i = 0; i < schemaCount; i++) {
            schemas[i] = JsonSchema.from(validationContext, "path" + i, baseUri.resolve("schema" + i), node, null, false);
        }
        // Act & Assert – each schema must return the identical ValidationContext instance.
        for (int i = 0; i < schemaCount; i++) {
            assertSame(validationContext, schemas[i].getValidationContext(), "Schema #" + i + " should share the same ValidationContext instance");
        }
    }

    /**
     * Group: testGetValidationContextImmutability **
     */
    @Test
    void getValidationContext_immutability_noNewInstanceCreated() {
        // Act – retrieve the context twice.
        ValidationContext firstCall = jsonSchema.getValidationContext();
        ValidationContext secondCall = jsonSchema.getValidationContext();
        // Assert – both calls must return the exact same object reference.
        assertSame(firstCall, secondCall, "Subsequent calls to getValidationContext() should return the original instance, not a copy");
    }
}
