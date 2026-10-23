```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_createChildSchema_Test {

    private static final String ROOT_SCHEMA_PATH = "root";

    @Mock
    private ValidationContext validationContext;      // mock injected by Mockito
    @Mock
    private JsonMetaSchema metaSchema;                // required by ValidationContext#getMetaSchema()
    @Mock
    private JsonSchema mockChildSchema;                // result of newSchema(...)
    @Mock
    private JsonNode childNode;                       // schemaNode supplied to createChildSchema

    private JsonSchema jsonSchema;                     // instance under test

    @BeforeAll
    static void setupBeforeAll() {
        // No class‑level resources needed.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Stub the ValidationContext behaviour used in the JsonSchema constructor.
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        // config can be null – the constructor checks for null before using it.
        when(validationContext.getConfig()).thenReturn(null);

        // Prepare a minimal schema node required by the private constructor.
        ObjectNode dummyRootNode = new ObjectNode(JsonNodeFactory.instance);

        // Use reflection to invoke the private JsonSchema constructor.
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class,
                String.class,
                URI.class,
                JsonNode.class,
                JsonSchema.class,
                boolean.class);
        ctor.setAccessible(true);
        jsonSchema = ctor.newInstance(
                validationContext,
                ROOT_SCHEMA_PATH,
                null,                 // currentUri
                dummyRootNode,        // schemaNode
                null,                 // parent
                false);               // suppressSubSchemaRetrieval

        // Stub the `newSchema` method that createChildSchema delegates to.
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(mockChildSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions for a clean next test.
        reset(validationContext, mockChildSchema, childNode);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No class‑level resources to clean.
    }

    @Test
    void createChildSchema_delegatesToValidationContext_andReturnsChild() {
        // GIVEN – the parameters for the call
        String childPath = "childPath";

        // WHEN
        JsonSchema result = jsonSchema.createChildSchema(childPath, childNode);

        // THEN
        assertSame(mockChildSchema, result, "createChildSchema should return the schema produced by ValidationContext");
        verify(validationContext, times(1)).newSchema(eq(childPath), eq(childNode), eq(jsonSchema));
        verifyNoMoreInteractions(validationContext);
    }
}
```