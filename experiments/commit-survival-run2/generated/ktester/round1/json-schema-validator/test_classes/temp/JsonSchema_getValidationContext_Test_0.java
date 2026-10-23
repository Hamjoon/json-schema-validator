package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonMetaSchema;
import com.networknt.schema.ValidationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
        // Configure the ValidationContext mock – only the meta‑schema is needed for the factory.
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        // Return null for config to avoid additional initialisation logic inside the library.
        when(validationContext.getConfig()).thenReturn(null);
        URI currentUri = URI.create("http://example.com/schema");
        // Create a JsonSchema instance via the public factory method.
        jsonSchema = JsonSchema.from(validationContext, "testSchemaPath", currentUri, schemaNode, // no parent schema
        null, // do not suppress sub‑schema retrieval
        false);
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
        JsonNode childNode = mock(JsonNode.class);
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
        URI currentUri = URI.create("http://example.com/nullContext");
        JsonSchema schemaWithNullContext = JsonSchema.from(null, "nullPath", currentUri, schemaNode, null, false);
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
        // Arrange – create several independent schemas using the same ValidationContext mock.
        JsonSchema[] schemas = new JsonSchema[schemaCount];
        URI baseUri = URI.create("http://example.com/multi");
        for (int i = 0; i < schemaCount; i++) {
            schemas[i] = JsonSchema.from(validationContext, "path" + i, baseUri.resolve("schema" + i), schemaNode, null, false);
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
        // Additionally, verify that the mock was not interacted with in a way that would suggest
        // the method creates a new instance (e.g., calling a factory on the mock).
        verifyNoMoreInteractions(validationContext);
    }
}
