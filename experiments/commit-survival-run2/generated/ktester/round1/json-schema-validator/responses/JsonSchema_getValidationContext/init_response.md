```java
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

@ExtendWith(MockitoExtension.class)
class JsonSchema_getValidationContext_Test {

    @Mock
    ValidationContext validationContext;

    @Mock
    JsonMetaSchema metaSchema;

    @Mock
    JsonNode schemaNode;

    JsonSchema jsonSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Configure the ValidationContext mock.
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        // Return null for config to avoid additional initialisation logic.
        when(validationContext.getConfig()).thenReturn(null);

        URI currentUri = URI.create("http://example.com/schema");
        // Use the public static factory method to create a JsonSchema instance.
        jsonSchema = JsonSchema.from(validationContext,
                                    "testSchemaPath",
                                    currentUri,
                                    schemaNode,
                                    null,          // no parent schema
                                    false);        // do not suppress sub‑schema retrieval
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void getValidationContext_returnsTheSameInstanceProvidedInConstructor() {
        ValidationContext result = jsonSchema.getValidationContext();
        assertSame(validationContext, result,
                "JsonSchema#getValidationContext should return the ValidationContext supplied at construction");
    }
}
```